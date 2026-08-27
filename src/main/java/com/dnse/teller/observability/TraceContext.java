package com.dnse.teller.observability;

import java.util.ArrayList;
import java.util.List;

/**
 * Cây dấu vết hoàn chỉnh (Trace Context) chứa toàn bộ các spans của một request.
 */
public class TraceContext {
    private String traceId;
    private String sessionId;
    private String rootOperation;
    private long startTimeMs;
    private long endTimeMs;
    private long totalDurationMs;
    private String status;
    private List<TraceSpan> spans = new ArrayList<>();

    public TraceContext() {}

    public TraceContext(String traceId, String sessionId, String rootOperation, long startTimeMs) {
        this.traceId = traceId;
        this.sessionId = sessionId;
        this.rootOperation = rootOperation;
        this.startTimeMs = startTimeMs;
        this.status = "OK";
    }

    public void addSpan(TraceSpan span) {
        if (this.spans == null) this.spans = new ArrayList<>();
        this.spans.add(span);
    }

    public void complete() {
        this.endTimeMs = System.currentTimeMillis();
        this.totalDurationMs = this.endTimeMs - this.startTimeMs;
    }

    // Getters and Setters
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getRootOperation() { return rootOperation; }
    public void setRootOperation(String rootOperation) { this.rootOperation = rootOperation; }

    public long getStartTimeMs() { return startTimeMs; }
    public void setStartTimeMs(long startTimeMs) { this.startTimeMs = startTimeMs; }

    public long getEndTimeMs() { return endTimeMs; }
    public void setEndTimeMs(long endTimeMs) { this.endTimeMs = endTimeMs; }

    public long getTotalDurationMs() { return totalDurationMs; }
    public void setTotalDurationMs(long totalDurationMs) { this.totalDurationMs = totalDurationMs; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<TraceSpan> getSpans() { return spans; }
    public void setSpans(List<TraceSpan> spans) { this.spans = spans; }
}
