package com.dnse.teller.security;

/**
 * Giấy phép hạch toán cho MỘT giao dịch cụ thể.
 *
 * P0-1/P0-2: trước đây quyền post core được thể hiện bằng chuỗi
 * "business_orchestrator" — mà chuỗi thì ai cũng gõ được, và tool tài chính còn
 * tự hardcode chuỗi đó khi gọi gateway, khiến kiểm tra quyền thành code chết.
 *
 * Lớp này không có constructor public. Chỉ {@link PostingAuthorizer} (cùng
 * package) mới tạo được, và chỉ sau khi đã tự mình kiểm chứng lại toàn bộ
 * control gate từ dữ liệu đã persist. Vì vậy tầng HTTP không thể dựng ra nó,
 * và LLM cũng không thể "nói" ra nó.
 */
public final class PostingAuthorization {

    private final String sessionId;
    private final String workflow;
    private final String capabilityId;
    private final String idempotencyKey;
    private final long amount;
    private final String tellerActorId;
    private final String supervisorActorId;
    private final int sessionRevision;
    private final String issuedAt;

    PostingAuthorization(String sessionId, String workflow, String capabilityId, String idempotencyKey,
                         long amount, String tellerActorId, String supervisorActorId,
                         int sessionRevision, String issuedAt) {
        this.sessionId = sessionId;
        this.workflow = workflow;
        this.capabilityId = capabilityId;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.tellerActorId = tellerActorId;
        this.supervisorActorId = supervisorActorId;
        this.sessionRevision = sessionRevision;
        this.issuedAt = issuedAt;
    }

    public String getSessionId() { return sessionId; }
    public String getWorkflow() { return workflow; }
    public String getCapabilityId() { return capabilityId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public long getAmount() { return amount; }
    public String getTellerActorId() { return tellerActorId; }
    public String getSupervisorActorId() { return supervisorActorId; }
    public int getSessionRevision() { return sessionRevision; }
    public String getIssuedAt() { return issuedAt; }
}
