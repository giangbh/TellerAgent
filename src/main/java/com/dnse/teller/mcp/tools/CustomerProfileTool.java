package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import com.dnse.teller.mock.MockBankingDataService;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CustomerProfileTool implements McpTool {

    private final MockBankingDataService mockService;

    public CustomerProfileTool(MockBankingDataService mockService) {
        this.mockService = mockService;
    }

    @Override
    public String getId() { return "customer.profile.read"; }

    @Override
    public String getName() { return "customer_profile_read"; }

    @Override
    public String getDescription() { return "Đọc hồ sơ khách hàng đã được giới hạn cho phiên Teller."; }

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
        return List.of("domestic_transfer", "cash_deposit", "cash_withdrawal", "policy_assistance", "dynamic_autonomous");
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
        String ref = args != null && args.get("customerRef") != null ? String.valueOf(args.get("customerRef")) : "CIF-0001842";
        return mockService.getCustomerProfile(ref);
    }
}
