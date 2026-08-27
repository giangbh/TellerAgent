package com.dnse.teller.evaluator;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kết quả đánh giá chất lượng phản hồi từ LLM-as-a-Judge độc lập.
 */
public class EvaluationJudgeResult {
    private String evaluationId;
    private String sessionId;
    private String traceId;
    private double overallScore; // 0.0 - 10.0
    private double hallucinationScore; // 0.0 - 10.0 (10 = Không có ảo giác, 100% chính xác)
    private double politenessScore; // 0.0 - 10.0 (10 = Chuẩn mực xưng hô ngân hàng, lịch sự)
    private double complianceScore; // 0.0 - 10.0 (10 = Tuân thủ an toàn dữ liệu và hạn mức)
    private double latencyScore; // 0.0 - 10.0 (10 = Siêu nhanh < 1500ms)
    private long latencyMs;
    private String verdict; // "PASS", "WARNING", "FAIL"
    private String critique;
    private String evaluatedAt;
    private String judgeModel;
    private Map<String, Object> breakdown = new LinkedHashMap<>();

    public EvaluationJudgeResult() {}

    // Getters and Setters
    public String getEvaluationId() { return evaluationId; }
    public void setEvaluationId(String evaluationId) { this.evaluationId = evaluationId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }

    public double getHallucinationScore() { return hallucinationScore; }
    public void setHallucinationScore(double hallucinationScore) { this.hallucinationScore = hallucinationScore; }

    public double getPolitenessScore() { return politenessScore; }
    public void setPolitenessScore(double politenessScore) { this.politenessScore = politenessScore; }

    public double getComplianceScore() { return complianceScore; }
    public void setComplianceScore(double complianceScore) { this.complianceScore = complianceScore; }

    public double getLatencyScore() { return latencyScore; }
    public void setLatencyScore(double latencyScore) { this.latencyScore = latencyScore; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public String getCritique() { return critique; }
    public void setCritique(String critique) { this.critique = critique; }

    public String getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(String evaluatedAt) { this.evaluatedAt = evaluatedAt; }

    public String getJudgeModel() { return judgeModel; }
    public void setJudgeModel(String judgeModel) { this.judgeModel = judgeModel; }

    public Map<String, Object> getBreakdown() { return breakdown; }
    public void setBreakdown(Map<String, Object> breakdown) { this.breakdown = breakdown; }
}
