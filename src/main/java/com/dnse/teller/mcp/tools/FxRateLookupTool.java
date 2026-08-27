package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FxRateLookupTool implements McpTool {
    private static final Map<String, double[]> FX_RATES = Map.of(
        "USD", new double[]{25450, 25480},
        "EUR", new double[]{27200, 27500},
        "JPY", new double[]{165.5, 168.2},
        "GBP", new double[]{32800, 33200},
        "SGD", new double[]{19100, 19400},
        "AUD", new double[]{16600, 16900}
    );

    @Override
    public String getId() { return "fx.rate.lookup"; }

    @Override
    public String getName() { return "fx_rate_lookup"; }

    @Override
    public String getDescription() { return "Tra cứu tỷ giá ngoại tệ trực tiếp và tính toán quy đổi tiền tệ theo thời gian thực."; }

    @Override
    public String getRisk() { return "READ_SAFE"; }

    @Override
    public boolean isSideEffect() { return false; }

    @Override
    public boolean isRequiresIdempotency() { return false; }

    @Override
    public List<String> getAllowedCallers() {
        return List.of("dynamic_tool_agent", "policy_agent", "transaction_draft_agent", "business_orchestrator");
    }

    @Override
    public List<String> getWorkflows() {
        return List.of("dynamic_autonomous", "domestic_transfer", "policy_assistance", "cash_deposit", "cash_withdrawal");
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "currency", Map.of("type", "string", "description", "Mã ngoại tệ (USD, EUR, JPY, GBP, SGD, AUD)"),
                "amount", Map.of("type", "number", "description", "Số tiền ngoại tệ cần quy đổi (tùy chọn)")
            ),
            "required", List.of("currency")
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        String currency = "USD";
        if (args != null && args.get("currency") != null) {
            currency = String.valueOf(args.get("currency")).toUpperCase().trim();
        }

        double[] rates = FX_RATES.getOrDefault(currency, new double[]{25450, 25480});
        double buyRate = rates[0];
        double sellRate = rates[1];

        Long amount = null;
        Long convertedBuyVnd = null;
        Long convertedSellVnd = null;

        if (args != null && args.get("amount") != null) {
            amount = ((Number) args.get("amount")).longValue();
            convertedBuyVnd = Math.round(amount * buyRate);
            convertedSellVnd = Math.round(amount * sellRate);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currency", currency);
        result.put("buyRate", buyRate);
        result.put("sellRate", sellRate);
        result.put("effectiveDate", "2026-08-27");
        result.put("bankSource", "Ngân hàng TMCP Quốc tế (Live FX Mock)");

        if (amount != null) {
            result.put("amount", amount);
            result.put("convertedBuyVnd", convertedBuyVnd);
            result.put("convertedSellVnd", convertedSellVnd);
            result.put("summary", String.format("%d %s = %,d VND (mua) / %,d VND (bán)", amount, currency, convertedBuyVnd, convertedSellVnd));
        } else {
            result.put("summary", String.format("Tỷ giá %s: Mua vào %,.0f VND, Bán ra %,.0f VND", currency, buyRate, sellRate));
        }

        return result;
    }
}
