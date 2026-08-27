package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CashLimitCheckTool implements McpTool {
    private static final long TELLER_MAX_CASH = 100_000_000L;

    @Override
    public String getId() { return "cash.limit.check"; }

    @Override
    public String getName() { return "cash_limit_check"; }

    @Override
    public String getDescription() { return "Kiểm tra hạn mức tiền mặt của Teller và số dư khả dụng khi rút."; }

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
        return List.of("cash_deposit", "cash_withdrawal", "dynamic_autonomous");
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "amount", Map.of("type", "number", "description", "Số tiền giao dịch tiền mặt"),
                "availableBalance", Map.of("type", "number", "description", "Số dư khả dụng tài khoản"),
                "transactionType", Map.of("type", "string", "description", "CASH_DEPOSIT hoặc CASH_WITHDRAWAL")
            ),
            "required", List.of("amount", "transactionType")
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        long amount = args != null && args.get("amount") != null ? ((Number) args.get("amount")).longValue() : 0L;
        String type = args != null && args.get("transactionType") != null ? String.valueOf(args.get("transactionType")) : "CASH_DEPOSIT";
        Long balance = args != null && args.get("availableBalance") != null ? ((Number) args.get("availableBalance")).longValue() : null;

        boolean supervisorRequired = amount > TELLER_MAX_CASH;
        boolean balanceSufficient = true;

        if ("CASH_WITHDRAWAL".equals(type) && balance != null) {
            balanceSufficient = balance >= amount;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("amount", amount);
        result.put("tellerMaxAmount", TELLER_MAX_CASH);
        result.put("supervisorRequired", supervisorRequired);
        result.put("balanceSufficient", balanceSufficient);
        result.put("sufficientFunds", balanceSufficient);
        result.put("reason", supervisorRequired ? "Số tiền vượt hạn mức GDV (100.000.000 VND), cần Kiểm soát viên duyệt." : "Trong hạn mức GDV tự phê duyệt.");
        result.put("summary", result.get("reason"));
        return result;
    }
}
