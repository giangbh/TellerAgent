package com.dnse.teller.mock;

import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Chấm điểm tín dụng, tra cứu lịch sử CIC và phê duyệt hạn mức trước.
 */
@Service
public class MockCreditScoreService {

    private final MockCustomerProfileService customerProfileService;

    private static final String[] CIC_GROUPS = {
        "Nhóm 1 (Dư nợ chuẩn, không có lịch sử nợ xấu)",
        "Nhóm 1 (Dư nợ chuẩn, lịch sử thanh toán thẻ đúng hạn 100%)",
        "Nhóm 1 (Khách hàng tín nhiệm cao, không có nợ quá hạn)"
    };

    public MockCreditScoreService(MockCustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    private String formatMoney(long amount) {
        return new DecimalFormat("#,###").format(amount);
    }

    public Map<String, Object> getCustomerCreditScore(String customerRef) {
        String cif = customerProfileService.normalizeCif(customerRef);
        customerProfileService.ensureCustomerExists(cif);
        Map<String, Object> profile = customerProfileService.getCustomerProfile(cif);

        Random rng = new Random(cif.hashCode() + 202);
        int score = 680 + Math.abs(rng.nextInt() % 140); // 680 - 820
        String cicGroup = CIC_GROUPS[Math.abs(rng.nextInt()) % CIC_GROUPS.length];

        String segment = String.valueOf(profile.get("segment"));
        long preApprovedLimit;
        if ("DIAMOND_VIP".equals(segment)) {
            preApprovedLimit = (rng.nextInt(5) + 5) * 100_000_000L; // 500tr - 900tr
        } else if ("PRIORITY".equals(segment)) {
            preApprovedLimit = (rng.nextInt(3) + 2) * 100_000_000L; // 200tr - 400tr
        } else {
            preApprovedLimit = (rng.nextInt(5) + 3) * 10_000_000L;  // 30tr - 70tr
        }

        String grade = score >= 780 ? "AAA (Tín nhiệm xuất sắc)" : (score >= 720 ? "AA (Tín nhiệm rất tốt)" : "A (Tín nhiệm tốt)");

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("customerRef", cif);
        res.put("customerName", profile.get("displayName"));
        res.put("creditScore", score);
        res.put("creditRatingGrade", grade);
        res.put("cicStatus", cicGroup);
        res.put("preApprovedLoanLimit", preApprovedLimit);
        res.put("overdraftEligible", true);
        res.put("maxOverdraftAmount", Math.min(preApprovedLimit / 2, 200_000_000L));
        res.put("summary", String.format("Tín dụng KH %s: Điểm tín nhiệm %d/850 (%s), CIC: %s, Hạn mức vay phê duyệt trước: %s VND.",
            profile.get("displayName"), score, grade, cicGroup, formatMoney(preApprovedLimit)));

        return res;
    }
}
