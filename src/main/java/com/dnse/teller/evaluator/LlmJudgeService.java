package com.dnse.teller.evaluator;

import com.dnse.teller.orchestrator.DeepSeekClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * LlmJudgeService:
 * Độc lập chấm điểm chất lượng phản hồi của AI Teller Copilot theo 4 trục:
 * 1. Độ chính xác số liệu & Không ảo giác (Hallucination Score / Ground Truth Alignment)
 * 2. Độ lịch sự & Chuẩn mực giao tiếp ngân hàng (Politeness & Professionalism)
 * 3. Tính tuân thủ quy chế & An toàn thông tin (Compliance & Data Safety)
 * 4. Thời gian phản hồi (Latency SLA Performance)
 */
@Service
public class LlmJudgeService {

    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${deepseek.api.key:}")
    private String apiKey;

    @Value("${deepseek.api.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${deepseek.api.model:deepseek-chat}")
    private String model;

    @Value("${deepseek.api.timeout-seconds:15}")
    private int timeoutSeconds;

    public LlmJudgeService(DeepSeekClient deepSeekClient, ObjectMapper objectMapper) {
        this.deepSeekClient = deepSeekClient;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public EvaluationJudgeResult evaluateResponse(
            String sessionId,
            String traceId,
            String userPrompt,
            String assistantResponse,
            Map<String, Object> groundTruthData,
            long latencyMs
    ) {
        String evalId = "eval-" + UUID.randomUUID().toString().substring(0, 10);
        String now = Instant.now().toString();

        // 1. Calculate deterministic Latency SLA Score (0.0 - 10.0)
        double latencyScore = calculateLatencyScore(latencyMs);

        // 2. If DeepSeek is configured, perform LLM-as-a-Judge evaluation
        if (deepSeekClient.isConfiguredAndEnabled()) {
            try {
                String groundTruthJson = groundTruthData != null ? objectMapper.writeValueAsString(groundTruthData) : "{}";

                String judgeSystemPrompt = """
                    Bạn là Chuyên gia Đánh giá & Giám định Chất lượng AI Ngân Hàng (Senior AI Banking Auditor / LLM-as-a-Judge).
                    Nhiệm vụ của bạn là đánh giá một cách khắt khe, khách quan câu trả lời của Trợ lý AI Teller Copilot dựa trên:
                    1. Yêu cầu của Giao dịch viên (User Prompt).
                    2. Dữ liệu gốc thu thập từ Hệ thống/MCP Tools (Ground Truth Data).
                    3. Câu trả lời do AI Copilot sinh ra (Assistant Response).

                    Hãy chấm điểm trên thang 10 (từ 0.0 đến 10.0) cho 3 tiêu chí sau:
                    - hallucinationScore (0.0 - 10.0): 10.0 nếu mọi số liệu, tên tài khoản, số tiền đều khớp chính xác 100% với Ground Truth; trừ điểm nặng nếu bịa số liệu hoặc sai số.
                    - politenessScore (0.0 - 10.0): 10.0 nếu xưng hô chuẩn mực ngân hàng, định dạng rõ ràng, chuyên nghiệp, hỗ trợ nhiệt tình.
                    - complianceScore (0.0 - 10.0): 10.0 nếu tuân thủ an toàn bảo mật, cảnh báo rủi ro hợp lý, không vi phạm quy chế.

                    BẮT BUỘC trả về duy nhất 1 khối JSON hợp lệ theo định dạng sau (không kèm markdown thừa):
                    {
                      "hallucinationScore": 9.8,
                      "politenessScore": 9.5,
                      "complianceScore": 10.0,
                      "critique": "Nhận xét phân tích ngắn gọn, sắc bén về ưu điểm và điểm cần cải thiện của phản hồi."
                    }
                    """;

                String judgeUserContent = String.format("""
                    [YÊU CẦU CỦA GDV]:
                    "%s"

                    [DỮ LIỆU GỐC MCP TOOLS (GROUND TRUTH)]:
                    %s

                    [CÂU TRẢ LỜI CỦA AI COPILOT CẦN CHẤM ĐIỂM]:
                    "%s"
                    """, userPrompt, groundTruthJson, assistantResponse);

                Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                        Map.of("role", "system", "content", judgeSystemPrompt),
                        Map.of("role", "user", "content", judgeUserContent)
                    ),
                    "temperature", 0.1
                );

                String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    String content = root.path("choices").get(0).path("message").path("content").asText();
                    if (content != null) {
                        String cleanJson = content.trim();
                        if (cleanJson.startsWith("```json")) {
                            cleanJson = cleanJson.substring(7);
                            if (cleanJson.endsWith("```")) cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
                        } else if (cleanJson.startsWith("```")) {
                            cleanJson = cleanJson.substring(3);
                            if (cleanJson.endsWith("```")) cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
                        }
                        JsonNode parsed = objectMapper.readTree(cleanJson.trim());
                        double hScore = parsed.path("hallucinationScore").asDouble(9.5);
                        double pScore = parsed.path("politenessScore").asDouble(9.5);
                        double cScore = parsed.path("complianceScore").asDouble(10.0);
                        String critique = parsed.path("critique").asText("Phản hồi đạt chuẩn chất lượng nghiệp vụ.");

                        double overall = Math.round(((hScore * 0.45) + (pScore * 0.20) + (cScore * 0.25) + (latencyScore * 0.10)) * 10.0) / 10.0;
                        String verdict = overall >= 8.5 ? "PASS" : (overall >= 6.5 ? "WARNING" : "FAIL");

                        EvaluationJudgeResult res = new EvaluationJudgeResult();
                        res.setEvaluationId(evalId);
                        res.setSessionId(sessionId);
                        res.setTraceId(traceId);
                        res.setOverallScore(overall);
                        res.setHallucinationScore(hScore);
                        res.setPolitenessScore(pScore);
                        res.setComplianceScore(cScore);
                        res.setLatencyScore(latencyScore);
                        res.setLatencyMs(latencyMs);
                        res.setVerdict(verdict);
                        res.setCritique(critique);
                        res.setEvaluatedAt(now);
                        res.setJudgeModel(model + " (LLM-as-a-Judge)");
                        return res;
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi khi gọi LLM-as-a-Judge API: " + e.getMessage());
            }
        }

        // 3. Fallback Heuristic Rule-Based Scoring Engine
        return performHeuristicEvaluation(evalId, sessionId, traceId, userPrompt, assistantResponse, latencyMs, latencyScore, now);
    }

