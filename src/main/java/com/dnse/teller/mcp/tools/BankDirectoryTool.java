package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BankDirectoryTool implements McpTool {
    private static final Map<String, String> BANKS = Map.of(
        "VCB", "Ngân hàng TMCP Ngoại thương Việt Nam",
        "BIDV", "Ngân hàng TMCP Đầu tư và Phát triển Việt Nam",
        "CTG", "Ngân hàng TMCP Công thương Việt Nam",
        "TCB", "Ngân hàng TMCP Kỹ thương Việt Nam"
    );

    @Override
    public String getId() { return "bank.directory.lookup"; }

    @Override
    public String getName() { return "bank_directory_lookup"; }

    @Override
    public String getDescription() { return "Tra cứu mã và tên ngân hàng thụ hưởng."; }

    @Override
    public String getRisk() { return "READ_SAFE"; }

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
                "bankCode", Map.of("type", "string", "description", "Mã ngân hàng (VCB, BIDV, CTG, TCB...)")
            ),
            "required", List.of("bankCode")
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        String bankCode = args != null && args.get("bankCode") != null ? String.valueOf(args.get("bankCode")).toUpperCase() : "";
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bankCode", bankCode);
        result.put("bankName", BANKS.getOrDefault(bankCode, "Ngân hàng thụ hưởng (mock)"));
        result.put("serviceStatus", "AVAILABLE");
        return result;
    }
}
