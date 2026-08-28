package com.dnse.teller.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Một lần xác nhận, gắn với danh tính cụ thể.
 *
 * Thay cho cờ boolean trước đây: một cờ boolean không trả lời được câu hỏi
 * "ai đã xác nhận" — mà đó lại chính là câu hỏi đầu tiên của thanh tra.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApprovalRecord {
    private String actorId;
    private String actorName;
    private String actorRole;
    private String at;

    /** Với xác nhận của khách hàng: bằng chứng khách đã đồng ý (OTP / SIGNATURE / DOCUMENT). */
    private String evidenceType;
    private String evidenceRef;

    /** Với xác nhận của khách hàng: GDV đã ghi nhận bằng chứng đó. */
    private String recordedByActorId;

    public ApprovalRecord() {}

    public ApprovalRecord(String actorId, String actorName, String actorRole, String at) {
        this.actorId = actorId;
        this.actorName = actorName;
        this.actorRole = actorRole;
        this.at = at;
    }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }

    public String getAt() { return at; }
    public void setAt(String at) { this.at = at; }

    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String evidenceType) { this.evidenceType = evidenceType; }

    public String getEvidenceRef() { return evidenceRef; }
    public void setEvidenceRef(String evidenceRef) { this.evidenceRef = evidenceRef; }

    public String getRecordedByActorId() { return recordedByActorId; }
    public void setRecordedByActorId(String recordedByActorId) { this.recordedByActorId = recordedByActorId; }
}
