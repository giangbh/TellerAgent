package com.dnse.teller.orchestrator;

import com.dnse.teller.mcp.McpTool;
import com.dnse.teller.mcp.McpToolRegistry;
import com.dnse.teller.model.Intent;
import com.dnse.teller.model.Plan;
import com.dnse.teller.model.Plan.PlanStep;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class DeepSeekClient {

    @Value("${deepseek.api.enabled:false}")
    private boolean enabled;

    @Value("${deepseek.api.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${deepseek.api.key:}")
    private String apiKey;

    @Value("${deepseek.api.model:deepseek-chat}")
    private String model;

    @Value("${deepseek.api.temperature:0.1}")
    private double temperature;

    @Value("${deepseek.api.timeout-seconds:15}")
    private int timeoutSeconds;

    private final McpToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public DeepSeekClient(McpToolRegistry toolRegistry, ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public boolean isConfiguredAndEnabled() {
        return enabled && apiKey != null && !apiKey.trim().isEmpty() && !apiKey.contains("your-deepseek-api-key");
    }

    public static class DeepSeekPlanResult {
        private final Intent intent;
        private final Plan plan;
        private final String assistantResponse;

        public DeepSeekPlanResult(Intent intent, Plan plan, String assistantResponse) {
            this.intent = intent;
            this.plan = plan;
            this.assistantResponse = assistantResponse;
        }

        public Intent getIntent() { return intent; }
        public Plan getPlan() { return plan; }
        public String getAssistantResponse() { return assistantResponse; }
    }

    public Optional<DeepSeekPlanResult> reasonAndPlan(String prompt, Map<String, Object> currentEntities) {
        if (!isConfiguredAndEnabled()) {
            return Optional.empty();
        }

        try {
            List<Map<String, Object>> toolsManifest = buildToolsManifest();

            String systemPrompt = """
                Bạn là AI Copilot cho Giao dịch viên Ngân hàng (Teller Agent) tại quầy.
                Nhiệm vụ của bạn là phân tích câu lệnh ngôn ngữ tự nhiên tiếng Việt từ Giao dịch viên,
                nhận diện ý định, trích xuất chính xác các thực thể (số tiền, số tài khoản, mã ngân hàng, loại ngoại tệ, chi nhánh, mã CIF),
                và lựa chọn các công cụ MCP phù hợp để thực thi qua Function Calling (Tools Call).

                Nguyên tắc:
                1. Nếu yêu cầu liên quan đến tra cứu thông tin (tỷ giá, chi nhánh, số dư, chính sách, danh mục tài khoản), hãy gọi công cụ đọc tương ứng.
                2. Nếu là chuyển khoản (Domestic Transfer), gọi các công cụ liên quan hoặc chuẩn bị dữ liệu giao dịch.
                3. Đảm bảo số tiền được chuẩn hóa thành số nguyên VND (ví dụ 50tr -> 50000000).
                """;

            List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", prompt)
            );

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("tools", toolsManifest);
            requestBody.put("tool_choice", "auto");
            requestBody.put("temperature", temperature);

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.trim())
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("DeepSeek API returned error code " + response.statusCode() + ": " + response.body());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return Optional.empty();
            }

            JsonNode messageNode = choices.get(0).path("message");
            String content = messageNode.path("content").asText(null);
            JsonNode toolCallsNode = messageNode.path("tool_calls");

            if (toolCallsNode.isArray() && !toolCallsNode.isEmpty()) {
                List<PlanStep> steps = new ArrayList<>();
                Map<String, Object> extractedEntities = new LinkedHashMap<>(currentEntities != null ? currentEntities : Map.of());
                int stepIdx = 1;

                for (JsonNode tc : toolCallsNode) {
                    JsonNode func = tc.path("function");
                    String funcName = func.path("name").asText();
                    String argsJson = func.path("arguments").asText("{}");

                    Map<String, Object> args = objectMapper.readValue(argsJson, new TypeReference<Map<String, Object>>() {});
                    extractedEntities.putAll(args);

                    // Find corresponding MCP capability ID
                    String capabilityId = resolveCapabilityId(funcName);
                    String stepId = "S" + stepIdx;
                    List<String> dependsOn = stepIdx > 1 ? List.of("S" + (stepIdx - 1)) : List.of();

                    steps.add(new PlanStep(stepId, "DELEGATE", "dynamic_tool_agent:" + capabilityId, dependsOn));
                    stepIdx++;
                }

                Intent dynamicIntent = new Intent("DYNAMIC_AUTONOMOUS_TASK", "dynamic_autonomous", 0.99, extractedEntities);
                dynamicIntent.setQuery(prompt);

                Plan plan = new Plan(
                    "DeepSeek AI tự động lập kế hoạch và điều phối " + steps.size() + " công cụ MCP",
                    steps,
                    List.of("Thực thi toàn bộ các bước được DeepSeek đề xuất", "Tổng hợp phản hồi an toàn cho GDV")
                );

                return Optional.of(new DeepSeekPlanResult(dynamicIntent, plan, content));
            }

            // If DeepSeek returned pure text analysis
            if (content != null && !content.trim().isEmpty()) {
                Intent textIntent = new Intent("POLICY_ASSISTANCE", "policy_assistance", 0.95, Map.of());
                textIntent.setQuery(prompt);
                Plan plan = new Plan("DeepSeek AI phân tích chính sách nghiệp vụ", List.of(
                    new PlanStep("S1", "DELEGATE", "policy_agent", List.of())
                ), List.of("Tổng hợp nội dung tư vấn"));
                return Optional.of(new DeepSeekPlanResult(textIntent, plan, content));
            }

        } catch (Exception e) {
            System.err.println("Cảnh báo: Lỗi khi gọi DeepSeek API (Sẽ tự động fallback sang Rule-based): " + e.getMessage());
        }

        return Optional.empty();
    }

    public List<Map<String, Object>> buildToolsManifest() {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (McpTool tool : toolRegistry.getAllTools()) {
            Map<String, Object> funcDef = new LinkedHashMap<>();
            // OpenAI tool name must match regex ^[a-zA-Z0-9_-]+$
            String cleanName = tool.getId().replace('.', '_');
            funcDef.put("name", cleanName);
            funcDef.put("description", tool.getDescription() + " (Capability: " + tool.getId() + ")");
            funcDef.put("parameters", tool.getInputSchema() != null ? tool.getInputSchema() : Map.of("type", "object", "properties", Map.of()));

            tools.add(Map.of(
                "type", "function",
                "function", funcDef
            ));
        }
        return tools;
    }

    private String resolveCapabilityId(String funcName) {
        if (funcName.contains("_") && !funcName.contains(".")) {
            // e.g. customer_profile_read -> customer.profile.read
            // or fx_rate_lookup -> fx.rate.lookup
            Optional<McpTool> opt = toolRegistry.resolve(funcName.replace('_', '.'));
            if (opt.isPresent()) return opt.get().getId();

            Optional<McpTool> byName = toolRegistry.resolve(funcName);
            if (byName.isPresent()) return byName.get().getId();
        }

        Optional<McpTool> direct = toolRegistry.resolve(funcName);
        return direct.map(McpTool::getId).orElse(funcName);
    }
}
