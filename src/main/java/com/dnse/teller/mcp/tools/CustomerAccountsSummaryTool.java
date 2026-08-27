package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CustomerAccountsSummaryTool implements McpTool {

    @Override
    public String getId() { return "customer.accounts.summary"; }

    @Override
    public String getName() { return "customer_accounts_summary"; }

    @Override
    public String getDescription() { return "Tổng hợp toàn diện danh mục tài khoản thanh toán, tiền gửi tiết kiệm và hạn mức tín dụng của khách hàng."; }

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
        return List.of("dynamic_autonomous", "domestic_transfer", "cash_deposit", "cash_withdrawal", "policy_assistance");
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

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customerRef", ref);
        result.put("customerName", "Nguyễn Minh Anh");
        result.put("segment", "PRIORITY");
        result.put("totalAvailableBalance", 292_500_000L);
        result.put("totalTermDeposit", 1_500_000_000L);
        result.put("creditLimitTotal", 100_000_000L);
        result.put("accountsCount", 2);
        result.put("termDepositsCount", 1);

        result.put("accounts", List.of(
            Map.of("accountRef", "ACC-001", "accountNoMasked", "•••• 6789", "type", "CURRENT", "balance", 250_000_000L, "currency", "VND"),
            Map.of("accountRef", "ACC-002", "accountNoMasked", "•••• 2486", "type", "CURRENT", "balance", 42_500_000L, "currency", "VND")
        ));

        result.put("termDeposits", List.of(
            Map.of("depositRef", "TD-882194", "amount", 1_500_000_000L, "termMonths", 12, "interestRate", "5.8% / năm", "maturityDate", "2027-04-15")
        ));

        result.put("summary", "Khách hàng Priority sở hữu 2 tài khoản thanh toán (tổng 292,5 tr VND) và 1 sổ tiết kiệm 1,5 tỷ VND.");
        return result;
    }
}
