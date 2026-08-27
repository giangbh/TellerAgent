package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TransactionDraftValidateTool implements McpTool {
    private static final List<String> REQUIRED_FIELDS = List.of(
        "sourceAccountRef", "beneficiaryAccount", "beneficiaryName", "bankCode", "amount"
    );

    @Override
    public String getId() { return "transaction.draft.validate"; }

    @Override
    public String getName() { return "transaction_draft_validate"; }

    @Override
    public String getDescription() { return "Kiểm tra schema và trường bắt buộc của bản nháp chuyển khoản."; }

    @Override
    public String getRisk() { return "DRAFT"; }

    @Override
    public boolean isSideEffect() { return false; }

    @Override
    public boolean isRequiresIdempotency() { return false; }

    @Override
    public List<String> getAllowedCallers() { return List.of("transaction_draft_agent"); }

    @Override
    public List<String> getWorkflows() { return List.of("domestic_transfer"); }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "sourceAccountRef", Map.of("type", "string"),
                "beneficiaryAccount", Map.of("type", "string"),
                "beneficiaryName", Map.of("type", "string"),
                "bankCode", Map.of("type", "string"),
                "amount", Map.of("type", "number")
            )
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        List<String> missing = new ArrayList<>();
        if (args == null) {
            missing.addAll(REQUIRED_FIELDS);
        } else {
            for (String field : REQUIRED_FIELDS) {
                Object val = args.get(field);
                if (val == null || (val instanceof String && ((String) val).trim().isEmpty())) {
                    missing.add(field);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", missing.isEmpty());
        result.put("missingFields", missing);
        return result;
    }
}
