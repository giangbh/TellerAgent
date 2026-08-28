package com.dnse.teller.gateway;

import com.dnse.teller.persistence.WorkflowRepository;
import com.dnse.teller.security.AuthorizationException;
import com.dnse.teller.security.PostingAuthorization;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cửa duy nhất đi vào core banking.
 *
 * P0-2: bản cũ kiểm tra caller bằng so sánh chuỗi, trong khi chuỗi đó do chính
 * tool hardcode khi gọi — nhánh từ chối là code chết. Nay gateway nhận
 * {@link PostingAuthorization}, thứ chỉ PostingAuthorizer tạo được sau khi đã
 * kiểm chứng lại control gate từ dữ liệu đã persist.
 */
@Service
public class InternalApiGateway {
    private final WorkflowRepository repository;
    private final ObjectMapper objectMapper;
    private final AtomicLong postingCounter = new AtomicLong(0);

    public InternalApiGateway(WorkflowRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> dispatchCorePosting(PostingAuthorization auth, String capabilityId, Map<String, Object> args) throws Exception {
        if (auth == null) {
            throw new AuthorizationException(
                    "Từ chối hạch toán: không có PostingAuthorization.", "POSTING_AUTHORIZATION_MISSING", 403);
        }
        if (!auth.getCapabilityId().equals(capabilityId)) {
            throw new AuthorizationException(
                    "Giấy phép cấp cho " + auth.getCapabilityId() + " nhưng đang gọi " + capabilityId + ".",
                    "CAPABILITY_MISMATCH", 403);
        }

        // Số tiền thực post phải khớp số tiền đã được phê duyệt, không lệch một đồng.
        long argAmount = args != null && args.get("amount") instanceof Number n ? n.longValue() : -1L;
        if (argAmount != auth.getAmount()) {
            throw new AuthorizationException(
                    "Số tiền hạch toán (" + argAmount + ") khác số tiền đã phê duyệt (" + auth.getAmount() + ").",
                    "AMOUNT_TAMPERED", 409);
        }

        String idempotencyKey = auth.getIdempotencyKey();
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new AuthorizationException("Thiếu idempotency key.", "IDEMPOTENCY_REQUIRED", 400);
        }

        // Idempotency: trả lại đúng kết quả cũ nếu key đã tồn tại.
        Optional<Map<String, Object>> existing = repository.findAuditByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            String payloadJson = (String) existing.get().get("payload_json");
            return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
        }

        String traceId = "TRC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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
        result.put("amount", auth.getAmount());
        result.put("authorizedBy", auth.getTellerActorId());
        result.put("countersignedBy", auth.getSupervisorActorId());
        result.put("sessionRevision", auth.getSessionRevision());
        result.put("mock", true);

        String resultJson = objectMapper.writeValueAsString(result);
        repository.recordAuditLog(traceId, auth.getTellerActorId(), capabilityId, idempotencyKey, "POSTED", resultJson);

        return result;
    }
}
