package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import com.dnse.teller.mock.MockBankingDataService;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class NextBestOfferTool implements McpTool {

    private final MockBankingDataService mockService;

    public NextBestOfferTool(MockBankingDataService mockService) {
        this.mockService = mockService;
    }

    @Override
    public String getId() { return "recommendation.nbo.products"; }

    @Override
    public String getName() { return "recommendation_nbo_products"; }

    @Override
    public String getDescription() { return "Gợi ý gói giải pháp và ưu đãi cá nhân hóa (Next-Best-Offer / NBO) phù hợp nhất cho khách hàng."; }

    @Override
    public String getRisk() { return "READ_SAFE"; }

    @Override
    public boolean isSideEffect() { return false; }

    @Override
    public boolean isRequiresIdempotency() { return false; }

    @Override
    public List<String> getAllowedCallers() {
        return List.of("dynamic_tool_agent", "policy_agent", "customer_context_agent", "business_orchestrator");
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
        return mockService.getNextBestOffers(ref);
    }
}
