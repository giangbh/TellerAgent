package com.dnse.teller.model;

import com.dnse.teller.security.AuthenticatedActor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Tập hợp các xác nhận của một giao dịch.
 *
 * P0-3/P0-4: các setter boolean cũ (setCustomer/setTeller/setSupervisor) đã bị
 * gỡ bỏ. Không còn cách nào đặt cờ "đã xác nhận" mà không kèm danh tính. Các
 * getter boolean vẫn được giữ để tương thích với frontend và evaluateControl(),
 * nhưng chúng là giá trị dẫn xuất chỉ-đọc.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Approvals {

    private ApprovalRecord customerRecord;
    private ApprovalRecord tellerRecord;
    private ApprovalRecord supervisorRecord;

    public Approvals() {}

    // ---- Ghi nhận xác nhận: bắt buộc có danh tính đã xác thực ----

    /**
     * GDV ghi nhận việc khách hàng đã đồng ý, kèm bằng chứng độc lập.
     * Hệ thống không tự sinh ra sự đồng ý của khách hàng.
     */
    public ApprovalRecord recordCustomerConsent(AuthenticatedActor recordedBy, String evidenceType, String evidenceRef) {
        ApprovalRecord record = new ApprovalRecord("CUSTOMER", "Khách hàng", "CUSTOMER", Instant.now().toString());
        record.setEvidenceType(evidenceType);
        record.setEvidenceRef(evidenceRef);
        record.setRecordedByActorId(recordedBy.userId());
        this.customerRecord = record;
        return record;
    }

    public ApprovalRecord recordTeller(AuthenticatedActor actor) {
        this.tellerRecord = new ApprovalRecord(actor.userId(), actor.displayName(), actor.role().name(), Instant.now().toString());
        return this.tellerRecord;
    }

    public ApprovalRecord recordSupervisor(AuthenticatedActor actor) {
        this.supervisorRecord = new ApprovalRecord(actor.userId(), actor.displayName(), actor.role().name(), Instant.now().toString());
        return this.supervisorRecord;
    }

    // ---- Giá trị dẫn xuất, chỉ đọc ----

    @JsonProperty(value = "customer", access = JsonProperty.Access.READ_ONLY)
    public boolean isCustomer() { return customerRecord != null; }

    @JsonProperty(value = "teller", access = JsonProperty.Access.READ_ONLY)
    public boolean isTeller() { return tellerRecord != null; }

    @JsonProperty(value = "supervisor", access = JsonProperty.Access.READ_ONLY)
    public boolean isSupervisor() { return supervisorRecord != null; }

    // ---- Bản ghi đầy đủ. Setter chỉ phục vụ Jackson khi nạp lại session từ DB. ----

    public ApprovalRecord getCustomerRecord() { return customerRecord; }
    public void setCustomerRecord(ApprovalRecord customerRecord) { this.customerRecord = customerRecord; }

    public ApprovalRecord getTellerRecord() { return tellerRecord; }
    public void setTellerRecord(ApprovalRecord tellerRecord) { this.tellerRecord = tellerRecord; }

    public ApprovalRecord getSupervisorRecord() { return supervisorRecord; }
    public void setSupervisorRecord(ApprovalRecord supervisorRecord) { this.supervisorRecord = supervisorRecord; }
}
