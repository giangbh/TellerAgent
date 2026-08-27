package com.dnse.teller.mcp.tools;

import com.dnse.teller.mcp.McpTool;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BranchDirectoryTool implements McpTool {
    private static final List<Map<String, String>> BRANCHES = List.of(
        Map.of(
            "branchCode", "CN-HN-01",
            "branchName", "Chi nhánh Trụ sở Hà Nội",
            "city", "Hà Nội",
            "address", "Số 54 Liễu Giai, Ba Đình, Hà Nội",
            "hotline", "1900 1234",
            "workingHours", "08:00 - 17:00 (Thứ 2 - Thứ 6)"
        ),
        Map.of(
            "branchCode", "CN-HN-02",
            "branchName", "Phòng giao dịch Hoàn Kiếm",
            "city", "Hà Nội",
            "address", "Số 15 Ngô Quyền, Hoàn Kiếm, Hà Nội",
            "hotline", "1900 1235",
            "workingHours", "08:00 - 17:00 (Thứ 2 - Thứ 6)"
        ),
        Map.of(
            "branchCode", "CN-HCM-01",
            "branchName", "Chi nhánh Sài Gòn",
            "city", "TP. Hồ Chí Minh",
            "address", "Số 120 Nguyễn Thị Minh Khai, Quận 3, TP. Hồ Chí Minh",
            "hotline", "1900 5678",
            "workingHours", "08:00 - 17:00 (Thứ 2 - Thứ 6)"
        ),
        Map.of(
            "branchCode", "CN-DN-01",
            "branchName", "Chi nhánh Đà Nẵng",
            "city", "Đà Nẵng",
            "address", "Số 88 Nguyễn Văn Linh, Hải Châu, Đà Nẵng",
            "hotline", "1900 9999",
            "workingHours", "08:00 - 17:00 (Thứ 2 - Thứ 6)"
        )
    );

    @Override
    public String getId() { return "branch.directory.lookup"; }

    @Override
    public String getName() { return "branch_directory_lookup"; }

    @Override
    public String getDescription() { return "Tra cứu mạng lưới chi nhánh, phòng giao dịch, địa chỉ và hotline trên toàn quốc."; }

    @Override
    public String getRisk() { return "READ_SAFE"; }

    @Override
    public boolean isSideEffect() { return false; }

    @Override
    public boolean isRequiresIdempotency() { return false; }

    @Override
    public List<String> getAllowedCallers() {
        return List.of("dynamic_tool_agent", "policy_agent", "business_orchestrator");
    }

    @Override
    public List<String> getWorkflows() {
        return List.of("dynamic_autonomous", "domestic_transfer", "policy_assistance", "cash_deposit", "cash_withdrawal");
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "city", Map.of("type", "string", "description", "Tên thành phố hoặc khu vực cần tra cứu (ví dụ: Hà Nội, TP. Hồ Chí Minh, Đà Nẵng)")
            )
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) {
        String city = args != null && args.get("city") != null ? String.valueOf(args.get("city")).toLowerCase().trim() : "";
        List<Map<String, String>> matches = new ArrayList<>();

        for (Map<String, String> b : BRANCHES) {
            if (city.isEmpty() || b.get("city").toLowerCase().contains(city) || b.get("branchName").toLowerCase().contains(city)) {
                matches.add(b);
            }
        }

        if (matches.isEmpty()) {
            matches.addAll(BRANCHES);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalFound", matches.size());
        result.put("branches", matches);
        result.put("summary", String.format("Tìm thấy %d điểm giao dịch phù hợp.", matches.size()));
        return result;
    }
}
