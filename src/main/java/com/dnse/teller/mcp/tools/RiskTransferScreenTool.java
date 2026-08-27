package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RiskTransferScreenTool implements McpTool {
    @Override
    public String getId() { return "risk.transfer.screen"; }

    @Override
    public String getName() { return "risk_transfer_screen"; }

    @Override
    public String getDescription() { return "Mô phỏng kiểm tra AML/fraud cho giao dịch chuyển tiền."; }

    @Override
    public String getRisk() { return "CONTROL_READ"; }

    @Override
    public boolean isSideEffect() { return false; }

    @Override
    public boolean isRequiresIdempotency() { return false; }

    @Override
    public List<String> getAllowedCallers() { return List.of("risk_assistant"); }

    @Override
    public List<String> getWorkflows() { return List.of("domestic_transfer"); }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "amount", Map.of("type", "number"),
                "beneficiaryAccount", Map.of("type", "string")
            )
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        long amount = 0;
        String beneficiaryAccount = "";
        if (args != null) {
            if (args.get("amount") != null) {
                amount = ((Number) args.get("amount")).longValue();
            }
            if (args.get("beneficiaryAccount") != null) {
                beneficiaryAccount = String.valueOf(args.get("beneficiaryAccount"));
            }
        }

        boolean flagged = amount >= 200_000_000L || beneficiaryAccount.endsWith("999");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("decision", flagged ? "REVIEW" : "PASS");
        result.put("alerts", flagged ? List.of("Giao dịch cần rà soát bổ sung theo kịch bản mock.") : List.of());
        result.put("model", "MOCK_RULESET_2026.1");
        return result;
    }
}
