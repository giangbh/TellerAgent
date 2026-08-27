package com.dnse.teller.observability;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * OpenTelemetryTracer:
 * Quản lý vòng đời Distributed Tracing, bắt đầu/kết thúc các Spans và lưu trữ lịch sử APM Traces.
 */
@Service
public class OpenTelemetryTracer {

    private final ThreadLocal<TraceContext> currentTrace = new ThreadLocal<>();
    private final Deque<TraceContext> recentTraces = new ConcurrentLinkedDeque<>();
    private final Map<String, TraceContext> traceIndex = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_TRACES = 200;

    public TraceContext startTrace(String sessionId, String rootOperation) {
        String traceId = "trace-" + UUID.randomUUID().toString().substring(0, 12);
        TraceContext context = new TraceContext(traceId, sessionId, rootOperation, System.currentTimeMillis());
        currentTrace.set(context);
        return context;
    }

    public TraceSpan startSpan(String spanName, String parentSpanId) {
        TraceContext ctx = currentTrace.get();
        String spanId = "span-" + UUID.randomUUID().toString().substring(0, 8);
        TraceSpan span = new TraceSpan(spanId, parentSpanId, spanName, System.currentTimeMillis());
        if (ctx != null) {
            ctx.addSpan(span);
        }
        return span;
    }

    public void endSpan(TraceSpan span) {
        if (span != null) {
            span.end();
        }
    }

    public TraceContext endTrace() {
        TraceContext ctx = currentTrace.get();
        if (ctx != null) {
            ctx.complete();
            traceIndex.put(ctx.getTraceId(), ctx);
            recentTraces.addFirst(ctx);
            while (recentTraces.size() > MAX_HISTORY_TRACES) {
                TraceContext removed = recentTraces.removeLast();
                if (removed != null) {
                    traceIndex.remove(removed.getTraceId());
                }
            }
            currentTrace.remove();
        }
        return ctx;
    }

    public TraceContext getCurrentTrace() {
        return currentTrace.get();
    }

    public Optional<TraceContext> getTraceById(String traceId) {
        return Optional.ofNullable(traceIndex.get(traceId));
    }

    public List<TraceContext> getRecentTraces(int limit) {
        int max = Math.min(limit > 0 ? limit : 20, recentTraces.size());
        List<TraceContext> list = new ArrayList<>();
        int count = 0;
        for (TraceContext ctx : recentTraces) {
            list.add(ctx);
            count++;
            if (count >= max) break;
        }
        return list;
    }
}
