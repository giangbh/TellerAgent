package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import com.dnse.teller.mock.MockBankingDataService;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CustomerCreditScoreTool implements McpTool {

    private final MockBankingDataService mockService;

    public CustomerCreditScoreTool(MockBankingDataService mockService) {
        this.mockService = mockService;
    }

    @Override
    public String getId() { return "customer.credit.score.check"; }

    @Override
    public String getName() { return "customer_credit_score_check"; }

    @Override
    public String getDescription() { return "Tra cứu điểm tín dụng nội bộ, hạng CIC và hạn mức vay phê duyệt trước của khách hàng."; }

    @Override
    public String getRisk() { return "SENSITIVE_READ"; }

    @Override
    public boolean isSideEffect() { return false; }

    @Override
    public boolean isRequiresIdempotency() { return false; }

    @Override
    public List<String> getAllowedCallers() {
        return List.of("dynamic_tool_agent", "customer_context_agent", "risk_assistant", "business_orchestrator");
    }

    @Override
    public List<String> getWorkflows() {
        return List.of("dynamic_autonomous", "policy_assistance");
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
        return mockService.getCustomerCreditScore(ref);
    }
}
