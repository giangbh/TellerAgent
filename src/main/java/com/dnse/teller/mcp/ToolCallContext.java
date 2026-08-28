package com.dnse.teller.mcp;

import com.dnse.teller.security.PostingAuthorization;

/**
 * Ngữ cảnh của một lời gọi tool.
 *
 * Trước đây caller/workflow là hai tham số String rời rạc và có GIÁ TRỊ MẶC ĐỊNH
 * bằng danh tính đặc quyền cao nhất ("business_orchestrator" / "domestic_transfer"),
 * nên một request JSON-RPC trống rỗng cũng được cấp quyền cao nhất. Nay:
 *
 *  - caller/workflow không còn mặc định, thiếu là từ chối;
 *  - quyền hạch toán không nằm ở chuỗi caller mà ở {@link PostingAuthorization},
 *    thứ mà tầng HTTP không thể tạo ra.
 */
public record ToolCallContext(
        String caller,
        String workflow,
        String idempotencyKey,
        PostingAuthorization postingAuthorization
) {

    /** Ngữ cảnh cho tool chỉ đọc / không side effect. */
    public static ToolCallContext readOnly(String caller, String workflow) {
        return new ToolCallContext(caller, workflow, null, null);
    }

    /** Ngữ cảnh cho tool hạch toán — bắt buộc có giấy phép đã được kiểm chứng. */
    public static ToolCallContext forPosting(PostingAuthorization authorization) {
        if (authorization == null) {
            throw new IllegalArgumentException("PostingAuthorization là bắt buộc cho tool hạch toán.");
        }
        return new ToolCallContext(
                "business_orchestrator",
                authorization.getWorkflow(),
                authorization.getIdempotencyKey(),
                authorization
        );
    }

    public boolean hasPostingAuthorization() {
        return postingAuthorization != null;
    }
}
