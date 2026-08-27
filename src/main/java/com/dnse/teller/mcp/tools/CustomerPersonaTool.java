package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import com.dnse.teller.mock.MockBankingDataService;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CustomerPersonaTool implements McpTool {

    private final MockBankingDataService mockService;

    public CustomerPersonaTool(MockBankingDataService mockService) {
        this.mockService = mockService;
    }

    @Override
    public String getId() { return "customer.persona.analytics"; }

    @Override
    public String getName() { return "customer_persona_analytics"; }

    @Override
    public String getDescription() { return "Phân tích chân dung 360 độ: nhóm hành vi, kênh ưa thích, độ gắn bó và khẩu vị đầu tư."; }

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
        return List.of("dynamic_autonomous", "policy_assistance", "domestic_transfer");
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
        return mockService.getCustomerPersona(ref);
    }
}
