package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import com.dnse.teller.mock.MockBankingDataService;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MCP Tool: Trích lục lịch sử tương tác 360 độ của khách hàng (Contact Center, VOC Khiếu nại, Sự cố giao dịch Timeout/Declined trên Mobile/ATM/POS).
 */
@Component
public class CustomerInteractionHistoryTool implements McpTool {

    private final MockBankingDataService mockService;

    public CustomerInteractionHistoryTool(MockBankingDataService mockService) {
        this.mockService = mockService;
    }

    @Override
    public String getId() { return "customer.interaction.history"; }

    @Override
    public String getName() { return "customer_interaction_history"; }

    @Override
    public String getDescription() {
        return "Trích lục toàn diện lịch sử tương tác khách hàng: cuộc gọi tổng đài Contact Center (1900), phản ánh/khiếu nại VOC, đánh giá CSAT/NPS, và lịch sử sự cố lỗi giao dịch (Timeout Napas, Declined POS, lỗi nuốt thẻ/khóa PIN ATM, OTP chậm).";
    }

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
        return List.of("dynamic_autonomous", "policy_assistance", "cash_deposit", "cash_withdrawal", "domestic_transfer");
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
        return mockService.getCustomerInteractionHistory(ref);
    }
}
