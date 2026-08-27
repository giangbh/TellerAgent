package com.dnse.teller.mock;

import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Cố vấn tối ưu lãi suất tiền gửi và mô phỏng tiền lãi đáo hạn.
 */
@Service
public class MockSavingsAdvisorService {

    private String formatMoney(long amount) {
        return new DecimalFormat("#,###").format(amount);
    }

    public Map<String, Object> calculateSavingsYield(long amount, int termMonths) {
        long baseAmount = amount > 0 ? amount : 100_000_000L;
        int term = termMonths > 0 ? termMonths : 6;

        Map<Integer, Double> rateTable = Map.of(
            1, 3.2,
            3, 4.0,
            6, 5.6,
            9, 5.8,
            12, 6.2,
            18, 6.4,
            24, 6.7,
            36, 7.0
        );

        double rate = rateTable.getOrDefault(term, 5.5);
        long interestEarned = Math.round(baseAmount * (rate / 100.0) * (term / 12.0));
        long totalMaturity = baseAmount + interestEarned;

        List<Map<String, Object>> comparison = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : rateTable.entrySet()) {
            int t = entry.getKey();
            double r = entry.getValue();
            long earned = Math.round(baseAmount * (r / 100.0) * (t / 12.0));
            comparison.add(Map.of(
                "termMonths", t,
                "interestRate", r,
                "estimatedInterest", earned,
                "totalAtMaturity", baseAmount + earned
            ));
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

    public Map<String, Object> adviseSavingsPlan(long amount, int termMonths) {
        return calculateSavingsYield(amount, termMonths);
    }
}
