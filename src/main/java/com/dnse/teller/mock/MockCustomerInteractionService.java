package com.dnse.teller.mock;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * MockCustomerInteractionService:
 * Quản lý lịch sử tương tác 360 độ của khách hàng qua đa kênh:
 * 1. Lịch sử cuộc gọi lên tổng đài Contact Center (1900-5588-00).
 * 2. Lịch sử khiếu nại, phàn nàn, ý kiến đóng góp (VOC) và đánh giá CSAT/NPS.
 * 3. Lịch sử các sự cố/lỗi giao dịch gặp phải (Timeout Napas 24/7, Declined POS, ATM nuốt thẻ/khóa PIN, OTP chậm).
 * 4. Đánh giá mức độ hài lòng (Sentiment Analytics) và khuyến nghị hành động cho Giao dịch viên tại quầy.
 */
@Service
public class MockCustomerInteractionService {

    private final MockCustomerProfileService customerProfileService;

    public MockCustomerInteractionService(MockCustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    public Map<String, Object> getCustomerInteractionHistory(String customerRef) {
        String cif = customerProfileService.normalizeCif(customerRef);
        customerProfileService.ensureCustomerExists(cif);
        Map<String, Object> profile = customerProfileService.getCustomerProfile(cif);
        String name = (String) profile.getOrDefault("displayName", "Khách hàng");
        String segment = (String) profile.getOrDefault("segment", "STANDARD");

        Random rng = new Random(cif.hashCode() + 999);
        LocalDate today = LocalDate.now();

        // 1. Contact Center Inbound/Outbound Call History
        List<Map<String, Object>> callLogs = new ArrayList<>();
        String[] callReasons = {
            "Hỏi thủ tục nâng hạn mức thẻ tín dụng Visa Platinum",
            "Thắc mắc về phí biến động số dư OTT / SMS Banking",
            "Yêu cầu mở khóa thẻ thanh toán quốc tế do nhập sai CVV trên Shopee",
            "Hỏi lãi suất tiền gửi tiết kiệm online 12 tháng so với tại quầy",
            "Báo sự cố chuyển tiền 24/7 qua VietinBank bị trừ tiền nhưng người nhận chưa nhận được",
            "Tư vấn điều kiện mở gói tài khoản số đẹp VIP"
        };
        String[] callAgents = {"CallCenter-GDV-012 (Lê Thu Trang)", "CallCenter-GDV-045 (Phạm Quốc Huy)", "CallCenter-GDV-089 (Trần Mai Lan)", "CallCenter-GDV-003 (Đỗ Hải Đăng)"};
        String[] callChannels = {"HOTLINE_1900_558800", "LIVE_CHAT_APP", "ZALO_OA_SUPPORT", "HOTLINE_VIP_PRIORITY"};

        int numCalls = rng.nextInt(3) + 3; // 3 to 5 calls
        for (int i = 0; i < numCalls; i++) {
            LocalDate d = today.minusDays(i * 12 + rng.nextInt(6) + 1);
            Map<String, Object> call = new LinkedHashMap<>();
            call.put("callId", "CALL-2026-" + (100000 + rng.nextInt(900000)));
            call.put("timestamp", d.format(DateTimeFormatter.ISO_DATE) + " " + String.format("%02d:%02d:%02d", rng.nextInt(12) + 8, rng.nextInt(60), rng.nextInt(60)));
            call.put("channel", callChannels[rng.nextInt(callChannels.length)]);
            call.put("agent", callAgents[rng.nextInt(callAgents.length)]);
            call.put("reason", callReasons[i % callReasons.length]);
            call.put("durationSeconds", rng.nextInt(300) + 75);
            call.put("status", "RESOLVED");
            call.put("resolutionNote", "Đã hướng dẫn và giải đáp thỏa đáng cho khách hàng.");
            callLogs.add(call);
        }

        // 2. Customer Complaints & Voice of Customer (VOC) / CSAT Feedbacks
        List<Map<String, Object>> complaints = new ArrayList<>();
        Map<String, Object> comp1 = new LinkedHashMap<>();
        comp1.put("ticketId", "VOC-2026-0842");
        comp1.put("createdAt", today.minusDays(18).format(DateTimeFormatter.ISO_DATE));
        comp1.put("category", "FEE_DISPUTE");
        comp1.put("channel", "MOBILE_APP_FEEDBACK");
        comp1.put("title", "Thắc mắc phí thường niên thẻ Napas bị trừ 2 lần");
        comp1.put("customerComment", "Tôi thấy trong sao kê tháng vừa rồi bị trừ 2 lần phí thẻ 55.000 VND. Yêu cầu kiểm tra và hoàn lại nếu trừ thừa.");
        comp1.put("csatRating", 3); // 3/5 stars
        comp1.put("status", "RESOLVED");
        comp1.put("resolution", "Bộ phận Thẻ đã kiểm tra, hoàn tiền 55.000 VND thừa vào tài khoản thanh toán ngày " + today.minusDays(16).format(DateTimeFormatter.ISO_DATE) + " và gửi SMS xin lỗi.");
        complaints.add(comp1);

        Map<String, Object> comp2 = new LinkedHashMap<>();
        comp2.put("ticketId", "VOC-2026-1190");
        comp2.put("createdAt", today.minusDays(4).format(DateTimeFormatter.ISO_DATE));
        comp2.put("category", "SYSTEM_SERVICE_QUALITY");
        comp2.put("channel", "CONTACT_CENTER_1900");
        comp2.put("title", "Phản ánh cây ATM tại Chi nhánh Cầu Giấy bị bảo trì giờ tan tầm");
        comp2.put("customerComment", "Cây ATM góc đường Duy Tân không rút được tiền mặt lúc 17h30, báo đang bảo trì.");
        comp2.put("csatRating", 2); // 2/5 stars
        comp2.put("status", "RESOLVED");
        comp2.put("resolution", "Đã chuyển đội Vận hành Kỹ thuật nạp tiền và khôi phục hoạt động trong 45 phút.");
        complaints.add(comp2);

        // 3. Transaction Failures & Incidents History (Mobile, ATM, POS)
        List<Map<String, Object>> failedTransactions = new ArrayList<>();

        Map<String, Object> fail1 = new LinkedHashMap<>();
        fail1.put("incidentId", "INC-NAPAS-" + (rng.nextInt(90000) + 10000));
        fail1.put("timestamp", today.minusDays(3).format(DateTimeFormatter.ISO_DATE) + " 19:42:15");
        fail1.put("channel", "MOBILE_APP_QR_247");
        fail1.put("amount", 2_500_000L);
        fail1.put("beneficiaryBank", "VCB (Vietcombank)");
        fail1.put("errorCode", "ERR_NAPAS_GATEWAY_TIMEOUT");
        fail1.put("errorDescription", "Cổng chuyển mạch NAPAS phản hồi quá hạn (Timeout). Tiền tạm thời bị giữ.");
        fail1.put("resolutionStatus", "AUTO_REFUNDED");
        fail1.put("resolutionDetail", "Hệ thống tự động hoàn 2.500.000 VND về tài khoản nguồn sau 15 phút.");
        failedTransactions.add(fail1);

        Map<String, Object> fail2 = new LinkedHashMap<>();
        fail2.put("incidentId", "INC-POS-" + (rng.nextInt(90000) + 10000));
        fail2.put("timestamp", today.minusDays(14).format(DateTimeFormatter.ISO_DATE) + " 12:15:30");
        fail2.put("channel", "POS_MERCHANT (Golden Gate Restaurant)");
        fail2.put("amount", 1_850_000L);
        fail2.put("cardMasked", "•••• 8899 (Visa Platinum)");
        fail2.put("errorCode", "DECLINED_EXCEED_DAILY_CONTACTLESS_LIMIT");
        fail2.put("errorDescription", "Vượt hạn mức thanh toán chạm không cần PIN 1.000.000 VND/lần.");
        fail2.put("resolutionStatus", "CUSTOMER_RETRIED_SUCCESS");
        fail2.put("resolutionDetail", "Khách hàng cắm chip và nhập mã PIN thanh toán thành công.");
        failedTransactions.add(fail2);

        Map<String, Object> fail3 = new LinkedHashMap<>();
        fail3.put("incidentId", "INC-ATM-" + (rng.nextInt(90000) + 10000));
        fail3.put("timestamp", today.minusDays(35).format(DateTimeFormatter.ISO_DATE) + " 08:30:11");
        fail3.put("channel", "ATM_OFF_US (BIDV ATM Cầu Giấy)");
        fail3.put("amount", 5_000_000L);
        fail3.put("errorCode", "ATM_HARDWARE_DISPENSE_TIMEOUT");
        fail3.put("errorDescription", "Cơ cấu trả tiền của cây ATM đối tác bị nghẽn.");
        fail3.put("resolutionStatus", "DISPUTE_REFUNDED_100%");
        fail3.put("resolutionDetail", "Trung tâm Thẻ đã tra soát liên ngân hàng và ghi có 5.000.000 VND sau 24h.");
        failedTransactions.add(fail3);

        // 4. Sentiment Analytics & Recommendations for Teller
        int csatSum = 0;
        for (Map<String, Object> c : complaints) {
            csatSum += ((Number) c.getOrDefault("csatRating", 4)).intValue();
        }
        double avgCsat = complaints.isEmpty() ? 4.5 : Math.round((double) csatSum / complaints.size() * 10.0) / 10.0;
        int npsScore = avgCsat >= 4.0 ? 8 : (avgCsat >= 3.0 ? 6 : 4);
        String sentiment = avgCsat >= 4.0 ? "SATISFIED" : (avgCsat >= 3.0 ? "NEUTRAL" : "FRUSTRATED");

        String tellerRecommendation;
        if ("PRIORITY".equals(segment) || "DIAMOND_VIP".equals(segment)) {
            tellerRecommendation = String.format(
                "Khách hàng %s (%s). Vừa gặp sự cố lỗi Timeout Napas 2.500.000 VND cách đây 3 ngày (đã tự động hoàn tiền). GDV nên chủ động hỏi thăm nhẹ nhàng, gửi lời xin lỗi về sự bất tiện trên kênh Mobile và thông báo hệ thống tra soát đã giải quyết 100%%.",
                name, segment
            );
        } else {
            tellerRecommendation = String.format(
                "Khách hàng %s có lịch sử gọi tổng đài %d lần trong 90 ngày qua (chủ yếu hỏi hạn mức và phí). GDV nên hướng dẫn khách kích hoạt tính năng tra cứu hạn mức và quản lý thẻ trực tiếp trên App để tiết kiệm thời gian.",
                name, callLogs.size()
            );
        }

        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("overallSentiment", sentiment);
        analytics.put("csatAverageScore", avgCsat);
        analytics.put("npsScore", npsScore);
        analytics.put("totalContactCenterCalls90Days", callLogs.size());
        analytics.put("openComplaintsCount", 0);
        analytics.put("totalResolvedComplaints", complaints.size());
        analytics.put("failedTransactionsLast60Days", failedTransactions.size());
        analytics.put("churnRiskLevel", "LOW");
        analytics.put("recommendedActionForTeller", tellerRecommendation);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("customerRef", cif);
        res.put("displayName", name);
        res.put("segment", segment);
        res.put("analytics", analytics);
        res.put("contactCenterCalls", callLogs);
        res.put("complaintsAndVoc", complaints);
        res.put("transactionFailuresAndIssues", failedTransactions);
        res.put("summary", String.format(
            "Lịch sử tương tác khách hàng %s (%s): %d cuộc gọi tổng đài, %d khiếu nại VOC (100%% đã xử lý), %d sự cố giao dịch (đã hoàn tiền tra soát). Trạng thái hài lòng: %s (CSAT: %.1f/5.0). Gợi ý GDV: %s",
            name, cif, callLogs.size(), complaints.size(), failedTransactions.size(), sentiment, avgCsat, tellerRecommendation
        ));

        return res;
    }
}
