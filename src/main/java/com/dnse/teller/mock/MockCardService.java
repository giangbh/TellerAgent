package com.dnse.teller.mock;

import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Quản trị & vận hành dịch vụ thẻ ghi nợ nội địa, thẻ tín dụng quốc tế và thao tác thẻ.
 */
@Service
public class MockCardService {

    private final MockCustomerProfileService customerProfileService;

    public MockCardService(MockCustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    private String formatMoney(long amount) {
        return new DecimalFormat("#,###").format(amount);
    }

    public Map<String, Object> getCardServices(String customerRef) {
        String cif = customerProfileService.normalizeCif(customerRef);
        customerProfileService.ensureCustomerExists(cif);
        Map<String, Object> profile = customerProfileService.getCustomerProfile(cif);

        Random rng = new Random(cif.hashCode() + 404);
        List<Map<String, Object>> cards = new ArrayList<>();

        String segment = String.valueOf(profile.get("segment"));
        long creditLimit = "DIAMOND_VIP".equals(segment) ? 300_000_000L : ("PRIORITY".equals(segment) ? 100_000_000L : 50_000_000L);
        long availableLimit = Math.round(creditLimit * (0.6 + (rng.nextDouble() * 0.35)));

        cards.add(Map.of(
            "cardRef", "CRD-VISA-" + (rng.nextInt(900) + 100),
            "cardType", "CREDIT_VISA_PLATINUM",
            "cardNumberMasked", String.format("4000 •••• •••• %04d", rng.nextInt(9000) + 1000),
            "status", "ACTIVE",
            "creditLimit", creditLimit,
            "availableLimit", availableLimit,
            "ecomEnabled", true,
            "contactlessEnabled", true,
            "expiryDate", "12/29"
        ));

        cards.add(Map.of(
            "cardRef", "CRD-NAPAS-" + (rng.nextInt(900) + 100),
            "cardType", "DEBIT_NAPAS_DOMESTIC",
            "cardNumberMasked", String.format("9704 •••• •••• %04d", rng.nextInt(9000) + 1000),
            "status", "ACTIVE",
            "dailyAtmWithdrawalLimit", 50_000_000L,
            "ecomEnabled", true,
            "contactlessEnabled", true,
            "expiryDate", "08/30"
        ));

        if ("DIAMOND_VIP".equals(segment)) {
            cards.add(Map.of(
                "cardRef", "CRD-WORLD-" + (rng.nextInt(900) + 100),
                "cardType", "CREDIT_MASTERCARD_WORLD_ELITE",
                "cardNumberMasked", String.format("5200 •••• •••• %04d", rng.nextInt(9000) + 1000),
                "status", "ACTIVE",
                "creditLimit", 500_000_000L,
                "availableLimit", 480_000_000L,
                "ecomEnabled", true,
                "contactlessEnabled", true,
                "expiryDate", "05/31"
            ));
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("customerRef", cif);
        res.put("customerName", profile.get("displayName"));
        res.put("cardCount", cards.size());
        res.put("cards", cards);
        res.put("summary", String.format("Khách hàng %s có %d thẻ đang hoạt động (Gồm thẻ tín dụng hạn mức %s VND, khả dụng %s VND và thẻ ghi nợ nội địa Napas).",
            profile.get("displayName"), cards.size(), formatMoney(creditLimit), formatMoney(availableLimit)));

        return res;
    }
}
