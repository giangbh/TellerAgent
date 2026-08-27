package com.dnse.teller.mock;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class MockBankingDataService {

    private final JdbcTemplate jdbcTemplate;

    private static final List<String> SURNAMES = List.of(
        "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng",
        "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý", "Đinh", "Đoàn", "Lâm", "Trịnh"
    );

    private static final List<String> GIVEN_NAMES = List.of(
        "Minh Anh", "Quốc Bảo", "Hoàng Long", "Thu Hà", "Đức Thắng", "Hải Đăng", "Quỳnh Nga",
        "Tuấn Kiệt", "Gia Huy", "Kim Ngân", "Bảo Trâm", "Khánh Linh", "Quang Huy", "Mai Phương",
        "Thanh Tùng", "Ngọc Linh", "Hồng Nhung", "Văn Hùng", "Thùy Dung", "Anh Dũng", "Thế Vinh",
        "Phương Thảo", "Trọng Nhân", "Hữu Phước", "Thanh Hằng"
    );

    private static final List<String> SEGMENTS = List.of("STANDARD", "GOLD", "PRIORITY", "DIAMOND_VIP");
    private static final List<String> RISK_RATINGS = List.of("LOW", "LOW", "LOW", "MEDIUM", "LOW");

    public MockBankingDataService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        seedDefaultCustomer();
    }

    public synchronized void seedDefaultCustomer() {
        String defaultCif = "CIF-0001842";
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mock_customers WHERE cif = ?", Integer.class, defaultCif);

        if (count == null || count == 0) {
            jdbcTemplate.update("""
                INSERT INTO mock_customers (cif, display_name, kyc_status, segment, risk_rating, identity_masked, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """, defaultCif, "Nguyễn Minh Anh", "VERIFIED", "PRIORITY", "LOW", "0123••••89", Instant.now().toString());

            // Add default accounts
            jdbcTemplate.update("""
                INSERT INTO mock_accounts (account_number, cif, account_ref, currency, available_balance, account_type, term_months, interest_rate, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, "3456789", defaultCif, "ACC-001", "VND", 250_000_000L, "CHECKING", 0, 0.0, "ACTIVE");

            jdbcTemplate.update("""
                INSERT INTO mock_accounts (account_number, cif, account_ref, currency, available_balance, account_type, term_months, interest_rate, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, "012345678901", defaultCif, "ACC-002", "VND", 42_500_000L, "CHECKING", 0, 0.0, "ACTIVE");

            jdbcTemplate.update("""
                INSERT INTO mock_accounts (account_number, cif, account_ref, currency, available_balance, account_type, term_months, interest_rate, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, "TD-888999", defaultCif, "ACC-TD-01", "VND", 1_500_000_000L, "TERM_DEPOSIT", 6, 5.8, "ACTIVE");
        }
    }

    public Map<String, Object> getCustomerProfile(String customerRef) {
        String cif = normalizeCif(customerRef);
        ensureCustomerExists(cif);

        String sql = "SELECT * FROM mock_customers WHERE cif = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("customerRef", rs.getString("cif"));
            profile.put("displayName", rs.getString("display_name"));
            profile.put("kycStatus", rs.getString("kyc_status"));
            profile.put("segment", rs.getString("segment"));
            profile.put("riskRating", rs.getString("risk_rating"));
            profile.put("identityMasked", rs.getString("identity_masked"));
            profile.put("summary", String.format("Khách hàng %s (CIF: %s) · Phân hạng: %s · KYC: %s · Xếp hạng rủi ro: %s.",
                rs.getString("display_name"), rs.getString("cif"), rs.getString("segment"), rs.getString("kyc_status"), rs.getString("risk_rating")));
            return profile;
        }, cif);
    }

    public Map<String, Object> getCustomerAccounts(String customerRef) {
        String cif = normalizeCif(customerRef);
        ensureCustomerExists(cif);

        String sql = "SELECT * FROM mock_accounts WHERE cif = ? AND account_type = 'CHECKING'";
        List<Map<String, Object>> accounts = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> acc = new LinkedHashMap<>();
            String accNum = rs.getString("account_number");
            acc.put("accountRef", rs.getString("account_ref"));
            acc.put("accountNumber", accNum);
            acc.put("accountNoMasked", "•••• " + (accNum.length() > 4 ? accNum.substring(accNum.length() - 4) : accNum));
            acc.put("currency", rs.getString("currency"));
            acc.put("availableBalance", rs.getLong("available_balance"));
            return acc;
        }, cif);

        StringBuilder summary = new StringBuilder("Khách hàng có " + accounts.size() + " tài khoản thanh toán khả dụng:");
        for (Map<String, Object> a : accounts) {
            summary.append(String.format(" %s (%s %s),", a.get("accountNoMasked"), formatMoney((Long) a.get("availableBalance")), a.get("currency")));
        }
        if (summary.charAt(summary.length() - 1) == ',') {
            summary.setCharAt(summary.length() - 1, '.');
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customerRef", cif);
        result.put("accounts", accounts);
        result.put("summary", summary.toString());
        return result;
    }

    public Map<String, Object> getCustomerAccountsSummary(String customerRef) {
        String cif = normalizeCif(customerRef);
        ensureCustomerExists(cif);
        Map<String, Object> profile = getCustomerProfile(cif);

        String sql = "SELECT * FROM mock_accounts WHERE cif = ?";
        List<Map<String, Object>> allAccs = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("accountNumber", rs.getString("account_number"));
            m.put("accountRef", rs.getString("account_ref"));
            m.put("currency", rs.getString("currency"));
            m.put("availableBalance", rs.getLong("available_balance"));
            m.put("accountType", rs.getString("account_type"));
            m.put("termMonths", rs.getInt("term_months"));
            m.put("interestRate", rs.getDouble("interest_rate"));
            return m;
        }, cif);

        long totalAvailable = 0;
        long totalTerm = 0;
        int checkingCount = 0;
        int termCount = 0;

        for (Map<String, Object> acc : allAccs) {
            String type = String.valueOf(acc.get("accountType"));
            long balance = ((Number) acc.get("availableBalance")).longValue();
            if ("CHECKING".equals(type)) {
                totalAvailable += balance;
                checkingCount++;
            } else if ("TERM_DEPOSIT".equals(type)) {
                totalTerm += balance;
                termCount++;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customerRef", cif);
        result.put("customerName", profile.get("displayName"));
        result.put("segment", profile.get("segment"));
        result.put("totalAvailableBalance", totalAvailable);
        result.put("totalTermDeposit", totalTerm);
        result.put("checkingAccountCount", checkingCount);
        result.put("termDepositCount", termCount);
        result.put("summary", String.format("Khách hàng %s (%s) sở hữu %d tài khoản thanh toán (tổng %s VND) và %d sổ tiết kiệm (%s VND).",
            profile.get("displayName"), profile.get("segment"), checkingCount, formatMoney(totalAvailable), termCount, formatMoney(totalTerm)));

        return result;
    }

    public Map<String, Object> getCustomerPersona(String customerRef) {
        String cif = normalizeCif(customerRef);
        ensureCustomerExists(cif);
        Map<String, Object> profile = getCustomerProfile(cif);

        Random rng = new Random(cif.hashCode() + 101);
        int age = 28 + Math.abs(rng.nextInt() % 35);
        int tenureMonths = 12 + Math.abs(rng.nextInt() % 72);
        int monthlyTxCount = 15 + Math.abs(rng.nextInt() % 45);

        List<String> personas = List.of(
            "Nhà đầu tư tích lũy & Tiết kiệm thông minh",
            "Doanh nhân / Chủ hộ kinh doanh vừa và nhỏ",
            "Chuyên viên công nghệ & Người tiêu dùng số",
            "Khách hàng cao cấp ưa chuộng tiện ích đặc quyền"
        );
        String persona = personas.get(Math.abs(rng.nextInt()) % personas.size());
        String channel = rng.nextBoolean() ? "Mobile Banking (85%)" : "Quầy giao dịch & Thẻ (60%)";
        String riskAppetite = "PRIORITY".equals(profile.get("segment")) || "DIAMOND_VIP".equals(profile.get("segment")) ? "TĂNG TRƯỞNG & ĐẦU TƯ" : "THẬN TRỌNG & BẢO TOÀN VỐN";

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("customerRef", cif);
        res.put("customerName", profile.get("displayName"));
        res.put("segment", profile.get("segment"));
        res.put("estimatedAge", age);
        res.put("relationshipTenure", tenureMonths + " tháng");
        res.put("monthlyTxFrequency", monthlyTxCount + " giao dịch/tháng");
        res.put("personaCategory", persona);
        res.put("preferredChannel", channel);
        res.put("investmentRiskAppetite", riskAppetite);
        res.put("summary", String.format("Chân dung KH %s (%s): Nhóm '%s', gắn bó %d tháng, kênh ưa chuộng: %s, khẩu vị: %s.",
            profile.get("displayName"), profile.get("segment"), persona, tenureMonths, channel, riskAppetite));

        return res;
    }

    public Map<String, Object> getCustomerCreditScore(String customerRef) {
        String cif = normalizeCif(customerRef);
        ensureCustomerExists(cif);
        Map<String, Object> profile = getCustomerProfile(cif);

        Random rng = new Random(cif.hashCode() + 202);
        int score = 680 + Math.abs(rng.nextInt() % 140); // 680 - 820
        String cicGroup = "Nhóm 1 (Dư nợ chuẩn, không có lịch sử nợ xấu)";
        long preApprovedLimit = "DIAMOND_VIP".equals(profile.get("segment")) ? 500_000_000L
            : ("PRIORITY".equals(profile.get("segment")) ? 200_000_000L : 50_000_000L);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("customerRef", cif);
        res.put("customerName", profile.get("displayName"));
        res.put("creditScore", score);
        res.put("creditRatingGrade", score >= 750 ? "AAA (Xuất sắc)" : "AA (Tốt)");
        res.put("cicStatus", cicGroup);
        res.put("preApprovedLoanLimit", preApprovedLimit);
        res.put("overdraftEligible", true);
        res.put("summary", String.format("Tín dụng KH %s: Điểm tín nhiệm %d/850 (%s), CIC: %s, Hạn mức vay phê duyệt trước: %s VND.",
            profile.get("displayName"), score, res.get("creditRatingGrade"), cicGroup, formatMoney(preApprovedLimit)));

        return res;
    }

    public Map<String, Object> getNextBestOffers(String customerRef) {
        String cif = normalizeCif(customerRef);
        ensureCustomerExists(cif);
        Map<String, Object> profile = getCustomerProfile(cif);

        List<Map<String, Object>> offers = new ArrayList<>();
        offers.add(Map.of(
            "productCode", "CARD-CASHBACK-SIGNATURE",
            "productName", "Thẻ Tín Dụng B.Smart Signature Cashback",
            "benefit", "Hoàn tiền 10% chi tiêu ẩm thực, phòng chờ thương gia sân bay không giới hạn",
            "priorityScore", 0.95
        ));
        offers.add(Map.of(
            "productCode", "SAVINGS-ACCUMULATIVE-6M",
            "productName", "Gói Tiền Gửi Đắc Lộc 6 Tháng Lãi Suất 6.2%/năm",
            "benefit", "Tặng 0.3%/năm lãi suất thưởng cho phân hạng " + profile.get("segment"),
            "priorityScore", 0.91
        ));
        offers.add(Map.of(
            "productCode", "OVERDRAFT-LINE-FAST",
            "productName", "Hạn Mức Thấu Chi Tài Khoản Trực Tuyến",
            "benefit", "Cấp sẵn hạn mức 100-300 triệu VND, miễn lãi 15 ngày đầu",
            "priorityScore", 0.88
        ));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("customerRef", cif);
        res.put("customerName", profile.get("displayName"));
        res.put("segment", profile.get("segment"));
        res.put("offers", offers);
        res.put("summary", String.format("Top 3 Offer tư vấn cho KH %s (%s): 1) Thẻ Signature Cashback 10%% · 2) Tiết kiệm Đắc Lộc 6.2%% · 3) Thấu chi miễn lãi 15 ngày.",
            profile.get("displayName"), profile.get("segment")));

        return res;
    }

    public Map<String, Object> adviseSavingsPlan(Long amount, Integer termMonths) {
        long baseAmount = amount != null && amount > 0 ? amount : 100_000_000L;
        int term = termMonths != null && termMonths > 0 ? termMonths : 6;

        Map<Integer, Double> rateTable = Map.of(
            1, 3.2,
            3, 4.2,
            6, 5.6,
            12, 6.0,
            24, 6.3
        );

        double rate = rateTable.getOrDefault(term, 5.6);
        long interestEarned = Math.round(baseAmount * (rate / 100.0) * (term / 12.0));
        long totalMaturity = baseAmount + interestEarned;

        List<Map<String, Object>> comparison = new ArrayList<>();
        for (Map.Entry<Integer, Double> e : rateTable.entrySet()) {
            int t = e.getKey();
            double r = e.getValue();
            long earned = Math.round(baseAmount * (r / 100.0) * (t / 12.0));
            comparison.add(Map.of("termMonths", t, "rate", r, "interestEarned", earned, "totalAtMaturity", baseAmount + earned));
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("depositAmount", baseAmount);
        res.put("selectedTermMonths", term);
        res.put("interestRate", rate);
        res.put("estimatedInterestEarned", interestEarned);
        res.put("totalAtMaturity", totalMaturity);
        res.put("allTermsComparison", comparison);
        res.put("summary", String.format("Gửi %s VND kỳ hạn %d tháng (Lãi suất %.1f%%/năm) ➔ Tiền lãi dự kiến: %s VND (Tổng nhận khi đáo hạn: %s VND).",
            formatMoney(baseAmount), term, rate, formatMoney(interestEarned), formatMoney(totalMaturity)));

        return res;
    }

    public Map<String, Object> getCardServices(String customerRef) {
        String cif = normalizeCif(customerRef);
        ensureCustomerExists(cif);
        Map<String, Object> profile = getCustomerProfile(cif);

        List<Map<String, Object>> cards = new ArrayList<>();
        cards.add(Map.of(
            "cardRef", "CRD-VISA-01",
            "cardType", "CREDIT_VISA_PLATINUM",
            "cardNumberMasked", "4000 •••• •••• 8866",
            "status", "ACTIVE",
            "creditLimit", 100_000_000L,
            "availableLimit", 82_400_000L,
            "ecomEnabled", true,
            "contactlessEnabled", true
        ));
        cards.add(Map.of(
            "cardRef", "CRD-NAPAS-02",
            "cardType", "DEBIT_NAPAS_DOMESTIC",
            "cardNumberMasked", "9704 •••• •••• 3489",
            "status", "ACTIVE",
            "dailyAtmWithdrawalLimit", 50_000_000L,
            "ecomEnabled", true,
            "contactlessEnabled", true
        ));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("customerRef", cif);
        res.put("customerName", profile.get("displayName"));
        res.put("cards", cards);
        res.put("summary", String.format("Khách hàng %s có 2 thẻ hoạt động: 1 Thẻ tín dụng Visa Platinum (hạn mức 100 tr, khả dụng 82,4 tr) và 1 Thẻ ghi nợ Napas.",
            profile.get("displayName")));

        return res;
    }

    public Map<String, Object> getTransactionHistory(String accountNumber, String customerRef) {
        String num = accountNumber != null && !accountNumber.trim().isEmpty() ? accountNumber.trim() : "3456789";
        String cif = normalizeCif(customerRef);

        List<Map<String, Object>> txs = new ArrayList<>();
        LocalDate today = LocalDate.now();

        txs.add(Map.of("txId", "TX-9901", "date", today.minusDays(1).toString(), "description", "Nhận tiền chuyển khoản từ CTY TNHH ABC (Lương T8)", "amount", 45_000_000L, "type", "INFLOW", "balanceAfter", 250_000_000L));
        txs.add(Map.of("txId", "TX-9902", "date", today.minusDays(3).toString(), "description", "Thanh toán QR POS siêu thị WinMart+", "amount", -1_850_000L, "type", "OUTFLOW", "balanceAfter", 205_000_000L));
        txs.add(Map.of("txId", "TX-9903", "date", today.minusDays(5).toString(), "description", "Chuyển tiền nhanh 24/7 đến Tran Thi Lan", "amount", -5_000_000L, "type", "OUTFLOW", "balanceAfter", 206_850_000L));
        txs.add(Map.of("txId", "TX-9904", "date", today.minusDays(8).toString(), "description", "Tiền lãi tiết kiệm định kỳ", "amount", 7_250_000L, "type", "INFLOW", "balanceAfter", 211_850_000L));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("accountNumber", num);
        res.put("customerRef", cif);
        res.put("transactions", txs);
        res.put("summary", String.format("Sao kê gần nhất tài khoản %s: 4 giao dịch (Tổng tiền vào: +52,25 tr VND, Tổng chi tiêu: -6,85 tr VND, Số dư hiện tại: 250 tr VND).", num));

        return res;
    }

    public Map<String, Object> resolveAccount(String accountNumber) {
        String num = accountNumber != null ? accountNumber.trim() : "3456789";

        String sql = "SELECT a.*, c.display_name, c.segment FROM mock_accounts a JOIN mock_customers c ON a.cif = c.cif WHERE a.account_number = ?";
        List<Map<String, Object>> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("accountRef", rs.getString("account_ref"));
            res.put("accountNumber", rs.getString("account_number"));
            res.put("accountNoMasked", "•••• " + (num.length() > 4 ? num.substring(num.length() - 4) : num));
            res.put("accountHolder", rs.getString("display_name"));
            res.put("currency", rs.getString("currency"));
            res.put("status", rs.getString("status"));
            res.put("availableBalance", rs.getLong("available_balance"));
            res.put("segment", rs.getString("segment"));
            res.put("summary", String.format("Tài khoản %s: Chủ tài khoản %s (Trạng thái: %s, Số dư khả dụng: %s VND).",
                num, rs.getString("display_name"), rs.getString("status"), formatCurrency(rs.getLong("available_balance"))));
            return res;
        }, num);

        if (!list.isEmpty()) {
            return list.get(0);
        }

        // Auto-generate synthetic account & customer for this number
        String syntheticCif = "CIF-" + Math.abs(num.hashCode() % 100000);
        ensureCustomerExists(syntheticCif);

        long balance = 50_000_000L + (Math.abs(num.hashCode()) % 400_000_000L);
        jdbcTemplate.update("""
            INSERT INTO mock_accounts (account_number, cif, account_ref, currency, available_balance, account_type, term_months, interest_rate, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, num, syntheticCif, "ACC-GEN-" + num, "VND", balance, "CHECKING", 0, 0.0, "ACTIVE");

        return resolveAccount(num);
    }

    private synchronized void ensureCustomerExists(String cif) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mock_customers WHERE cif = ?", Integer.class, cif);
        if (count != null && count > 0) {
            return;
        }

        Random rng = new Random(cif.hashCode());
        String surname = SURNAMES.get(Math.abs(rng.nextInt()) % SURNAMES.size());
        String given = GIVEN_NAMES.get(Math.abs(rng.nextInt()) % GIVEN_NAMES.size());
        String fullName = surname + " " + given;

        String segment = SEGMENTS.get(Math.abs(rng.nextInt()) % SEGMENTS.size());
        String risk = RISK_RATINGS.get(Math.abs(rng.nextInt()) % RISK_RATINGS.size());
        String cccd = String.format("0%02d••••%02d", rng.nextInt(90) + 10, rng.nextInt(90) + 10);

        jdbcTemplate.update("""
            INSERT INTO mock_customers (cif, display_name, kyc_status, segment, risk_rating, identity_masked, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """, cif, fullName, "VERIFIED", segment, risk, cccd, Instant.now().toString());

        // Generate 1-3 accounts
        int accCount = 1 + (Math.abs(rng.nextInt()) % 3);
        for (int i = 1; i <= accCount; i++) {
            String accNum = String.format("%04d%04d%02d", Math.abs(rng.nextInt()) % 9000 + 1000, Math.abs(rng.nextInt()) % 9000 + 1000, i);
            long balance = (15 + (Math.abs(rng.nextInt()) % 350)) * 1_000_000L;
            jdbcTemplate.update("""
                INSERT INTO mock_accounts (account_number, cif, account_ref, currency, available_balance, account_type, term_months, interest_rate, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, accNum, cif, "ACC-" + i, "VND", balance, "CHECKING", 0, 0.0, "ACTIVE");
        }

        // 70% chance of term deposit
        if (rng.nextBoolean()) {
            long tdAmount = (100 + (Math.abs(rng.nextInt()) % 1500)) * 1_000_000L;
            int term = (rng.nextBoolean() ? 6 : 12);
            double rate = term == 6 ? 5.6 : 6.2;
            jdbcTemplate.update("""
                INSERT INTO mock_accounts (account_number, cif, account_ref, currency, available_balance, account_type, term_months, interest_rate, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, "TD-" + Math.abs(rng.nextInt() % 900000 + 100000), cif, "ACC-TD", "VND", tdAmount, "TERM_DEPOSIT", term, rate, "ACTIVE");
        }
    }

    private String normalizeCif(String ref) {
        if (ref == null || ref.trim().isEmpty()) return "CIF-0001842";
        String clean = ref.trim().toUpperCase();
        if (!clean.startsWith("CIF-") && clean.matches("\\d+")) {
            return "CIF-" + clean;
        }
        return clean;
    }

    private String formatMoney(long amount) {
        if (amount >= 1_000_000_000L) {
            double ty = (double) amount / 1_000_000_000.0;
            return (ty == (long) ty ? String.format("%d", (long) ty) : String.format("%.1f", ty)).replace(".", ",") + " tỷ";
        }
        if (amount >= 1_000_000L) {
            double tr = (double) amount / 1_000_000.0;
            return (tr == (long) tr ? String.format("%d", (long) tr) : String.format("%.1f", tr)).replace(".", ",") + " tr";
        }
        return new DecimalFormat("#,###").format(amount);
    }

    private String formatCurrency(long amount) {
        return new DecimalFormat("#,###").format(amount).replace(",", ".");
    }
}
