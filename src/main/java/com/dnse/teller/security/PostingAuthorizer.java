package com.dnse.teller.security;

import com.dnse.teller.model.ApprovalRecord;
import com.dnse.teller.model.Approvals;
import com.dnse.teller.model.Session;
import com.dnse.teller.model.TransactionDraft;
import com.dnse.teller.persistence.WorkflowRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Nơi duy nhất cấp quyền hạch toán.
 *
 * Nguyên tắc: KHÔNG tin state trong bộ nhớ do orchestrator đưa sang. Nạp lại
 * session đã persist và tự kiểm chứng độc lập. Đây là "last-mile check" —
 * nếu một lỗi logic ở tầng trên làm hỏng control gate, tầng này vẫn chặn.
 */
@Service
public class PostingAuthorizer {

    private final WorkflowRepository repository;

    public PostingAuthorizer(WorkflowRepository repository) {
        this.repository = repository;
    }

    public PostingAuthorization authorize(String sessionId) {
        Optional<Session> persisted = repository.findSessionById(sessionId);
        if (persisted.isEmpty()) {
            throw new AuthorizationException(
                    "Không tìm thấy session đã persist để cấp quyền hạch toán: " + sessionId,
                    "SESSION_NOT_PERSISTED", 409);
        }
        Session session = persisted.get();

        if (session.getExecution() != null && "POSTED".equals(session.getExecution().getStatus())) {
            throw new AuthorizationException("Giao dịch đã được hạch toán.", "ALREADY_POSTED", 409);
        }

        String workflow = session.getWorkflow();
        if (workflow == null || !(workflow.startsWith("cash_") || "domestic_transfer".equals(workflow))) {
            throw new AuthorizationException(
                    "Workflow '" + workflow + "' không phải luồng giao dịch tài chính.",
                    "WORKFLOW_NOT_FINANCIAL", 403);
        }

        TransactionDraft draft = session.getTransactionDraft();
        if (draft == null || draft.getValidation() == null || !Boolean.TRUE.equals(draft.getValidation().getValid())) {
            throw new AuthorizationException("Bản nháp chưa hợp lệ.", "DRAFT_NOT_VALID", 400);
        }
        if (draft.getAmount() == null || draft.getAmount() <= 0) {
            throw new AuthorizationException("Số tiền không hợp lệ.", "AMOUNT_INVALID", 400);
        }

        if (session.getRisk() == null || !"PASS".equals(session.getRisk().getDecision())) {
            throw new AuthorizationException("Kết quả rà soát rủi ro chưa PASS.", "RISK_NOT_PASSED", 403);
        }

        if (session.getControl() == null || !session.getControl().isPostingAllowed()) {
            throw new AuthorizationException("Control gate chưa đủ điều kiện.", "CONTROL_GATES_INCOMPLETE", 400);
        }

        Approvals approvals = session.getApprovals();
        if (approvals == null) {
            throw new AuthorizationException("Thiếu bản ghi phê duyệt.", "APPROVALS_MISSING", 400);
        }

        ApprovalRecord customer = approvals.getCustomerRecord();
        if (customer == null || isBlank(customer.getEvidenceType()) || isBlank(customer.getRecordedByActorId())) {
            throw new AuthorizationException(
                    "Thiếu xác nhận của khách hàng kèm bằng chứng.", "CUSTOMER_CONSENT_MISSING", 400);
        }

        ApprovalRecord teller = approvals.getTellerRecord();
        if (teller == null || isBlank(teller.getActorId())) {
            throw new AuthorizationException("Thiếu xác nhận của GDV.", "TELLER_APPROVAL_MISSING", 400);
        }

        boolean supervisorRequired = supervisorRequired(draft);
        ApprovalRecord supervisor = approvals.getSupervisorRecord();
        if (supervisorRequired) {
            if (supervisor == null || isBlank(supervisor.getActorId())) {
                throw new AuthorizationException(
                        "Giao dịch vượt hạn mức, cần kiểm soát viên xác nhận.", "SUPERVISOR_APPROVAL_MISSING", 400);
            }
            // Phân tách trách nhiệm: mắt thứ hai phải là người khác.
            if (teller.getActorId().equals(supervisor.getActorId())) {
                throw new AuthorizationException(
                        "Vi phạm nguyên tắc 4 mắt: GDV và kiểm soát viên là cùng một người ("
                                + teller.getActorId() + ").",
                        "SEGREGATION_OF_DUTIES_VIOLATION", 403);
            }
        }

        // Khách hàng phải được ghi nhận bởi chính GDV đang chịu trách nhiệm giao dịch.
        if (!teller.getActorId().equals(customer.getRecordedByActorId())) {
            throw new AuthorizationException(
                    "GDV ghi nhận đồng ý của khách hàng khác với GDV xác nhận giao dịch.",
                    "CONSENT_RECORDER_MISMATCH", 403);
        }

        String capabilityId = workflow.startsWith("cash_") ? "core.cash.execute" : "core.transfer.execute";

        return new PostingAuthorization(
                session.getSessionId(),
                workflow,
                capabilityId,
                deriveIdempotencyKey(session),
                draft.getAmount(),
                teller.getActorId(),
                supervisor != null ? supervisor.getActorId() : null,
                session.getRevision(),
                Instant.now().toString()
        );
    }

    /**
     * P1-5: idempotency key phải là hàm xác định của nội dung nghiệp vụ.
     * UUID ngẫu nhiên sinh lúc execute sẽ tạo key mới sau mỗi lần crash-retry,
     * dẫn tới hạch toán trùng.
     */
    public static String deriveIdempotencyKey(Session session) {
        TransactionDraft d = session.getTransactionDraft();
        String material = String.join("|",
                nz(session.getSessionId()),
                nz(session.getWorkflow()),
                nz(d.getSourceAccountRef()),
                nz(d.getAccountNumber()),
                nz(d.getBeneficiaryAccount()),
                nz(d.getBankCode()),
                nz(d.getTransactionType()),
                d.getAmount() != null ? String.valueOf(d.getAmount()) : "0"
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            return "IDK-" + HexFormat.of().formatHex(hash).substring(0, 32);
        } catch (Exception ex) {
            throw new AuthorizationException("Không tạo được idempotency key.", "IDEMPOTENCY_KEY_ERROR", 500);
        }
    }

    private static boolean supervisorRequired(TransactionDraft draft) {
        if (draft.getLimit() == null) return false;
        return Boolean.TRUE.equals(draft.getLimit().get("supervisorRequired"));
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static String nz(String s) { return s == null ? "" : s; }
}
