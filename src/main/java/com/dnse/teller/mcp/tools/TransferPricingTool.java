package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TransferPricingTool implements McpTool {
    @Override
    public String getId() { return "pricing.transfer.fee"; }

    @Override
    public String getName() { return "pricing_transfer_fee"; }

    @Override
    public String getDescription() { return "Tính phí chuyển khoản tại quầy theo dữ liệu mock."; }

    @Override
    public String getRisk() { return "READ_SAFE"; }

    @Override
    public boolean isSideEffect() { return false; }

    @Override
    public boolean isRequiresIdempotency() { return false; }

    @Override
    public List<String> getAllowedCallers() {
        return List.of("policy_agent", "dynamic_tool_agent", "business_orchestrator");
    }

    @Override
    public List<String> getWorkflows() {
        return List.of("domestic_transfer", "dynamic_autonomous", "policy_assistance");
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "amount", Map.of("type", "number", "description", "Số tiền chuyển khoản (VND)")
            ),
            "required", List.of("amount")
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        long amount = 0;
        if (args != null && args.get("amount") != null) {
            amount = ((Number) args.get("amount")).longValue();
        }

        long fee = amount > 100_000_000L ? 25_000L : 15_000L;
        long vat = Math.round(fee * 0.1);
        long totalFee = fee + vat;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("amount", amount);
        result.put("fee", fee);
        result.put("vat", vat);
        result.put("totalFee", totalFee);
        result.put("currency", "VND");
        result.put("mock", true);
        result.put("summary", String.format("Phí chuyển khoản cho số tiền %,d VND: Phí cơ bản %,d VND + VAT 10%% (%,d VND) = Tổng phí %,d VND.", amount, fee, vat, totalFee));
        return result;
    }
}
