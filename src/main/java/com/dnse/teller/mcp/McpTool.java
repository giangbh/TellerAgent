package com.dnse.teller.mcp;

import java.util.List;
import java.util.Map;

public interface McpTool {
    String getId();
    String getName();
    String getDescription();
    String getRisk();
    boolean isSideEffect();
    boolean isRequiresIdempotency();
    List<String> getAllowedCallers();
    List<String> getWorkflows();
    Map<String, Object> getInputSchema();

    Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) throws Exception;

    /**
     * Tool ghi sổ tài chính. Nhóm này KHÔNG bao giờ được phơi ra qua endpoint
     * JSON-RPC công khai và chỉ chạy được khi có ToolCallContext mang
     * PostingAuthorization hợp lệ.
     */
    default boolean isFinancialWrite() {
        return "FINANCIAL_WRITE".equals(getRisk());
    }

    /**
     * Bản có ngữ cảnh. Mặc định uỷ quyền cho bản 2 tham số để 21 tool chỉ-đọc
     * hiện hữu không phải sửa gì. Riêng tool tài chính override bản này và làm
     * cho bản 2 tham số ném lỗi.
     */
    default Map<String, Object> execute(Map<String, Object> args, String idempotencyKey, ToolCallContext context) throws Exception {
        return execute(args, idempotencyKey);
    }
}
