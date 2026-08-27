package com.dnse.teller.mock;

import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Tra cứu tỷ giá ngoại tệ trực tuyến và mạng lưới chi nhánh ngân hàng toàn quốc.
 */
@Service
public class MockMarketDirectoryService {

    private String formatMoney(long amount) {
        return new DecimalFormat("#,###").format(amount);
    }

    public Map<String, Object> lookupFxRate(String currency, long amount) {
        String curr = currency != null ? currency.toUpperCase().trim() : "USD";
        long amt = amount > 0 ? amount : 1000L;

        Map<String, Double> buyRates = Map.of(
            "USD", 25380.0,
            "EUR", 27450.0,
            "GBP", 32100.0,
            "JPY", 168.5,
            "SGD", 19250.0,
            "AUD", 16800.0,
            "CAD", 18500.0,
            "CHF", 28900.0,
            "CNY", 3520.0,
            "KRW", 18.8
        );

        double buy = buyRates.getOrDefault(curr, 25000.0);
        double sell = buy * 1.018; // +1.8% spread
        long totalVnd = Math.round(amt * buy);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("currency", curr);
        res.put("amount", amt);
        res.put("buyRate", buy);
        res.put("sellRate", sell);
        res.put("totalVndEquivalent", totalVnd);
        res.put("summary", String.format("Tỷ giá %s hôm nay: Mua vào %.1f VND - Bán ra %.1f VND. Quy đổi %s %s = %s VND.",
            curr, buy, sell, formatMoney(amt), curr, formatMoney(totalVnd)));

        return res;
    }

    public Map<String, Object> lookupBranch(String query) {
        String q = query != null ? query.toLowerCase().trim() : "hà nội";

        List<Map<String, Object>> branches = new ArrayList<>();
        if (q.contains("hồ chí minh") || q.contains("hcm") || q.contains("sài gòn")) {
            branches.add(Map.of("name", "Chi nhánh B.Smart Sài Gòn", "address", "Số 123 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh", "hotline", "1900-5588-01", "openingHours", "08:00 - 17:00 (Thứ 2 - Thứ 6)"));
            branches.add(Map.of("name", "PGD Bến Thành", "address", "Số 45 Lê Thánh Tôn, Quận 1, TP. Hồ Chí Minh", "hotline", "1900-5588-02", "openingHours", "08:00 - 17:00 (Thứ 2 - Thứ 6)"));
            branches.add(Map.of("name", "PGD Thủ Đức", "address", "Số 88 Võ Văn Ngân, TP. Thủ Đức", "hotline", "1900-5588-03", "openingHours", "08:00 - 16:30 (Thứ 2 - Thứ 6)"));
        } else if (q.contains("đà nẵng")) {
            branches.add(Map.of("name", "Chi nhánh B.Smart Đà Nẵng", "address", "Số 200 Nguyễn Văn Linh, Quận Hải Châu, TP. Đà Nẵng", "hotline", "1900-5588-04", "openingHours", "08:00 - 17:00 (Thứ 2 - Thứ 6)"));
        } else {
            branches.add(Map.of("name", "Hội sở chính & Chi nhánh Sở Giao Dịch", "address", "Số 54 Liễu Giai, Ba Đình, Hà Nội", "hotline", "1900-5588-00", "openingHours", "08:00 - 17:30 (Thứ 2 - Thứ 6), 08:00 - 11:30 (Thứ 7)"));
            branches.add(Map.of("name", "PGD Hoàn Kiếm", "address", "Số 12 Ngô Quyền, Hoàn Kiếm, Hà Nội", "hotline", "1900-5588-05", "openingHours", "08:00 - 17:00 (Thứ 2 - Thứ 6)"));
            branches.add(Map.of("name", "PGD Cầu Giấy", "address", "Số 241 Xuân Thủy, Cầu Giấy, Hà Nội", "hotline", "1900-5588-06", "openingHours", "08:00 - 17:00 (Thứ 2 - Thứ 6)"));
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("query", query);
        res.put("branches", branches);
        res.put("summary", String.format("Tìm thấy %d điểm giao dịch phù hợp tại khu vực '%s'. Trụ sở chính: %s.",
            branches.size(), query, branches.get(0).get("name") + " (" + branches.get(0).get("address") + ")"));

        return res;
    }
}
