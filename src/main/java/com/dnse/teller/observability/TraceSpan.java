package com.dnse.teller.observability;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Đại diện cho một đơn vị thực thi (Span) trong mô hình phân tán OpenTelemetry.
 */
public class TraceSpan {
    private String spanId;
    private String parentSpanId;
    private String name;
    private long startTimeMs;
    private long endTimeMs;
    private long durationMs;
    private String status; // "OK", "ERROR"
    private Map<String, Object> attributes = new LinkedHashMap<>();

    public TraceSpan() {}

    public TraceSpan(String spanId, String parentSpanId, String name, long startTimeMs) {
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.name = name;
        this.startTimeMs = startTimeMs;
        this.status = "OK";
    }

    public void end() {
        this.endTimeMs = System.currentTimeMillis();
        this.durationMs = this.endTimeMs - this.startTimeMs;
    }

    public void addAttribute(String key, Object value) {
        if (this.attributes == null) this.attributes = new LinkedHashMap<>();
        this.attributes.put(key, value);
    }

    // Getters and Setters
    public String getSpanId() { return spanId; }
    public void setSpanId(String spanId) { this.spanId = spanId; }

    public String getParentSpanId() { return parentSpanId; }
    public void setParentSpanId(String parentSpanId) { this.parentSpanId = parentSpanId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getStartTimeMs() { return startTimeMs; }
    public void setStartTimeMs(long startTimeMs) { this.startTimeMs = startTimeMs; }

    public long getEndTimeMs() { return endTimeMs; }
    public void setEndTimeMs(long endTimeMs) { this.endTimeMs = endTimeMs; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
}
