package com.dnse.teller.mcp.tools;

import com.dnse.teller.gateway.InternalApiGateway;
import com.dnse.teller.mcp.McpTool;
import com.dnse.teller.mcp.ToolCallContext;
import com.dnse.teller.security.AuthorizationException;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CoreTransferExecuteTool implements McpTool {
    private final InternalApiGateway gateway;

    public CoreTransferExecuteTool(InternalApiGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public String getId() { return "core.transfer.execute"; }

    @Override
    public String getName() { return "core_transfer_execute"; }

    @Override
    public String getDescription() { return "Mock posting giao dịch chuyển khoản vào core banking qua Internal API Gateway."; }

    @Override
    public String getRisk() { return "FINANCIAL_WRITE"; }

    @Override
    public boolean isSideEffect() { return true; }

    @Override
    public boolean isRequiresIdempotency() { return true; }

    @Override
    public List<String> getAllowedCallers() { return List.of("business_orchestrator"); }

    @Override
    public List<String> getWorkflows() { return List.of("domestic_transfer"); }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "amount", Map.of("type", "number"),
                "beneficiaryAccount", Map.of("type", "string"),
                "beneficiaryName", Map.of("type", "string"),
                "bankCode", Map.of("type", "string"),
                "sourceAccountRef", Map.of("type", "string")
            ),
            "required", List.of("amount", "beneficiaryAccount")
        );
    }

    /**
     * P0-1/P0-2: bản không có ngữ cảnh luôn bị từ chối. Trước đây hàm này tự
     * hardcode caller "business_orchestrator" khi gọi gateway, biến kiểm tra
     * quyền của gateway thành code chết.
     */
    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        throw new AuthorizationException(
                "core.transfer.execute chỉ chạy được với PostingAuthorization hợp lệ.",
                "POSTING_AUTHORIZATION_MISSING", 403);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey, ToolCallContext context) throws Exception {
        if (context == null || !context.hasPostingAuthorization()) {
            throw new AuthorizationException(
                    "core.transfer.execute yêu cầu PostingAuthorization.", "POSTING_AUTHORIZATION_MISSING", 403);
        }
        return gateway.dispatchCorePosting(context.postingAuthorization(), getId(), args);
    }
}
