package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TransferLimitCheckTool implements McpTool {
    private static final long TELLER_MAX_AMOUNT = 100_000_000L;

    @Override
    public String getId() { return "transfer.limit.check"; }

    @Override
    public String getName() { return "transfer_limit_check"; }

    @Override
    public String getDescription() { return "Kiểm tra hạn mức giao dịch chuyển khoản của Teller."; }

    @Override
    public String getRisk() { return "CONTROL_READ"; }

    @Override
    public boolean isSideEffect() { return false; }

    @Override
    public boolean isRequiresIdempotency() { return false; }

    @Override
    public List<String> getAllowedCallers() {
        return List.of("transaction_draft_agent", "dynamic_tool_agent", "business_orchestrator");
    }

    @Override
    public List<String> getWorkflows() {
        return List.of("domestic_transfer", "dynamic_autonomous");
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "amount", Map.of("type", "number", "description", "Số tiền chuyển khoản")
            ),
            "required", List.of("amount")
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        long amount = 0;
        if (args != null && args.get("amount") != null) {
            amount = ((Number) args.get("amount")).longValue();
        }

        boolean supervisorRequired = amount > TELLER_MAX_AMOUNT;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("amount", amount);
        result.put("tellerMaxAmount", TELLER_MAX_AMOUNT);
        result.put("supervisorRequired", supervisorRequired);
        result.put("reason", supervisorRequired ? "Số tiền vượt hạn mức GDV (100.000.000 VND), cần Kiểm soát viên duyệt." : "Trong hạn mức GDV tự phê duyệt.");
        result.put("summary", result.get("reason"));
        return result;
    }
}
