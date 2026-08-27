package com.dnse.teller.mcp.tools;

import com.dnse.teller.gateway.InternalApiGateway;
import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CoreCashExecuteTool implements McpTool {
    private final InternalApiGateway gateway;

    public CoreCashExecuteTool(InternalApiGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public String getId() { return "core.cash.execute"; }

    @Override
    public String getName() { return "core_cash_execute"; }

    @Override
    public String getDescription() { return "Mock posting giao dịch nộp/rút tiền vào core banking qua Internal API Gateway."; }

    @Override
    public String getRisk() { return "FINANCIAL_WRITE"; }

    @Override
    public boolean isSideEffect() { return true; }

    @Override
    public boolean isRequiresIdempotency() { return true; }

    @Override
    public List<String> getAllowedCallers() { return List.of("business_orchestrator"); }

    @Override
    public List<String> getWorkflows() { return List.of("cash_deposit", "cash_withdrawal"); }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "amount", Map.of("type", "number"),
                "accountNumber", Map.of("type", "string"),
                "transactionType", Map.of("type", "string")
            ),
            "required", List.of("amount", "accountNumber", "transactionType")
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) throws Exception {
        String workflow = args != null && "CASH_WITHDRAWAL".equals(args.get("transactionType")) ? "cash_withdrawal" : "cash_deposit";
        return gateway.dispatchCorePosting("business_orchestrator", workflow, getId(), args, idempotencyKey);
    }
}
