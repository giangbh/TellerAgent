package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AccountResolveTool implements McpTool {
    @Override
    public String getId() { return "account.resolve.by_number"; }

    @Override
    public String getName() { return "account_resolve_by_number"; }

    @Override
    public String getDescription() { return "Đối chiếu số tài khoản và trả thông tin chủ tài khoản mock."; }

    @Override
    public String getRisk() { return "SENSITIVE_READ"; }

    @Override
    public boolean isSideEffect() { return false; }

    @Override
    public boolean isRequiresIdempotency() { return false; }

    @Override
    public List<String> getAllowedCallers() {
        return List.of("transaction_draft_agent", "dynamic_tool_agent", "business_orchestrator");
    }

    @Override
    public List<String> getWorkflows() {
        return List.of("cash_deposit", "cash_withdrawal", "dynamic_autonomous", "domestic_transfer");
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "accountNumber", Map.of("type", "string", "description", "Số tài khoản cần tra cứu")
            ),
            "required", List.of("accountNumber")
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        String num = args != null && args.get("accountNumber") != null ? String.valueOf(args.get("accountNumber")) : "3456789";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accountRef", "ACC-MOCK-" + num);
        result.put("accountNumber", num);
        result.put("accountNoMasked", "•••• " + (num.length() > 4 ? num.substring(num.length() - 4) : num));
        result.put("accountHolder", "Nguyễn Minh Anh");
        result.put("currency", "VND");
        result.put("status", "ACTIVE");
        result.put("availableBalance", 250_000_000L);
        result.put("summary", String.format("Tài khoản %s: Chủ tài khoản Nguyễn Minh Anh (Trạng thái: ACTIVE, Số dư khả dụng: 250.000.000 VND).", num));
        return result;
    }
}
