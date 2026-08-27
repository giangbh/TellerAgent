package com.dnse.teller.mock;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Quản lý tài khoản thanh toán, tiền gửi, tổng hợp danh mục tài sản và đối soát số tài khoản.
 */
@Service
public class MockAccountService {

    private final JdbcTemplate jdbcTemplate;
    private final MockCustomerProfileService customerProfileService;

    public MockAccountService(JdbcTemplate jdbcTemplate, MockCustomerProfileService customerProfileService) {
        this.jdbcTemplate = jdbcTemplate;
        this.customerProfileService = customerProfileService;
    }

    private String formatMoney(long amount) {
        return new DecimalFormat("#,###").format(amount);
    }

    public Map<String, Object> getCustomerAccounts(String customerRef) {
        String cif = customerProfileService.normalizeCif(customerRef);
        customerProfileService.ensureCustomerExists(cif);

        String sql = "SELECT * FROM mock_accounts WHERE cif = ? AND account_type = 'CHECKING'";
        List<Map<String, Object>> accounts = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> acc = new LinkedHashMap<>();
            String accNum = rs.getString("account_number");
            acc.put("accountRef", rs.getString("account_ref"));
            acc.put("accountNumber", accNum);
            acc.put("accountNoMasked", "•••• " + (accNum.length() > 4 ? accNum.substring(accNum.length() - 4) : accNum));
            acc.put("currency", rs.getString("currency"));
            acc.put("availableBalance", rs.getLong("available_balance"));
            acc.put("status", rs.getString("status"));
            return acc;
        }, cif);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("customerRef", cif);
        res.put("accounts", accounts);
        res.put("primaryAccountRef", accounts.isEmpty() ? null : accounts.get(0).get("accountRef"));
        res.put("primaryAccountNumber", accounts.isEmpty() ? null : accounts.get(0).get("accountNumber"));
        res.put("primaryBalance", accounts.isEmpty() ? 0L : accounts.get(0).get("availableBalance"));
        res.put("summary", String.format("Khách hàng có %d tài khoản thanh toán hoạt động. Tài khoản chính: %s (Số dư: %s VND).",
            accounts.size(),
            res.get("primaryAccountNumber"),
            formatMoney((Long) res.get("primaryBalance"))
        ));

        return res;
    }

    public Map<String, Object> getCustomerAccountsSummary(String customerRef) {
        String cif = customerProfileService.normalizeCif(customerRef);
        customerProfileService.ensureCustomerExists(cif);
        Map<String, Object> profile = customerProfileService.getCustomerProfile(cif);

        String sql = "SELECT * FROM mock_accounts WHERE cif = ?";
        List<Map<String, Object>> checkingAccounts = new ArrayList<>();
        List<Map<String, Object>> termDeposits = new ArrayList<>();
        final long[] totalChecking = {0};
        final long[] totalTermDeposit = {0};

        jdbcTemplate.query(sql, (rs, rowNum) -> {
            String type = rs.getString("account_type");
            long bal = rs.getLong("available_balance");
            String accNum = rs.getString("account_number");

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("accountRef", rs.getString("account_ref"));
            item.put("accountNumber", accNum);
            item.put("accountNoMasked", "•••• " + (accNum.length() > 4 ? accNum.substring(accNum.length() - 4) : accNum));
            item.put("currency", rs.getString("currency"));
            item.put("availableBalance", bal);
            item.put("status", rs.getString("status"));

            if ("CHECKING".equals(type)) {
                totalChecking[0] += bal;
                checkingAccounts.add(item);
            } else {
                totalTermDeposit[0] += bal;
                item.put("termMonths", rs.getInt("term_months"));
                item.put("interestRate", rs.getDouble("interest_rate"));
                termDeposits.add(item);
            }
            return null;
        }, cif);

        long totalAum = totalChecking[0] + totalTermDeposit[0];

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("customerRef", cif);
        res.put("displayName", profile.get("displayName"));
        res.put("segment", profile.get("segment"));
        res.put("totalAum", totalAum);
        res.put("totalCheckingBalance", totalChecking[0]);
        res.put("totalTermDepositBalance", totalTermDeposit[0]);
        res.put("checkingAccounts", checkingAccounts);
        res.put("termDeposits", termDeposits);
        res.put("summary", String.format("Tổng tài sản (AUM) của KH %s (%s) là %s VND (Gồm %d tài khoản thanh toán: %s VND và %d sổ tiết kiệm: %s VND).",
            profile.get("displayName"), profile.get("segment"), formatMoney(totalAum),
            checkingAccounts.size(), formatMoney(totalChecking[0]),
            termDeposits.size(), formatMoney(totalTermDeposit[0])
        ));

        return res;
    }

    public Map<String, Object> resolveAccount(String accountNumber) {
        String num = accountNumber != null ? accountNumber.trim() : "3456789";

        String sql = "SELECT a.*, c.display_name, c.segment FROM mock_accounts a JOIN mock_customers c ON a.cif = c.cif WHERE a.account_number = ?";
        List<Map<String, Object>> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("accountRef", rs.getString("account_ref"));
            res.put("accountNumber", rs.getString("account_number"));
            res.put("accountHolder", rs.getString("display_name"));
            res.put("segment", rs.getString("segment"));
            res.put("cif", rs.getString("cif"));
            res.put("currency", rs.getString("currency"));
            res.put("availableBalance", rs.getLong("available_balance"));
            res.put("status", rs.getString("status"));
            res.put("accountType", rs.getString("account_type"));
            return res;
        }, num);

        if (!list.isEmpty()) {
            Map<String, Object> acc = list.get(0);
            acc.put("summary", String.format("Tài khoản %s: Chủ tài khoản %s (Trạng thái: %s, Số dư khả dụng: %s VND).",
                acc.get("accountNumber"), acc.get("accountHolder"), acc.get("status"), formatMoney((Long) acc.get("availableBalance"))));
            return acc;
        }

        // Tự động sinh dữ liệu mới nếu chưa tồn tại
        Random rng = new Random(num.hashCode());
        String cif = "CIF-" + String.format("%07d", Math.abs(rng.nextLong()) % 9000000L + 1000000L);
        customerProfileService.ensureCustomerExists(cif);

        long bal = (rng.nextInt(300) + 5) * 1_000_000L;
        jdbcTemplate.update("""
            INSERT INTO mock_accounts (account_number, cif, account_ref, currency, available_balance, account_type, term_months, interest_rate, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, num, cif, "ACC-" + (num.length() > 4 ? num.substring(num.length() - 4) : num), "VND", bal, "CHECKING", 0, 0.0, "ACTIVE");

        Map<String, Object> profile = customerProfileService.getCustomerProfile(cif);
        Map<String, Object> acc = new LinkedHashMap<>();
        acc.put("accountRef", "ACC-" + (num.length() > 4 ? num.substring(num.length() - 4) : num));
        acc.put("accountNumber", num);
        acc.put("accountHolder", profile.get("displayName"));
        acc.put("segment", profile.get("segment"));
        acc.put("cif", cif);
        acc.put("currency", "VND");
        acc.put("availableBalance", bal);
        acc.put("status", "ACTIVE");
        acc.put("accountType", "CHECKING");
        acc.put("summary", String.format("Tài khoản %s: Chủ tài khoản %s (Trạng thái: ACTIVE, Số dư khả dụng: %s VND).",
            num, profile.get("displayName"), formatMoney(bal)));

        return acc;
    }
}
