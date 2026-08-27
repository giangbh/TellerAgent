package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TransferLimitCheckTool implements McpTool {
    @Override
    public String getId() { return "transfer.limit.check"; }

    @Override
    public String getName() { return "transfer_limit_check"; }

    @Override
    public String getDescription() { return "Kiểm tra hạn mức Teller và yêu cầu cấp phê duyệt."; }

    @Override
    public String getRisk() { return "CONTROL_READ"; }

    @Override
    public boolean isSideEffect() { return false; }

    @Override
    public boolean isRequiresIdempotency() { return false; }

    @Override
    public List<String> getAllowedCallers() { return List.of("transaction_draft_agent"); }

    @Override
    public List<String> getWorkflows() { return List.of("domestic_transfer"); }

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
        long tellerLimit = 100_000_000L;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("amount", amount);
        result.put("tellerLimit", tellerLimit);
        result.put("withinTellerLimit", amount <= tellerLimit);
        result.put("supervisorRequired", amount > tellerLimit);
        return result;
    }
}
