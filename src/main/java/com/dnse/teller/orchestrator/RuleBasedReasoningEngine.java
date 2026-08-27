package com.dnse.teller.orchestrator;

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
public class RuleBasedReasoningEngine {

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

    // Advanced Banking Patterns
    private static final Pattern PERSONA_PAT = Pattern.compile("chân dung|phân tích khách hàng|hành vi|khẩu vị đầu tư|gắn bó|thói quen", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CREDIT_SCORE_PAT = Pattern.compile("điểm tín dụng|cic|nợ xấu|vay được bao nhiêu|hạn mức vay|chấm điểm tín nhiệm", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern NBO_PAT = Pattern.compile("tư vấn|gợi ý|ưu đãi|sản phẩm phù hợp|bán chéo|\\boffer\\b|khuyến nghị", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern SAVINGS_ADVISE_PAT = Pattern.compile("tính lãi|lãi bao nhiêu|kỳ hạn tốt nhất|tối ưu lãi|phương án tiết kiệm", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CARD_MANAGE_PAT = Pattern.compile("thẻ tín dụng|thẻ ghi nợ|khóa thẻ|mở thẻ|đổi pin|hạn mức thẻ|\\bvisa\\b|\\bmastercard\\b|\\bnapas\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern HISTORY_PAT = Pattern.compile("sao kê|lịch sử giao dịch|tiền vào tiền ra|trích lục|biến động số dư", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern ACCOUNT_PAT = Pattern.compile("\\b\\d{6,14}\\b");
    private static final Pattern NAME_PAT = Pattern.compile("(?:cho|tên|người nhận)\\s+([A-Za-zÀ-ỹĐđ\\s]{3,40}?)(?=\\s+(?:tại|ở|ngân hàng|số tài khoản|stk|tk)|[,.;]|$)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CIF_PAT = Pattern.compile("(?:cif|khách hàng|kh)[-:\\s]*([A-Za-z0-9_-]{4,15})", Pattern.CASE_INSENSITIVE);
    private static final Pattern TERM_PAT = Pattern.compile("(\\d{1,2})\\s*(?:tháng|thg|m)\\b", Pattern.CASE_INSENSITIVE);

    public RuleBasedReasoningEngine(McpToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public Intent detectIntent(String text, Intent currentIntent) {
        String lower = text != null ? text.toLowerCase() : "";
        String extractedCif = parseCif(text);
        Long extractedAmount = parseAmount(text);
        Integer extractedTerm = parseTerm(text);

        // 1. Dynamic Next-Best-Offer / Product Recommendation
        if (NBO_PAT.matcher(lower).find()) {
            Map<String, Object> entities = new LinkedHashMap<>();
            if (extractedCif != null) entities.put("customerRef", extractedCif);
            Intent nboIntent = new Intent("DYNAMIC_NBO_LOOKUP", "dynamic_autonomous", 0.98, entities);
            nboIntent.setQuery(text);
            return nboIntent;
        }

        // 2. Dynamic Savings Yield & Product Advisor
        if (SAVINGS_ADVISE_PAT.matcher(lower).find()) {
            Map<String, Object> entities = new LinkedHashMap<>();
            if (extractedAmount != null) entities.put("amount", extractedAmount);
            if (extractedTerm != null) entities.put("termMonths", extractedTerm);
            Intent savingsIntent = new Intent("DYNAMIC_SAVINGS_ADVISE", "dynamic_autonomous", 0.98, entities);
            savingsIntent.setQuery(text);
            return savingsIntent;
        }

        // 3. Dynamic Customer Persona 360 Analytics
        if (PERSONA_PAT.matcher(lower).find()) {
            Map<String, Object> entities = new LinkedHashMap<>();
            if (extractedCif != null) entities.put("customerRef", extractedCif);
            Intent personaIntent = new Intent("DYNAMIC_PERSONA_LOOKUP", "dynamic_autonomous", 0.98, entities);
            personaIntent.setQuery(text);
            return personaIntent;
        }

        // 4. Dynamic Credit Score & CIC Inquiry
        if (CREDIT_SCORE_PAT.matcher(lower).find()) {
            Map<String, Object> entities = new LinkedHashMap<>();
            if (extractedCif != null) entities.put("customerRef", extractedCif);
            Intent creditIntent = new Intent("DYNAMIC_CREDIT_SCORE_LOOKUP", "dynamic_autonomous", 0.98, entities);
            creditIntent.setQuery(text);
            return creditIntent;
        }

        // 5. Dynamic Card Services & Operations
        if (CARD_MANAGE_PAT.matcher(lower).find()) {
            Map<String, Object> entities = new LinkedHashMap<>();
            if (extractedCif != null) entities.put("customerRef", extractedCif);
            Intent cardIntent = new Intent("DYNAMIC_CARD_MANAGE", "dynamic_autonomous", 0.98, entities);
            cardIntent.setQuery(text);
            return cardIntent;
        }

        // 6. Dynamic Statement & Transaction History
        if (HISTORY_PAT.matcher(lower).find()) {
            Map<String, Object> entities = new LinkedHashMap<>();
            String account = parseAccount(text);
            if (account != null) entities.put("accountNumber", account);
            if (extractedCif != null) entities.put("customerRef", extractedCif);
            Intent historyIntent = new Intent("DYNAMIC_TRANSACTION_HISTORY", "dynamic_autonomous", 0.98, entities);
            historyIntent.setQuery(text);
            return historyIntent;
        }

        // 7. Dynamic FX Rate / Currency Inquiry
        if (FX_PAT.matcher(lower).find()) {
            Map<String, Object> entities = new LinkedHashMap<>();
            if (extractedAmount != null) entities.put("amount", extractedAmount);

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

        // 8. Dynamic Branch / Location Inquiry
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

        // 9. Dynamic Customer Portfolio & Accounts Summary Inquiry
        if (SUMMARY_PAT.matcher(lower).find()) {
            Map<String, Object> entities = new LinkedHashMap<>();
            if (extractedCif != null) entities.put("customerRef", extractedCif);
            Intent summaryIntent = new Intent("DYNAMIC_ACCOUNT_SUMMARY", "dynamic_autonomous", 0.98, entities);
            summaryIntent.setQuery(text);
            return summaryIntent;
        }

        // 10. Policy Assistance
        if (POLICY_QUERY_PAT.matcher(lower).find()) {
            Intent policyIntent = new Intent("POLICY_ASSISTANCE", "policy_assistance", 0.95, Map.of());
            policyIntent.setQuery(text);
            return policyIntent;
        }

        // 11. Cash Deposit & Withdrawal
        if (CASH_DEPOSIT_PAT.matcher(lower).find()) {
            return new Intent("CASH_DEPOSIT", "cash_deposit", 0.97, extractCashEntities(text, currentIntent != null ? currentIntent.getEntities() : Map.of()));
        }
        if (CASH_WITHDRAWAL_PAT.matcher(lower).find()) {
            return new Intent("CASH_WITHDRAWAL", "cash_withdrawal", 0.97, extractCashEntities(text, currentIntent != null ? currentIntent.getEntities() : Map.of()));
        }

        // 12. Domestic Transfer
        if (TRANSFER_PAT.matcher(lower).find()) {
            return new Intent("DOMESTIC_TRANSFER", "domestic_transfer", 0.94, extractTransferEntities(text, currentIntent != null ? currentIntent.getEntities() : Map.of()));
        }

        // 13. Resume previous state if present
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

        // 14. Default Dynamic Tool Discovery for any unclassified prompt
        List<String> discoveredTools = discoverTools(text);
        Map<String, Object> entities = extractTransferEntities(text, Map.of());
        if (extractedCif != null) entities.put("customerRef", extractedCif);
        Intent dynamicIntent = new Intent("DYNAMIC_AUTONOMOUS_TASK", "dynamic_autonomous", 0.88, entities);
        dynamicIntent.setQuery(text);
        dynamicIntent.getEntities().put("discoveredTools", discoveredTools);
        return dynamicIntent;
    }

    public Plan proposePlan(Session session) {
        String intentType = session.getIntent() != null ? session.getIntent().getType() : "DYNAMIC_AUTONOMOUS_TASK";

        if ("DYNAMIC_NBO_LOOKUP".equals(intentType)) {
            return new Plan(
                "Khám phá công cụ động: Tư vấn gợi ý sản phẩm ưu đãi cá nhân hóa (NBO)",
                List.of(new PlanStep("S1", "DELEGATE", "dynamic_tool_agent:recommendation.nbo.products", List.of())),
                List.of("Có danh sách gợi ý sản phẩm", "Có quyền lợi chi tiết")
            );
        }

        if ("DYNAMIC_SAVINGS_ADVISE".equals(intentType)) {
            return new Plan(
                "Khám phá công cụ động: Mô phỏng tối ưu lãi suất tiền gửi tiết kiệm",
                List.of(new PlanStep("S1", "DELEGATE", "dynamic_tool_agent:savings.product.advisor", List.of())),
                List.of("Có bảng tính lãi suất", "Có dòng tiền kỳ vọng khi đáo hạn")
            );
        }

        if ("DYNAMIC_PERSONA_LOOKUP".equals(intentType)) {
            return new Plan(
                "Khám phá công cụ động: Phân tích chân dung 360 độ và hành vi khách hàng",
                List.of(new PlanStep("S1", "DELEGATE", "dynamic_tool_agent:customer.persona.analytics", List.of())),
                List.of("Có nhóm chân dung", "Có kênh ưa chuộng & khẩu vị rủi ro")
            );
        }

        if ("DYNAMIC_CREDIT_SCORE_LOOKUP".equals(intentType)) {
            return new Plan(
                "Khám phá công cụ động: Tra cứu điểm tín dụng nội bộ, CIC và hạn mức vay",
                List.of(new PlanStep("S1", "DELEGATE", "dynamic_tool_agent:customer.credit.score.check", List.of())),
                List.of("Có điểm tín nhiệm CIC", "Có hạn mức phê duyệt trước")
            );
        }

        if ("DYNAMIC_CARD_MANAGE".equals(intentType)) {
            return new Plan(
                "Khám phá công cụ động: Tra cứu trạng thái thẻ và hạn mức thanh toán",
                List.of(new PlanStep("S1", "DELEGATE", "dynamic_tool_agent:card.service.manage", List.of())),
                List.of("Có danh sách thẻ", "Có trạng thái & hạn mức")
            );
        }

        if ("DYNAMIC_TRANSACTION_HISTORY".equals(intentType)) {
            return new Plan(
                "Khám phá công cụ động: Trích lục lịch sử giao dịch sao kê và dòng tiền",
                List.of(new PlanStep("S1", "DELEGATE", "dynamic_tool_agent:statement.transaction.history", List.of())),
                List.of("Có lịch sử biến động số dư", "Có phân loại thu-chi")
            );
        }

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

        if (lower.contains("tư vấn") || lower.contains("gợi ý") || lower.contains("offer")) selected.add("recommendation.nbo.products");
        if (lower.contains("chân dung") || lower.contains("hành vi")) selected.add("customer.persona.analytics");
        if (lower.contains("tín dụng") || lower.contains("cic") || lower.contains("vay")) selected.add("customer.credit.score.check");
        if (lower.contains("lãi") || lower.contains("tiết kiệm")) selected.add("savings.product.advisor");
        if (lower.contains("thẻ") || lower.contains("card")) selected.add("card.service.manage");
        if (lower.contains("sao kê") || lower.contains("lịch sử")) selected.add("statement.transaction.history");
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

    public String parseCif(String text) {
        if (text == null) return null;
        Matcher m = CIF_PAT.matcher(text);
        if (m.find()) {
            String raw = m.group(1).trim();
            if (raw.matches("(?i)cif-.*")) return raw.toUpperCase();
            return "CIF-" + raw.toUpperCase();
        }
        return null;
    }

    public Integer parseTerm(String text) {
        if (text == null) return null;
        Matcher m = TERM_PAT.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return null;
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

        Pattern amountFormatted = Pattern.compile("\\b(\\d{1,3}(?:\\.\\d{3})+)\\b");
        Matcher m2 = amountFormatted.matcher(normalized);
        if (m2.find()) {
            String raw = m2.group(1).replace(".", "");
            try {
                return Long.parseLong(raw);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    public String parseBank(String text) {
        if (text == null) return null;
        String lower = text.toLowerCase();
        for (String[] alias : BANK_ALIASES) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(alias[0]) + "\\b", Pattern.CASE_INSENSITIVE);
            if (p.matcher(lower).find()) {
                return alias[1];
            }
        }
        return null;
    }

    public String parseAccount(String text) {
        if (text == null) return null;
        Matcher m = ACCOUNT_PAT.matcher(text);
        if (m.find()) {
            return m.group(0);
        }
        return null;
    }

    public String parseName(String text) {
        if (text == null) return null;
        Matcher m = NAME_PAT.matcher(text);
        if (m.find()) {
            String name = m.group(1).trim();
            if (!name.equalsIgnoreCase("vietcombank") && !name.equalsIgnoreCase("vcb") && !name.equalsIgnoreCase("bidv")) {
                return name;
            }
        }
        return null;
    }

    public Map<String, Object> extractTransferEntities(String text, Map<String, Object> existing) {
        Map<String, Object> entities = new LinkedHashMap<>(existing != null ? existing : Map.of());

        Long amount = parseAmount(text);
        if (amount != null) entities.put("amount", amount);

        String bank = parseBank(text);
        if (bank != null) entities.put("bankCode", bank);

        String account = parseAccount(text);
        if (account != null) entities.put("beneficiaryAccount", account);

        String name = parseName(text);
        if (name != null) entities.put("beneficiaryName", name);

        String cif = parseCif(text);
        if (cif != null) entities.put("customerRef", cif);

        return entities;
    }

    public Map<String, Object> extractCashEntities(String text, Map<String, Object> existing) {
        Map<String, Object> entities = new LinkedHashMap<>(existing != null ? existing : Map.of());

        Long amount = parseAmount(text);
        if (amount != null) entities.put("amount", amount);

        String account = parseAccount(text);
        if (account != null) entities.put("accountNumber", account);

        String name = parseName(text);
        if (name != null) entities.put("accountHolder", name);

        String cif = parseCif(text);
        if (cif != null) entities.put("customerRef", cif);

        return entities;
    }
}