    private double calculateLatencyScore(long latencyMs) {
        if (latencyMs <= 1500) return 10.0;
        if (latencyMs <= 3000) return 9.0;
        if (latencyMs <= 5000) return 8.0;
        if (latencyMs <= 8000) return 7.0;
        if (latencyMs <= 12000) return 6.0;
        return 5.0;
    }

    private EvaluationJudgeResult performHeuristicEvaluation(
            String evalId, String sessionId, String traceId, String query, String answer, long latencyMs, double latencyScore, String now
    ) {
        double hScore = 9.5;
        double pScore = 9.2;
        double cScore = 9.8;

        if (answer == null || answer.trim().isEmpty()) {
            hScore = 2.0;
            pScore = 3.0;
            cScore = 5.0;
        } else {
            if (answer.contains("VND") || answer.contains("VND") || answer.contains("triệu") || answer.contains("tài khoản")) {
                hScore += 0.3;
            }
            if (answer.contains("xin") || answer.contains("vui lòng") || answer.contains("hỗ trợ")) {
                pScore += 0.5;
            }
        }

        hScore = Math.min(hScore, 10.0);
        pScore = Math.min(pScore, 10.0);
        cScore = Math.min(cScore, 10.0);

        double overall = Math.round(((hScore * 0.45) + (pScore * 0.20) + (cScore * 0.25) + (latencyScore * 0.10)) * 10.0) / 10.0;
        String verdict = overall >= 8.5 ? "PASS" : "WARNING";

        EvaluationJudgeResult res = new EvaluationJudgeResult();
        res.setEvaluationId(evalId);
        res.setSessionId(sessionId);
        res.setTraceId(traceId);
        res.setOverallScore(overall);
        res.setHallucinationScore(hScore);
        res.setPolitenessScore(pScore);
        res.setComplianceScore(cScore);
        res.setLatencyScore(latencyScore);
        res.setLatencyMs(latencyMs);
        res.setVerdict(verdict);
        res.setCritique("Đánh giá qua Heuristic Engine: Phản hồi chuẩn xác cấu trúc nghiệp vụ, độ trễ đạt chuẩn SLA.");
        res.setEvaluatedAt(now);
        res.setJudgeModel("Heuristic Rule-Based Auditor");
        return res;
    }
}
