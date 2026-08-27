package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import com.dnse.teller.mock.MockBankingDataService;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SavingsProductAdvisorTool implements McpTool {

    private final MockBankingDataService mockService;

    public SavingsProductAdvisorTool(MockBankingDataService mockService) {
        this.mockService = mockService;
    }

    @Override
    public String getId() { return "savings.product.advisor"; }

    @Override
    public String getName() { return "savings_product_advisor"; }

    @Override
    public String getDescription() { return "Tư vấn và mô phỏng tối ưu hóa lãi suất tiền gửi tiết kiệm theo kỳ hạn và dòng tiền."; }

    @Override
    public String getRisk() { return "READ_SAFE"; }

    @Override
    public boolean isSideEffect() { return false; }

    @Override
    public boolean isRequiresIdempotency() { return false; }

    @Override
    public List<String> getAllowedCallers() {
        return List.of("dynamic_tool_agent", "policy_agent", "business_orchestrator");
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
                "amount", Map.of("type", "integer", "description", "Số tiền dự kiến gửi (VND)"),
                "termMonths", Map.of("type", "integer", "description", "Kỳ hạn dự kiến gửi (tháng, ví dụ 1, 3, 6, 12, 24)")
            )
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        Long amount = args != null && args.get("amount") != null ? ((Number) args.get("amount")).longValue() : 100_000_000L;
        Integer term = args != null && args.get("termMonths") != null ? ((Number) args.get("termMonths")).intValue() : 6;
        return mockService.adviseSavingsPlan(amount, term);
    }
}
