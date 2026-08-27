package com.dnse.teller.mock;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Phân tích chân dung khách hàng 360, khẩu vị đầu tư, thói quen chi tiêu và kênh giao dịch.
 */
@Service
public class MockCustomerPersonaService {

    private final MockCustomerProfileService customerProfileService;

    private static final List<String> PERSONA_CATEGORIES = List.of(
        "Doanh nhân / Chủ hộ kinh doanh vừa và nhỏ",
        "Chuyên gia công nghệ & Quản lý cấp cao",
        "Khách hàng cao cấp ưa chuộng tích lũy & Bất động sản",
        "Người làm việc tự do / Freelancer thu nhập cao",
        "Nhà đầu tư chứng khoán năng động",
        "Gia đình trẻ định hướng tài chính an toàn",
        "Hưu trí an nhàn & Tiết kiệm dưỡng già"
    );

    private static final List<String> RISK_APPETITES = List.of(
        "TĂNG TRƯỞNG & ĐẦU TƯ MẠO HIỂM",
        "CÂN BẰNG TĂNG TRƯỞNG & BẢO TOÀN VỐN",
        "THẬN TRỌNG & ƯU TIÊN LÃI SUẤT TIỀN GỬI BẢO ĐẢM",
        "TỐI ĐA HÓA LỢI NHUẬN NGẮN HẠN"
    );

    private static final List<String> PREFERRED_CHANNELS = List.of(
        "Mobile Banking (App) & QR Pay (Chiếm 85%)",
        "Internet Banking & Thẻ Tín Dụng Quốc Tế (Chiếm 75%)",
        "Giao dịch tại Quầy & Chi Nhánh VIP (Chiếm 60%)",
        "Đa kênh Omnichannel linh hoạt (Mobile 50% + Quầy 50%)"
    );

    public MockCustomerPersonaService(MockCustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    public Map<String, Object> getCustomerPersona(String customerRef) {
        String cif = customerProfileService.normalizeCif(customerRef);
        customerProfileService.ensureCustomerExists(cif);
        Map<String, Object> profile = customerProfileService.getCustomerProfile(cif);

        Random rng = new Random(cif.hashCode() + 101);
        String persona = PERSONA_CATEGORIES.get(Math.abs(rng.nextInt()) % PERSONA_CATEGORIES.size());
        String riskAppetite = RISK_APPETITES.get(Math.abs(rng.nextInt()) % RISK_APPETITES.size());
        String channel = PREFERRED_CHANNELS.get(Math.abs(rng.nextInt()) % PREFERRED_CHANNELS.size());

        int age = 28 + (Math.abs(rng.nextInt()) % 35);
        int tenureMonths = 6 + (Math.abs(rng.nextInt()) % 72);
        int monthlyTxCount = 15 + (Math.abs(rng.nextInt()) % 80);
        long avgMonthlyInflow = (rng.nextInt(150) + 20) * 1_000_000L;

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("customerRef", cif);
        res.put("customerName", profile.get("displayName"));
        res.put("segment", profile.get("segment"));
        res.put("estimatedAge", age);
        res.put("relationshipTenure", tenureMonths + " tháng");
        res.put("monthlyTxFrequency", monthlyTxCount + " giao dịch/tháng");
        res.put("averageMonthlyInflow", avgMonthlyInflow);
        res.put("personaCategory", persona);
        res.put("preferredChannel", channel);
        res.put("investmentRiskAppetite", riskAppetite);
        res.put("summary", String.format("Chân dung KH %s (%s): Nhóm '%s', gắn bó %d tháng, kênh ưa chuộng: %s, khẩu vị: %s.",
            profile.get("displayName"), profile.get("segment"), persona, tenureMonths, channel, riskAppetite));

        return res;
    }
}
