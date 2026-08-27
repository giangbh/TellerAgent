package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import com.dnse.teller.mock.MockBankingDataService;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CardServiceManageTool implements McpTool {

    private final MockBankingDataService mockService;

    public CardServiceManageTool(MockBankingDataService mockService) {
        this.mockService = mockService;
    }

    @Override
    public String getId() { return "card.service.manage"; }

    @Override
    public String getName() { return "card_service_manage"; }

    @Override
    public String getDescription() { return "Tra cứu danh sách thẻ ghi nợ/tín dụng, trạng thái thẻ, hạn mức thanh toán POS/Online."; }

    @Override
    public String getRisk() { return "CONTROL_READ"; }

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
        return List.of("dynamic_autonomous", "policy_assistance");
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
        return mockService.getCardServices(ref);
    }
}
