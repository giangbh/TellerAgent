package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CustomerAccountsTool implements McpTool {
    @Override
    public String getId() { return "customer.accounts.list"; }

    @Override
    public String getName() { return "customer_accounts_list"; }

    @Override
    public String getDescription() { return "Liệt kê tài khoản nguồn dưới dạng che số."; }

    @Override
    public String getRisk() { return "SENSITIVE_READ"; }

    @Override
    public boolean isSideEffect() { return false; }

    @Override
    public boolean isRequiresIdempotency() { return false; }

    @Override
    public List<String> getAllowedCallers() { return List.of("customer_context_agent"); }

    @Override
    public List<String> getWorkflows() {
        return List.of("domestic_transfer", "cash_withdrawal");
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "customerRef", Map.of("type", "string", "description", "Mã CIF khách hàng")
            )
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        List<Map<String, Object>> accounts = new ArrayList<>();
        accounts.add(Map.of(
            "accountRef", "ACC-001",
            "accountNoMasked", "•••• 6789",
            "currency", "VND",
            "availableBalance", 250_000_000L
        ));
        accounts.add(Map.of(
            "accountRef", "ACC-002",
            "accountNoMasked", "•••• 2486",
            "currency", "VND",
            "availableBalance", 42_500_000L
        ));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accounts", accounts);
        return result;
    }
}
