package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class PolicySearchTool implements McpTool {
    private static final Pattern CASH_PATTERN = Pattern.compile("nộp|rút|tiền mặt", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    @Override
    public String getId() { return "knowledge.policy.search"; }

    @Override
    public String getName() { return "knowledge_policy_search"; }

    @Override
    public String getDescription() { return "Tra cứu kho quy trình đã phê duyệt và trả citation."; }

    @Override
    public String getRisk() { return "READ_SAFE"; }

    @Override
    public boolean isSideEffect() { return false; }

    @Override
    public boolean isRequiresIdempotency() { return false; }

    @Override
    public List<String> getAllowedCallers() {
        return List.of("policy_agent", "dynamic_tool_agent", "business_orchestrator");
    }

    @Override
    public List<String> getWorkflows() {
        return List.of("domestic_transfer", "cash_deposit", "cash_withdrawal", "policy_assistance", "dynamic_autonomous");
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "query", Map.of("type", "string", "description", "Từ khóa hoặc câu hỏi cần tra cứu")
            )
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        String query = args != null && args.get("query") != null ? String.valueOf(args.get("query")) : "";
        Map<String, Object> result = new LinkedHashMap<>();

        if (CASH_PATTERN.matcher(query).find()) {
            result.put("answer", "GDV phải đối chiếu số tài khoản, chủ tài khoản, số tiền và chứng từ trước khi xác nhận giao dịch tiền mặt.");
            result.put("citations", List.of(
                Map.of(
                    "document", "QTTM-2026",
                    "section", "3.1",
                    "title", "Quy trình giao dịch tiền mặt tại quầy",
                    "effectiveDate", "2026-01-01"
                )
            ));
        } else {
            result.put("answer", "Chuyển khoản tại quầy yêu cầu khách hàng đã xác thực, kiểm tra thông tin người thụ hưởng và xác nhận phí trước khi gửi core.");
            result.put("citations", List.of(
                Map.of(
                    "document", "QTTT-CK-2026",
                    "section", "4.2",
                    "title", "Quy trình chuyển khoản trong nước",
                    "effectiveDate", "2026-01-01"
                ),
                Map.of(
                    "document", "BIEUPHI-2026",
                    "section", "2.1",
                    "title", "Biểu phí dịch vụ tại quầy",
                    "effectiveDate", "2026-04-01"
                )
            ));
        }
        result.put("summary", result.get("answer"));
        return result;
    }
}
