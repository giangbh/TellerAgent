package com.dnse.teller.mock;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Đề xuất sản phẩm Next-Best-Offer (NBO) cá nhân hóa cho từng khách hàng.
 */
@Service
public class MockNextBestOfferService {

    private final MockCustomerProfileService customerProfileService;

    private static final List<Map<String, Object>> MASTER_OFFER_CATALOG = List.of(
        Map.of(
            "productCode", "CARD-CASHBACK-SIGNATURE",
            "productName", "Thẻ Tín Dụng B.Smart Signature Cashback",
            "benefit", "Hoàn tiền 10% chi tiêu ẩm thực, phòng chờ sân bay thương gia không giới hạn",
            "category", "CREDIT_CARD",
            "priorityScore", 0.95
        ),
        Map.of(
            "productCode", "SAVINGS-ACCUMULATIVE-6M",
            "productName", "Gói Tiền Gửi Đắc Lộc 6 Tháng Lãi Suất 6.2%/năm",
            "benefit", "Tặng 0.3%/năm lãi suất thưởng chào mừng kỳ hạn linh hoạt",
            "category", "TERM_DEPOSIT",
            "priorityScore", 0.92
        ),
        Map.of(
            "productCode", "OVERDRAFT-LINE-FAST",
            "productName", "Hạn Mức Thấu Chi Tài Khoản Trực Tuyến FastLine",
            "benefit", "Cấp sẵn hạn mức 100-300 triệu VND, miễn lãi 15 ngày đầu, giải ngân 1 phút",
            "category", "LENDING",
            "priorityScore", 0.88
        ),
        Map.of(
            "productCode", "INSURANCE-INVEST-LINK",
            "productName", "Bảo Hiểm Đầu Tư Thịnh Vượng Gia Đình",
            "benefit", "Bảo vệ tài chính toàn diện đến 5 tỷ VND kèm quỹ đầu tư sinh lời 10-14%/năm",
            "category", "BANCASSURANCE",
            "priorityScore", 0.85
        ),
        Map.of(
            "productCode", "BEAUTIFUL-ACCOUNT-VIP",
            "productName", "Mở Tài Khoản Số Đẹp / Số Phong Thủy Miễn Phí",
            "benefit", "Tặng số tài khoản Lộc Phát / Tứ Quý trị giá 25 triệu VND cho khách hàng thân thiết",
            "category", "ACCOUNT_PRIVILEGE",
            "priorityScore", 0.82
        )
    );

    public MockNextBestOfferService(MockCustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    public Map<String, Object> getNextBestOffers(String customerRef) {
        String cif = customerProfileService.normalizeCif(customerRef);
        customerProfileService.ensureCustomerExists(cif);
        Map<String, Object> profile = customerProfileService.getCustomerProfile(cif);

        Random rng = new Random(cif.hashCode() + 303);
        List<Map<String, Object>> pool = new ArrayList<>(MASTER_OFFER_CATALOG);
        Collections.shuffle(pool, rng);

        List<Map<String, Object>> top3Offers = pool.subList(0, 3);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("customerRef", cif);
        res.put("customerName", profile.get("displayName"));
        res.put("segment", profile.get("segment"));
        res.put("offers", top3Offers);
        res.put("summary", String.format("Top 3 Offer tư vấn cho KH %s (%s): 1) %s · 2) %s · 3) %s.",
            profile.get("displayName"), profile.get("segment"),
            top3Offers.get(0).get("productName"),
            top3Offers.get(1).get("productName"),
            top3Offers.get(2).get("productName")
        ));

        return res;
    }
}
