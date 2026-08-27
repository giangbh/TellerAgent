package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import com.dnse.teller.mock.MockBankingDataService;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AccountResolveTool implements McpTool {

    private final MockBankingDataService mockService;

    public AccountResolveTool(MockBankingDataService mockService) {
        this.mockService = mockService;
    }

    @Override
    public String getId() { return "account.resolve.by_number"; }

    @Override
    public String getName() { return "account_resolve_by_number"; }

    @Override
    public String getDescription() { 
        return "Tra cứu thông tin chủ tài khoản, trạng thái hoạt động và số dư khả dụng từ số tài khoản (Account Name Lookup & Balance Check)."; 
    }

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
        return mockService.resolveAccount(num);
    }
}
