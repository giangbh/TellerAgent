package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RiskCashScreenTool implements McpTool {
    @Override
    public String getId() { return "risk.cash.screen"; }

    @Override
    public String getName() { return "risk_cash_screen"; }

    @Override
    public String getDescription() { return "Mô phỏng kiểm tra risk cho giao dịch tiền mặt."; }

    @Override
    public String getRisk() { return "CONTROL_READ"; }

    @Override
    public boolean isSideEffect() { return false; }

    @Override
    public boolean isRequiresIdempotency() { return false; }

    @Override
    public List<String> getAllowedCallers() { return List.of("risk_assistant"); }

    @Override
    public List<String> getWorkflows() { return List.of("cash_deposit", "cash_withdrawal"); }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "amount", Map.of("type", "number"),
                "limit", Map.of("type", "object")
            )
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        long amount = 0;
        boolean insufficientFunds = false;

        if (args != null) {
            if (args.get("amount") != null) {
                amount = ((Number) args.get("amount")).longValue();
            }
            Object limitObj = args.get("limit");
            if (limitObj instanceof Map<?, ?> map) {
                Object sf = map.get("sufficientFunds");
                if (Boolean.FALSE.equals(sf)) {
                    insufficientFunds = true;
                }
            }
        }

        boolean flagged = amount >= 300_000_000L || insufficientFunds;
        List<String> alerts = new ArrayList<>();
        if (insufficientFunds) {
            alerts.add("Số dư khả dụng không đủ cho giao dịch rút tiền mock.");
        } else if (amount >= 300_000_000L) {
            alerts.add("Giao dịch tiền mặt giá trị cao cần rà soát bổ sung.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("decision", flagged ? "REVIEW" : "PASS");
        result.put("alerts", alerts);
        result.put("model", "MOCK_CASH_RULESET_2026.1");
        return result;
    }
}
