package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BankDirectoryTool implements McpTool {
    private static final Map<String, Map<String, String>> BANKS = Map.of(
        "VCB", Map.of("bankCode", "VCB", "bankName", "Ngân hàng TMCP Ngoại thương Việt Nam", "bin", "970436", "fastTransferSupported", "true"),
        "BIDV", Map.of("bankCode", "BIDV", "bankName", "Ngân hàng TMCP Đầu tư và Phát triển Việt Nam", "bin", "970418", "fastTransferSupported", "true"),
        "CTG", Map.of("bankCode", "CTG", "bankName", "Ngân hàng TMCP Công thương Việt Nam", "bin", "970415", "fastTransferSupported", "true"),
        "TCB", Map.of("bankCode", "TCB", "bankName", "Ngân hàng TMCP Kỹ thương Việt Nam", "bin", "970407", "fastTransferSupported", "true")
    );

    @Override
    public String getId() { return "bank.directory.lookup"; }

    @Override
    public String getName() { return "bank_directory_lookup"; }

    @Override
    public String getDescription() { return "Tra cứu thông tin ngân hàng thụ hưởng từ mã định danh."; }

    @Override
    public String getRisk() { return "READ_SAFE"; }

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
        return List.of("domestic_transfer", "dynamic_autonomous", "policy_assistance");
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "bankCode", Map.of("type", "string", "description", "Mã ngân hàng (VCB, BIDV, CTG, TCB)")
            ),
            "required", List.of("bankCode")
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        String code = args != null && args.get("bankCode") != null ? String.valueOf(args.get("bankCode")).toUpperCase() : "VCB";
        Map<String, String> info = BANKS.get(code);

        if (info == null) {
            info = Map.of(
                "bankCode", code,
                "bankName", "Ngân hàng TMCP " + code,
                "bin", "970499",
                "fastTransferSupported", "true"
            );
        }

        Map<String, Object> result = new LinkedHashMap<>(info);
        result.put("summary", String.format("Ngân hàng: %s (Mã: %s, BIN: %s, Chuyển nhanh 24/7: Khả dụng).", info.get("bankName"), code, info.get("bin")));
        return result;
    }
}
