package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CashLimitCheckTool implements McpTool {
    @Override
    public String getId() { return "cash.limit.check"; }

    @Override
    public String getName() { return "cash_limit_check"; }

    @Override
    public String getDescription() { return "Kiểm tra hạn mức GDV và số dư khả dụng cho giao dịch tiền mặt."; }

    @Override
    public String getRisk() { return "CONTROL_READ"; }

    @Override
    public boolean isSideEffect() { return false; }

    @Override
    public boolean isRequiresIdempotency() { return false; }

    @Override
    public List<String> getAllowedCallers() { return List.of("transaction_draft_agent"); }

    @Override
    public List<String> getWorkflows() { return List.of("cash_deposit", "cash_withdrawal"); }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "amount", Map.of("type", "number"),
                "availableBalance", Map.of("type", "number"),
                "transactionType", Map.of("type", "string")
            )
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        long amount = 0;
        long availableBalance = 0;
        String transactionType = "";

        if (args != null) {
            if (args.get("amount") != null) {
                amount = ((Number) args.get("amount")).longValue();
            }
            if (args.get("availableBalance") != null) {
                availableBalance = ((Number) args.get("availableBalance")).longValue();
            }
            if (args.get("transactionType") != null) {
                transactionType = String.valueOf(args.get("transactionType"));
            }
        }

        boolean withdrawal = "CASH_WITHDRAWAL".equals(transactionType);
        long tellerLimit = 100_000_000L;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("amount", amount);
        result.put("tellerLimit", tellerLimit);
        result.put("withinTellerLimit", amount <= tellerLimit);
        result.put("supervisorRequired", amount > tellerLimit);
        result.put("sufficientFunds", !withdrawal || amount <= availableBalance);
        return result;
    }
}
