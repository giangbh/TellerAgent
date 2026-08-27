package com.dnse.teller.gateway;

import com.dnse.teller.persistence.WorkflowRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class InternalApiGateway {
    private final WorkflowRepository repository;
    private final ObjectMapper objectMapper;
    private final AtomicLong postingCounter = new AtomicLong(0);

    public InternalApiGateway(WorkflowRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> dispatchCorePosting(String caller, String workflow, String capabilityId, Map<String, Object> args, String idempotencyKey) throws Exception {
        String traceId = "TRC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 1. Authorization Check (Only business_orchestrator via Gateway is authorized for Core posting)
        if (!"business_orchestrator".equals(caller)) {
            throw new SecurityException("API Gateway Rejected: Caller '" + caller + "' không có quyền truy cập Core Banking.");
        }

        // 2. Idempotency Check in Ledger
        if (idempotencyKey != null) {
            Optional<Map<String, Object>> existing = repository.findAuditByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                String payloadJson = (String) existing.get().get("payload_json");
                return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
            }
        }

        // 3. Execute Core Banking Mock Posting
        long seq = postingCounter.incrementAndGet();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "POSTED");

        if ("core.cash.execute".equals(capabilityId)) {
            String txType = args != null && args.get("transactionType") != null ? String.valueOf(args.get("transactionType")) : "CASH_DEPOSIT";
            String prefix = "CASH_WITHDRAWAL".equals(txType) ? "CW" : "CD";
            result.put("coreReference", String.format("%s%08d", prefix, seq));
            result.put("transactionType", txType);
        } else {
            result.put("coreReference", String.format("FT%08d", seq));
        }

        result.put("postedAt", Instant.now().toString());
        result.put("traceId", traceId);
        result.put("amount", args != null && args.get("amount") != null ? ((Number) args.get("amount")).longValue() : 0L);
        result.put("mock", true);

        // 4. Save Immutable Audit Record & Idempotency in SQLite
        String resultJson = objectMapper.writeValueAsString(result);
        repository.recordAuditLog(traceId, caller, capabilityId, idempotencyKey, "POSTED", resultJson);

        return result;
    }
}
