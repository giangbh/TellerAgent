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
    public List<String> getAllowedCallers() {
        return List.of("customer_context_agent", "dynamic_tool_agent", "business_orchestrator");
    }

    @Override
    public List<String> getWorkflows() {
        return List.of("domestic_transfer", "cash_withdrawal", "dynamic_autonomous", "policy_assistance", "cash_deposit");
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
        result.put("summary", "Khách hàng có 2 tài khoản khả dụng: •••• 6789 (250 tr VND) và •••• 2486 (42,5 tr VND).");
        return result;
    }
}
