package com.dnse.teller.mock;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Quản lý thông tin định danh, KYC, phân khúc và chân dung cơ bản của Khách hàng.
 */
@Service
public class MockCustomerProfileService {

    private final JdbcTemplate jdbcTemplate;

    private static final List<String> SURNAMES = List.of(
        "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng",
        "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý", "Đinh", "Đoàn", "Lâm", "Trịnh"
    );

    private static final List<String> GIVEN_NAMES = List.of(
        "Minh Anh", "Quốc Bảo", "Hoàng Long", "Thu Hà", "Đức Thắng", "Hải Đăng", "Quỳnh Nga",
        "Tuấn Kiệt", "Gia Huy", "Kim Ngân", "Bảo Trâm", "Khánh Linh", "Quang Huy", "Mai Phương",
        "Thanh Tùng", "Ngọc Linh", "Hồng Nhung", "Văn Hùng", "Thùy Dung", "Anh Dũng", "Thế Vinh",
        "Phương Thảo", "Trọng Nhân", "Hữu Phước", "Thanh Hằng", "Tuấn Anh", "Yến Nhi", "Bảo Ngọc"
    );

    private static final List<String> OCCUPATIONS = List.of(
        "Kinh doanh tự do / Chủ hộ", "Kỹ sư CNTT / Chuyên gia", "Giám đốc điều hành Doanh nghiệp",
        "Bác sĩ / Y tế", "Giảng viên / Giáo dục", "Kiến trúc sư", "Nhà đầu tư tài chính", "Quản lý cấp cao"
    );

    private static final List<String> SEGMENTS = List.of("STANDARD", "GOLD", "PRIORITY", "DIAMOND_VIP");
    private static final List<String> RISK_RATINGS = List.of("LOW", "LOW", "LOW", "MEDIUM", "LOW");

    public MockCustomerProfileService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String normalizeCif(String customerRef) {
        if (customerRef == null || customerRef.trim().isEmpty()) return "CIF-0001842";
        String clean = customerRef.trim().toUpperCase();
        if (!clean.startsWith("CIF-")) {
            if (clean.matches("\\d+")) {
                clean = "CIF-" + String.format("%07d", Long.parseLong(clean));
            } else {
                clean = "CIF-" + clean;
            }
        }
        return clean;
    }

    public synchronized void ensureCustomerExists(String cif) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mock_customers WHERE cif = ?", Integer.class, cif);
        if (count == null || count == 0) {
            Random rng = new Random(cif.hashCode());
            String surname = SURNAMES.get(Math.abs(rng.nextInt()) % SURNAMES.size());
            String givenName = GIVEN_NAMES.get(Math.abs(rng.nextInt()) % GIVEN_NAMES.size());
            String fullName = surname + " " + givenName;

            String segment = SEGMENTS.get(Math.abs(rng.nextInt()) % SEGMENTS.size());
            String risk = RISK_RATINGS.get(Math.abs(rng.nextInt()) % RISK_RATINGS.size());
            String idMasked = String.format("%03d••••%02d", rng.nextInt(900) + 100, rng.nextInt(90) + 10);

            jdbcTemplate.update("""
                INSERT INTO mock_customers (cif, display_name, kyc_status, segment, risk_rating, identity_masked, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """, cif, fullName, "VERIFIED", segment, risk, idMasked, Instant.now().toString());

            // Tự động tạo 1-3 tài khoản mặc định
            String acc1 = String.format("%010d", Math.abs(rng.nextLong()) % 9000000000L + 1000000000L);
            long bal1 = (rng.nextInt(400) + 10) * 1_000_000L;
            jdbcTemplate.update("""
                INSERT INTO mock_accounts (account_number, cif, account_ref, currency, available_balance, account_type, term_months, interest_rate, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, acc1, cif, "ACC-" + acc1.substring(0, 4), "VND", bal1, "CHECKING", 0, 0.0, "ACTIVE");

            if (rng.nextBoolean()) {
                String accTd = "TD-" + (rng.nextInt(900000) + 100000);
                long balTd = (rng.nextInt(20) + 1) * 100_000_000L;
                int term = List.of(3, 6, 12, 24).get(rng.nextInt(4));
                double rate = 5.0 + (term * 0.1);
                jdbcTemplate.update("""
                    INSERT INTO mock_accounts (account_number, cif, account_ref, currency, available_balance, account_type, term_months, interest_rate, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, accTd, cif, "ACC-TD-" + accTd.substring(3), "VND", balTd, "TERM_DEPOSIT", term, rate, "ACTIVE");
            }
        }
    }

    public Map<String, Object> getCustomerProfile(String customerRef) {
        String cif = normalizeCif(customerRef);
        ensureCustomerExists(cif);

        String sql = "SELECT * FROM mock_customers WHERE cif = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            Map<String, Object> profile = new LinkedHashMap<>();
            Random rng = new Random(cif.hashCode() + 11);
            String occupation = OCCUPATIONS.get(Math.abs(rng.nextInt()) % OCCUPATIONS.size());
            int birthYear = 1970 + rng.nextInt(32); // 1970 - 2002

            profile.put("customerRef", rs.getString("cif"));
            profile.put("displayName", rs.getString("display_name"));
            profile.put("kycStatus", rs.getString("kyc_status"));
            profile.put("segment", rs.getString("segment"));
            profile.put("riskRating", rs.getString("risk_rating"));
            profile.put("identityMasked", rs.getString("identity_masked"));
            profile.put("birthYear", birthYear);
            profile.put("occupation", occupation);
            profile.put("branchRegistered", "Chi nhánh Sở Giao Dịch Hà Nội");
            profile.put("summary", String.format("Khách hàng %s (CIF: %s) · Phân hạng: %s · Nghề nghiệp: %s · KYC: %s · Xếp hạng rủi ro: %s.",
                rs.getString("display_name"), rs.getString("cif"), rs.getString("segment"), occupation, rs.getString("kyc_status"), rs.getString("risk_rating")));
            return profile;
        }, cif);
    }
}
