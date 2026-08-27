package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CashDraftValidateTool implements McpTool {
    private static final List<String> REQUIRED_FIELDS = List.of(
        "accountRef", "accountNumber", "amount", "transactionType"
    );

    @Override
    public String getId() { return "cash.draft.validate"; }

    @Override
    public String getName() { return "cash_draft_validate"; }

    @Override
    public String getDescription() { return "Kiểm tra các trường bắt buộc của màn hình nộp/rút tiền."; }

    @Override
    public String getRisk() { return "DRAFT"; }

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
                "accountRef", Map.of("type", "string"),
                "accountNumber", Map.of("type", "string"),
                "amount", Map.of("type", "number"),
                "transactionType", Map.of("type", "string")
            )
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        List<String> missing = new ArrayList<>();
        List<String> blockers = new ArrayList<>();

        if (args == null) {
            missing.addAll(REQUIRED_FIELDS);
        } else {
            for (String field : REQUIRED_FIELDS) {
                Object val = args.get(field);
                if (val == null || (val instanceof String && ((String) val).trim().isEmpty())) {
                    missing.add(field);
                }
            }
        }

        String accountStatus = args != null && args.get("accountStatus") != null ? String.valueOf(args.get("accountStatus")) : null;
        Object limitObj = args != null ? args.get("limit") : null;
        Boolean sufficientFunds = null;
        if (limitObj instanceof Map<?, ?> map) {
            Object sf = map.get("sufficientFunds");
            if (sf instanceof Boolean b) {
                sufficientFunds = b;
            }
        }

        if (Boolean.FALSE.equals(sufficientFunds)) {
            blockers.add("INSUFFICIENT_AVAILABLE_BALANCE");
        }

        boolean valid = missing.isEmpty() && "ACTIVE".equals(accountStatus) && !Boolean.FALSE.equals(sufficientFunds);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", valid);
        result.put("missingFields", missing);
        result.put("blockers", blockers);
        return result;
    }
}
