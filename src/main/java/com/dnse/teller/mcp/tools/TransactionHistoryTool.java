package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import com.dnse.teller.mock.MockBankingDataService;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TransactionHistoryTool implements McpTool {

    private final MockBankingDataService mockService;

    public TransactionHistoryTool(MockBankingDataService mockService) {
        this.mockService = mockService;
    }

    @Override
    public String getId() { return "statement.transaction.history"; }

    @Override
    public String getName() { return "statement_transaction_history"; }

    @Override
    public String getDescription() { 
        return "Tra cứu và in trích lục lịch sử giao dịch sao kê, biến động số dư, thống kê dòng tiền thu/chi (Inflow/Outflow) của tài khoản ngân hàng hoặc mã CIF khách hàng (Statement & Transaction History). Dùng khi GDV yêu cầu in sao kê, tra cứu lịch sử giao dịch, kiểm tra tiền vào/tiền ra."; 
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
        return List.of("dynamic_autonomous", "domestic_transfer", "cash_deposit", "cash_withdrawal");
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "accountNumber", Map.of("type", "string", "description", "Số tài khoản ngân hàng cần in sao kê lịch sử giao dịch"),
                "customerRef", Map.of("type", "string", "description", "Mã CIF khách hàng")
            )
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        String account = args != null && args.get("accountNumber") != null ? String.valueOf(args.get("accountNumber")) : "3456789";
        String ref = args != null && args.get("customerRef") != null ? String.valueOf(args.get("customerRef")) : "CIF-0001842";
        return mockService.getTransactionHistory(account, ref);
    }
}
