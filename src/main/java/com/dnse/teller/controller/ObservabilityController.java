package com.dnse.teller.controller;

import com.dnse.teller.evaluator.EvaluationJudgeResult;
import com.dnse.teller.evaluator.LlmJudgeService;
import com.dnse.teller.model.Session;
import com.dnse.teller.observability.OpenTelemetryTracer;
import com.dnse.teller.observability.TraceContext;
import com.dnse.teller.persistence.WorkflowRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class ObservabilityController {

    private final OpenTelemetryTracer tracer;
    private final LlmJudgeService judgeService;
    private final WorkflowRepository repository;

    public ObservabilityController(
            OpenTelemetryTracer tracer,
            LlmJudgeService judgeService,
            WorkflowRepository repository
    ) {
        this.tracer = tracer;
        this.judgeService = judgeService;
        this.repository = repository;
    }

    @GetMapping("/observability/traces")
    public ResponseEntity<List<TraceContext>> getRecentTraces(@RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(tracer.getRecentTraces(limit));
    }

    @GetMapping("/observability/traces/{traceId}")
    public ResponseEntity<?> getTraceDetail(@PathVariable String traceId) {
        Optional<TraceContext> trace = tracer.getTraceById(traceId);
        return trace.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/evaluations/judge")
    public ResponseEntity<EvaluationJudgeResult> evaluateSession(@RequestBody Map<String, Object> payload) {
        String sessionId = payload.get("sessionId") != null ? String.valueOf(payload.get("sessionId")) : "";
        String traceId = payload.get("traceId") != null ? String.valueOf(payload.get("traceId")) : "";
        String userPrompt = payload.get("userPrompt") != null ? String.valueOf(payload.get("userPrompt")) : "";
        String assistantResponse = payload.get("assistantResponse") != null ? String.valueOf(payload.get("assistantResponse")) : "";
        long latencyMs = payload.get("latencyMs") != null ? ((Number) payload.get("latencyMs")).longValue() : 2000L;

        Map<String, Object> groundTruth = new LinkedHashMap<>();
        if (payload.get("groundTruthData") instanceof Map) {
            groundTruth = (Map<String, Object>) payload.get("groundTruthData");
        } else if (!sessionId.isEmpty()) {
            Optional<Session> sessionOpt = repository.findSessionById(sessionId);
            if (sessionOpt.isPresent()) {
                Session s = sessionOpt.get();
                if (s.getAgentRuntime() != null && s.getAgentRuntime().getToolCalls() != null) {
                    for (Map<String, Object> tc : s.getAgentRuntime().getToolCalls()) {
                        String capId = (String) tc.get("capabilityId");
                        Object res = tc.get("result");
                        if (capId != null && res != null) {
                            groundTruth.put(capId, res);
                        }
                    }
                }
                if (s.getCustomerContext() != null && !s.getCustomerContext().isEmpty()) groundTruth.put("customerContext", s.getCustomerContext());
                if (s.getPolicyFindings() != null) groundTruth.put("policyFindings", s.getPolicyFindings());
                if (s.getTransactionDraft() != null) groundTruth.put("transactionDraft", s.getTransactionDraft());
                if (s.getRisk() != null) groundTruth.put("risk", s.getRisk());
            }
        }

        EvaluationJudgeResult result = judgeService.evaluateResponse(
                sessionId, traceId, userPrompt, assistantResponse, groundTruth, latencyMs
        );

        return ResponseEntity.ok(result);
    }
}
