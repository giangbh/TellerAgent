package com.dnse.teller.orchestrator;

import com.dnse.teller.mcp.McpTool;
import com.dnse.teller.mcp.McpToolRegistry;
import com.dnse.teller.model.Intent;
import com.dnse.teller.model.Plan;
import com.dnse.teller.model.Plan.PlanStep;
import com.dnse.teller.model.Session;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReasoningEngine {
    private final McpToolRegistry toolRegistry;

    private static final List<String[]> BANK_ALIASES = List.of(
        new String[]{"vietcombank", "VCB"},
        new String[]{"vcb", "VCB"},
        new String[]{"bidv", "BIDV"},
        new String[]{"vietinbank", "CTG"},
        new String[]{"ctg", "CTG"},
        new String[]{"techcombank", "TCB"},
        new String[]{"tcb", "TCB"}
    );

    private static final Pattern POLICY_QUERY_PAT = Pattern.compile("quy trình|biểu phí|như thế nào|hướng dẫn|thủ tục|chính sách|điều kiện", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CASH_DEPOSIT_PAT = Pattern.compile("nộp tiền|nộp\\s+\\d", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CASH_WITHDRAWAL_PAT = Pattern.compile("rút tiền|rút\\s+\\d", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern TRANSFER_PAT = Pattern.compile("chuyển khoản|chuyển tiền|\\btransfer\\b|chuyển\\s+\\d", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern FX_PAT = Pattern.compile("tỷ giá|ngoại tệ|\\busd\\b|\\beur\\b|\\bjpy\\b|\\bgbp\\b|\\bsgd\\b|đổi tiền|quy đổi", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern BRANCH_PAT = Pattern.compile("chi nhánh|phòng giao dịch|pgd|địa chỉ|hotline|trụ sở|ở đâu|điểm giao dịch", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SUMMARY_PAT = Pattern.compile("tổng hợp|tất cả tài khoản|bao nhiêu tài khoản|sổ tiết kiệm|tiền gửi|tổng số dư", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern ACCOUNT_PAT = Pattern.compile("\\b\\d{6,14}\\b");
    private static final Pattern NAME_PAT = Pattern.compile("(?:cho|tên|người nhận)\\s+([A-Za-zÀ-ỹĐđ\\s]{3,40}?)(?=\\s+(?:tại|ở|ngân hàng|số tài khoản|stk|tk)|[,.;]|$)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public ReasoningEngine(McpToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public Intent detectIntent(String text, Intent currentIntent) {
        String lower = text != null ? text.toLowerCase() : "";

        // 1. Dynamic FX Rate / Currency Inquiry
        if (FX_PAT.matcher(lower).find()) {
            Map<String, Object> entities = new LinkedHashMap<>();
            Long amount = parseAmount(text);
            if (amount != null) entities.put("amount", amount);

            String currency = "USD";
            if (lower.contains("eur")) currency = "EUR";
            else if (lower.contains("jpy")) currency = "JPY";
            else if (lower.contains("gbp")) currency = "GBP";
            else if (lower.contains("sgd")) currency = "SGD";
            entities.put("currency", currency);

            Intent fxIntent = new Intent("DYNAMIC_FX_LOOKUP", "dynamic_autonomous", 0.98, entities);
            fxIntent.setQuery(text);
            return fxIntent;
        }

        // 2. Dynamic Branch / Location Inquiry
        if (BRANCH_PAT.matcher(lower).find()) {
            Map<String, Object> entities = new LinkedHashMap<>();
            String city = "Hà Nội";
            if (lower.contains("hồ chí minh") || lower.contains("hcm") || lower.contains("sài gòn")) city = "TP. Hồ Chí Minh";
            else if (lower.contains("đà nẵng")) city = "Đà Nẵng";
            entities.put("city", city);

            Intent branchIntent = new Intent("DYNAMIC_BRANCH_LOOKUP", "dynamic_autonomous", 0.98, entities);
            branchIntent.setQuery(text);
            return branchIntent;
        }

        // 3. Dynamic Customer Portfolio & Accounts Summary Inquiry
        if (SUMMARY_PAT.matcher(lower).find()) {
            Intent summaryIntent = new Intent("DYNAMIC_ACCOUNT_SUMMARY", "dynamic_autonomous", 0.98, Map.of());
            summaryIntent.setQuery(text);
            return summaryIntent;
        }

        // 4. Policy Assistance
        if (POLICY_QUERY_PAT.matcher(lower).find()) {
            Intent policyIntent = new Intent("POLICY_ASSISTANCE", "policy_assistance", 0.95, Map.of());
            policyIntent.setQuery(text);
            return policyIntent;
        }

        // 5. Cash Deposit & Withdrawal
        if (CASH_DEPOSIT_PAT.matcher(lower).find()) {
            return new Intent("CASH_DEPOSIT", "cash_deposit", 0.97, extractCashEntities(text, currentIntent != null ? currentIntent.getEntities() : Map.of()));
        }
        if (CASH_WITHDRAWAL_PAT.matcher(lower).find()) {
            return new Intent("CASH_WITHDRAWAL", "cash_withdrawal", 0.97, extractCashEntities(text, currentIntent != null ? currentIntent.getEntities() : Map.of()));
        }

        // 6. Domestic Transfer
        if (TRANSFER_PAT.matcher(lower).find()) {
            return new Intent("DOMESTIC_TRANSFER", "domestic_transfer", 0.94, extractTransferEntities(text, currentIntent != null ? currentIntent.getEntities() : Map.of()));
        }

        // 7. Resume previous state if present
        if (currentIntent != null && "DOMESTIC_TRANSFER".equals(currentIntent.getType())) {
            Intent updated = new Intent(currentIntent.getType(), currentIntent.getWorkflow(), 0.99, extractTransferEntities(text, currentIntent.getEntities()));
            updated.setQuery(currentIntent.getQuery());
            return updated;
        }
        if (currentIntent != null && ("CASH_DEPOSIT".equals(currentIntent.getType()) || "CASH_WITHDRAWAL".equals(currentIntent.getType()))) {
            Intent updated = new Intent(currentIntent.getType(), currentIntent.getWorkflow(), 0.99, extractCashEntities(text, currentIntent.getEntities()));
            updated.setQuery(currentIntent.getQuery());
            return updated;
        }

        // 8. Default Dynamic Tool Discovery for any unclassified prompt
        List<String> discoveredTools = discoverTools(text);
        Intent dynamicIntent = new Intent("DYNAMIC_AUTONOMOUS_TASK", "dynamic_autonomous", 0.88, extractTransferEntities(text, Map.of()));
        dynamicIntent.setQuery(text);
        dynamicIntent.getEntities().put("discoveredTools", discoveredTools);
        return dynamicIntent;
    }

    public Plan proposePlan(Session session) {
        String intentType = session.getIntent() != null ? session.getIntent().getType() : "DYNAMIC_AUTONOMOUS_TASK";

        if ("DYNAMIC_FX_LOOKUP".equals(intentType)) {
            return new Plan(
                "Khám phá công cụ động: Tra cứu tỷ giá và tính toán quy đổi ngoại tệ",
                List.of(new PlanStep("S1", "DELEGATE", "dynamic_tool_agent:fx.rate.lookup", List.of())),
                List.of("Có thông tin tỷ giá", "Có kết quả quy đổi")
            );
        }

        if ("DYNAMIC_BRANCH_LOOKUP".equals(intentType)) {
            return new Plan(
                "Khám phá công cụ động: Tra cứu thông tin mạng lưới chi nhánh & hotline",
                List.of(new PlanStep("S1", "DELEGATE", "dynamic_tool_agent:branch.directory.lookup", List.of())),
                List.of("Có danh sách chi nhánh", "Có địa chỉ & hotline")
            );
        }

        if ("DYNAMIC_ACCOUNT_SUMMARY".equals(intentType)) {
            return new Plan(
                "Khám phá công cụ động: Tổng hợp danh mục tài khoản và tiền gửi tiết kiệm",
                List.of(new PlanStep("S1", "DELEGATE", "dynamic_tool_agent:customer.accounts.summary", List.of())),
                List.of("Có tổng số dư thanh toán", "Có số dư tiền gửi tiết kiệm")
            );
        }

        if ("POLICY_ASSISTANCE".equals(intentType)) {
            return new Plan(
                "Trả lời yêu cầu nghiệp vụ bằng nguồn đã phê duyệt",
                List.of(new PlanStep("S1", "DELEGATE", "policy_agent", List.of())),
                List.of("Có câu trả lời", "Có citation")
            );
        }

        if ("CASH_DEPOSIT".equals(intentType)) {
            return new Plan(
                "Mở màn hình nộp tiền và tự điền bản nháp, không posting",
                List.of(
                    new PlanStep("S1", "DELEGATE", "customer_context_agent", List.of()),
                    new PlanStep("S2", "DELEGATE", "policy_agent", List.of()),
                    new PlanStep("S3", "DELEGATE", "transaction_draft_agent", List.of("S1", "S2")),
                    new PlanStep("S4", "DELEGATE", "risk_assistant", List.of("S3"))
                ),
                List.of("Có đủ trường bắt buộc", "Qua validation", "Có risk decision", "Không posting")
            );
        }

        if ("CASH_WITHDRAWAL".equals(intentType)) {
            return new Plan(
                "Mở màn hình rút tiền và tự điền bản nháp, không posting",
                List.of(
                    new PlanStep("S1", "DELEGATE", "customer_context_agent", List.of()),
                    new PlanStep("S2", "DELEGATE", "policy_agent", List.of()),
                    new PlanStep("S3", "DELEGATE", "transaction_draft_agent", List.of("S1", "S2")),
                    new PlanStep("S4", "DELEGATE", "risk_assistant", List.of("S3"))
                ),
                List.of("Có đủ trường bắt buộc", "Qua validation", "Có risk decision", "Không posting")
            );
        }

        if ("DOMESTIC_TRANSFER".equals(intentType)) {
            return new Plan(
                "Tạo bản nháp chuyển khoản trong nước, không posting",
                List.of(
                    new PlanStep("S1", "DELEGATE", "customer_context_agent", List.of()),
                    new PlanStep("S2", "DELEGATE", "policy_agent", List.of()),
                    new PlanStep("S3", "DELEGATE", "transaction_draft_agent", List.of("S1", "S2")),
                    new PlanStep("S4", "DELEGATE", "risk_assistant", List.of("S3"))
                ),
                List.of("Có đủ trường bắt buộc", "Qua validation", "Có risk decision", "Không posting")
            );
        }

        // Generic Autonomous Dynamic Chaining
        List<String> tools = discoverTools(session.getIntent() != null ? session.getIntent().getQuery() : "");
        List<PlanStep> steps = new ArrayList<>();
        int i = 1;
        for (String t : tools) {
            steps.add(new PlanStep("S" + i, "DELEGATE", "dynamic_tool_agent:" + t, i > 1 ? List.of("S" + (i - 1)) : List.of()));
            i++;
        }

        return new Plan(
            "Tự động lập kế hoạch và điều phối " + steps.size() + " công cụ MCP phù hợp",
            steps,
            List.of("Tất cả công cụ MCP đã thực thi", "Tổng hợp phản hồi cho Teller")
        );
    }

    public List<String> discoverTools(String prompt) {
        String lower = prompt != null ? prompt.toLowerCase() : "";
        List<String> selected = new ArrayList<>();

        if (lower.contains("phí") || lower.contains("fee")) selected.add("pricing.transfer.fee");
        if (lower.contains("hạn mức") || lower.contains("limit")) selected.add("transfer.limit.check");
        if (lower.contains("ngân hàng") || lower.contains("bank") || lower.contains("vcb") || lower.contains("bidv")) selected.add("bank.directory.lookup");
        if (lower.contains("tỷ giá") || lower.contains("usd") || lower.contains("eur")) selected.add("fx.rate.lookup");
        if (lower.contains("chi nhánh") || lower.contains("pgd") || lower.contains("địa chỉ")) selected.add("branch.directory.lookup");
        if (lower.contains("tài khoản") || lower.contains("khách hàng") || lower.contains("cif")) selected.add("customer.profile.read");
        if (lower.contains("quy trình") || lower.contains("quy định")) selected.add("knowledge.policy.search");

        if (selected.isEmpty()) {
            selected.add("knowledge.policy.search");
        }

        return selected;
    }

    public Long parseAmount(String text) {
        if (text == null) return null;
        String normalized = text.toLowerCase().replace(",", ".");

        Pattern amountWithUnit = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(tỷ|ty|triệu|tr|nghìn|ngan|k|usd|eur|jpy|gbp)\\b");
        Matcher m1 = amountWithUnit.matcher(normalized);
        if (m1.find()) {
            double base = Double.parseDouble(m1.group(1));
            String unit = m1.group(2);
            long mult = switch (unit) {
                case "tỷ", "ty" -> 1_000_000_000L;
                case "triệu", "tr" -> 1_000_000L;
                case "nghìn", "ngan", "k" -> 1_000L;
                default -> 1L;
            };
            return Math.round(base * mult);
        }

        Pattern directAmount = Pattern.compile("(?:số tiền|chuyển|rút|nộp|giá)\\s+(\\d{2,})\\b");
        Matcher m2 = directAmount.matcher(normalized);
        if (m2.find()) {
            return Long.parseLong(m2.group(1));
        }
        return null;
    }

    private Map<String, Object> extractTransferEntities(String text, Map<String, Object> previous) {
        Map<String, Object> entities = new LinkedHashMap<>(previous);
        String lower = text != null ? text.toLowerCase() : "";

        Long amount = parseAmount(text);
        if (amount != null) entities.put("amount", amount);

        Matcher accMatcher = ACCOUNT_PAT.matcher(text != null ? text : "");
        String lastAccount = null;
        while (accMatcher.find()) {
            lastAccount = accMatcher.group();
        }
        if (lastAccount != null) entities.put("beneficiaryAccount", lastAccount);

        for (String[] pair : BANK_ALIASES) {
            if (lower.contains(pair[0])) {
                entities.put("bankCode", pair[1]);
                break;
            }
        }

        Matcher nameMatcher = NAME_PAT.matcher(text != null ? text : "");
        if (nameMatcher.find()) {
            entities.put("beneficiaryName", nameMatcher.group(1).trim());
        }

        return entities;
    }

    private Map<String, Object> extractCashEntities(String text, Map<String, Object> previous) {
        Map<String, Object> entities = new LinkedHashMap<>(previous);
        Long amount = parseAmount(text);
        if (amount != null) entities.put("amount", amount);

        Matcher accMatcher = ACCOUNT_PAT.matcher(text != null ? text : "");
        String lastAccount = null;
        while (accMatcher.find()) {
            lastAccount = accMatcher.group();
        }
        if (lastAccount != null) entities.put("accountNumber", lastAccount);

        return entities;
    }
}
