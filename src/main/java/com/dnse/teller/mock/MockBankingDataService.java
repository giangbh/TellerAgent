package com.dnse.teller.mock;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.Instant;
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
        // Ensure default test customer CIF-0001842 and account 3456789 exist
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

        // If account not found in DB, auto-generate synthetic account & customer for this number
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

        // Generate synthetic customer seeded by CIF string
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
