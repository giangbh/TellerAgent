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
                và lựa chọn đúng các công cụ MCP phù hợp để thực thi qua Function Calling (Tools Call).

                Quy tắc chọn công cụ MCP:
                1. Nộp tiền mặt vào tài khoản -> chọn tool `account_resolve_by_number` (tham số `accountNumber`), `cash_limit_check` (tham số `amount`).
                2. Rút tiền mặt từ tài khoản -> chọn tool `account_resolve_by_number` (tham số `accountNumber`), `cash_limit_check` (tham số `amount`).
                3. Tra cứu/in sao kê, xem lịch sử giao dịch, biến động số dư, dòng tiền thu chi -> BẮT BUỘC chọn tool `statement_transaction_history` (tham số `accountNumber` hoặc `customerRef`).
                4. Phân tích chân dung khách hàng 360, hành vi chi tiêu, khẩu vị đầu tư -> BẮT BUỘC chọn tool `customer_persona_analytics` (tham số `customerRef`).
                5. Kiểm tra điểm tín dụng CIC, nợ xấu, hạn mức vay phê duyệt trước -> BẮT BUỘC chọn tool `customer_credit_score_check` (tham số `customerRef`).
                6. Tư vấn gợi ý ưu đãi, đề xuất sản phẩm phù hợp tiếp theo (Next-Best-Offer/NBO) -> BẮT BUỘC chọn tool `recommendation_nbo_products` (tham số `customerRef`).
                7. Tính tiền lãi tiết kiệm, tối ưu phương án gửi tiền, so sánh kỳ hạn -> BẮT BUỘC chọn tool `savings_product_advisor` (tham số `amount`, `termMonths`).
                8. Dịch vụ thẻ (khóa thẻ, đổi PIN, tra cứu hạn mức thẻ) -> BẮT BUỘC chọn tool `card_service_manage` (tham số `customerRef`, `cardAction`).
                9. Tra cứu tỷ giá ngoại tệ & quy đổi tiền tệ -> BẮT BUỘC chọn tool `fx_rate_lookup` (tham số `currency`, `amount`).
                10. Tra cứu địa chỉ, hotline mạng lưới chi nhánh -> BẮT BUỘC chọn tool `branch_directory_lookup` (tham số `city`).
                11. Đối chiếu thông tin chủ tài khoản, kiểm tra số dư khả dụng -> chọn tool `account_resolve_by_number` (tham số `accountNumber`).
                12. Chuyển tiền / Chuyển khoản trong nước & 24/7 -> gọi chuỗi kiểm tra hạn mức `transfer_limit_check`, tra cứu ngân hàng `bank_directory_lookup`, tính phí `pricing_transfer_fee`.
                13. Chuẩn hóa số tiền thành số nguyên VND (VD: 50tr -> 50000000, 1.5 tỷ -> 1500000000, 500k -> 500000).
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

                String lowerPrompt = prompt != null ? prompt.toLowerCase() : "";
                String intentType = "DYNAMIC_AUTONOMOUS_TASK";
                String workflow = "dynamic_autonomous";

                if (lowerPrompt.contains("nộp tiền") || lowerPrompt.matches(".*nộp\\s+\\d.*")) {
                    intentType = "CASH_DEPOSIT";
                    workflow = "cash_deposit";
                } else if (lowerPrompt.contains("rút tiền") || lowerPrompt.matches(".*rút\\s+\\d.*")) {
                    intentType = "CASH_WITHDRAWAL";
                    workflow = "cash_withdrawal";
                } else if (lowerPrompt.contains("chuyển khoản") || lowerPrompt.contains("chuyển tiền") || lowerPrompt.matches(".*chuyển\\s+\\d.*")) {
                    intentType = "DOMESTIC_TRANSFER";
                    workflow = "domestic_transfer";
                }

                Plan plan;
                if ("cash_deposit".equals(workflow) || "cash_withdrawal".equals(workflow) || "domestic_transfer".equals(workflow)) {
                    plan = new Plan(
                        "Mở màn hình giao dịch và tự điền bản nháp Live Draft",
                        List.of(
                            new PlanStep("S1", "DELEGATE", "customer_context_agent", List.of()),
                            new PlanStep("S2", "DELEGATE", "policy_agent", List.of()),
                            new PlanStep("S3", "DELEGATE", "transaction_draft_agent", List.of("S1", "S2")),
                            new PlanStep("S4", "DELEGATE", "risk_assistant", List.of("S3"))
                        ),
                        List.of("Có đủ trường bắt buộc", "Qua validation", "Có risk decision", "Không posting")
                    );
                } else {
                    plan = new Plan(
                        "DeepSeek AI tự động lập kế hoạch và điều phối " + steps.size() + " công cụ MCP",
                        steps,
                        List.of("Thực thi toàn bộ các bước được DeepSeek đề xuất", "Tổng hợp phản hồi an toàn cho GDV")
                    );
                }

                Intent dynamicIntent = new Intent(intentType, workflow, 0.99, extractedEntities);
                dynamicIntent.setQuery(prompt);

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

    public Optional<String> synthesizeAnswer(String userPrompt, Map<String, Object> toolOutputs) {
        if (!isConfiguredAndEnabled() || toolOutputs == null || toolOutputs.isEmpty()) {
            return Optional.empty();
        }

        try {
            String rawJson = objectMapper.writeValueAsString(toolOutputs);

            String systemPrompt = """
                Bạn là AI Teller Copilot chuyên nghiệp và thông minh cho Giao dịch viên ngân hàng.
                Dưới đây là câu hỏi của Giao dịch viên và dữ liệu (JSON) thu thập được từ các MCP Tools ngân hàng.
                Nhiệm vụ của bạn là:
                1. Trả lời đúng, đầy đủ và trực diện tất cả các ý trong câu hỏi của GDV.
                2. Ưu tiên sử dụng các số liệu chuẩn xác đã được hệ thống tính toán sẵn trong phần 'analytics' / 'summary' (như tổng dòng tiền vào Inflow, tổng ra Outflow, giao dịch lớn nhất, số dư ròng Net Cashflow, điểm tín dụng, lãi suất) để đảm bảo độ chính xác số học tuyệt đối 100%.
                3. Kết hợp đối chiếu danh sách chi tiết các giao dịch để phân tích xu hướng chi tiêu, cơ cấu dòng tiền và đưa ra nhận định chuyên môn sắc bén cho GDV.
                4. Trình bày rõ ràng, mạch lạc, dễ đọc bằng các gạch đầu dòng bullet points.
                5. Định dạng tiền tệ VND chuyên nghiệp (ví dụ: 45.000.000 VND hoặc 45 tr VND).
                """;

            String userContent = "Yêu cầu của GDV: \"" + userPrompt + "\"\n\nDữ liệu MCP Tools trả về:\n" + rawJson;

            List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
            );

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.2);

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
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    String content = choices.get(0).path("message").path("content").asText();
                    if (content != null && !content.trim().isEmpty()) {
                        return Optional.of(content.trim());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi DeepSeek tổng hợp phản hồi nghiệp vụ: " + e.getMessage());
        }
        return Optional.empty();
    }
}
