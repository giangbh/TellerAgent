package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import com.dnse.teller.mock.MockBankingDataService;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CustomerAccountsSummaryTool implements McpTool {

    private final MockBankingDataService mockService;

    public CustomerAccountsSummaryTool(MockBankingDataService mockService) {
        this.mockService = mockService;
    }

    @Override
    public String getId() { return "customer.accounts.summary"; }

    @Override
    public String getName() { return "customer_accounts_summary"; }

    @Override
    public String getDescription() { return "Tổng hợp toàn bộ tài khoản, số dư tiền gửi tiết kiệm và hạn mức của khách hàng."; }

    @Override
    public String getRisk() { return "SENSITIVE_READ"; }

    @Override
    public boolean isSideEffect() { return false; }

    @Override
    public boolean isRequiresIdempotency() { return false; }

    @Override
    public List<String> getAllowedCallers() {
        return List.of("dynamic_tool_agent", "customer_context_agent", "business_orchestrator");
    }

    @Override
    public List<String> getWorkflows() {
        return List.of("dynamic_autonomous", "domestic_transfer", "cash_deposit", "cash_withdrawal", "policy_assistance");
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "customerRef", Map.of("type", "string", "description", "Mã CIF hoặc số CCCD/SĐT khách hàng")
            )
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        String ref = args != null && args.get("customerRef") != null ? String.valueOf(args.get("customerRef")) : "CIF-0001842";
        return mockService.getCustomerAccountsSummary(ref);
    }
}
