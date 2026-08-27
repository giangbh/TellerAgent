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
    public List<String> getAllowedCallers() { return List.of("policy_agent"); }

    @Override
    public List<String> getWorkflows() { return List.of("domestic_transfer"); }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "amount", Map.of("type", "number", "description", "Số tiền chuyển")
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
        long fee = Math.min(1_000_000L, Math.max(10_000L, Math.round(amount * 0.0003)));
        long vat = Math.round(fee * 0.1);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("amount", amount);
        result.put("fee", fee);
        result.put("vat", vat);
        result.put("totalFee", fee + vat);
        result.put("currency", "VND");
        result.put("mock", true);
        return result;
    }
}
