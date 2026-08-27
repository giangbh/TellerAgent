package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AccountResolveTool implements McpTool {
    @Override
    public String getId() { return "account.resolve.by_number"; }

    @Override
    public String getName() { return "account_resolve_by_number"; }

    @Override
    public String getDescription() { return "Đối chiếu số tài khoản và trả thông tin chủ tài khoản mock."; }

    @Override
    public String getRisk() { return "SENSITIVE_READ"; }

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
                "accountNumber", Map.of("type", "string", "description", "Số tài khoản cần đối chiếu")
            ),
            "required", List.of("accountNumber")
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        String accountNumber = args != null && args.get("accountNumber") != null ? String.valueOf(args.get("accountNumber")) : "";
        boolean known = "3456789".equals(accountNumber);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountRef", known ? "ACC-CASH-3456789" : "ACC-CASH-" + accountNumber);
        result.put("accountNumber", accountNumber);
        result.put("accountNoMasked", maskAccount(accountNumber));
        result.put("accountHolder", known ? "Nguyễn Minh Anh" : "Chủ tài khoản Mock");
        result.put("status", !accountNumber.isEmpty() ? "ACTIVE" : "NOT_FOUND");
        result.put("currency", "VND");
        result.put("availableBalance", known ? 180_000_000L : 90_000_000L);
        return result;
    }

    private String maskAccount(String value) {
        if (value == null || value.length() <= 4) return value;
        return "•".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }
}
