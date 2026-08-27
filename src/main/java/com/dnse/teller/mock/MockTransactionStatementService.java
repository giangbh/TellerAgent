package com.dnse.teller.mock;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Quản lý trích lục lịch sử giao dịch sao kê (10 - 30 giao dịch ngẫu nhiên phong phú).
 */
@Service
public class MockTransactionStatementService {

    private final MockCustomerProfileService customerProfileService;

    private static final String[] INFLOW_DESCRIPTIONS = {
        "Nhận tiền chuyển khoản lương từ CTY TNHH ABC",
        "Tiền lãi tiết kiệm định kỳ có kỳ hạn",
        "Khách hàng thanh toán tiền hợp đồng cung cấp dịch vụ",
        "Hoàn tiền ưu đãi chi tiêu thẻ tín dụng Signature Cashback",
        "Nhận kiều hối Western Union từ người thân tại Mỹ",
        "Thu hồi vốn & lợi nhuận đầu tư chứng khoán",
        "Nhận tiền chuyển khoản 24/7 từ đối tác kinh doanh",
        "Thanh lý tài sản cố định & thu hồi công nợ",
        "Nhận cổ tức bằng tiền mặt CTCP FPT"
    };
    private static final long[] INFLOW_BASE_AMOUNTS = {
        45_000_000L, 7_500_000L, 24_000_000L, 1_850_000L, 16_000_000L, 38_000_000L, 12_500_000L, 65_000_000L, 5_200_000L
    };

    private static final String[] OUTFLOW_DESCRIPTIONS = {
        "Thanh toán QR POS siêu thị WinMart+ & Co.opmart",
        "Chuyển tiền nhanh 24/7 thanh toán tiền thuê văn phòng",
        "Thanh toán tiền điện lực EVN và Internet cáp quang VNPT",
        "Rút tiền mặt tại cây ATM VCB / Techcombank",
        "Mua sắm online sàn TMĐT Shopee Pay / Tiki / Lazada",
        "Nộp phí bảo hiểm nhân thọ Manulife / Dai-ichi định kỳ",
        "Thanh toán toàn bộ dư nợ sao kê thẻ tín dụng Visa",
        "Ăn uống liên hoan nhà hàng ẩm thực Golden Gate QR Pay",
        "Đổ xăng dầu tại trạm Petrolimex qua QR Code",
        "Thanh toán học phí đại học quốc tế kỳ I",
        "Đặt vé máy bay & phòng khách sạn Vietnam Airlines / Agoda",
        "Chuyển tiền mừng cưới & biếu tặng người thân"
    };
    private static final long[] OUTFLOW_BASE_AMOUNTS = {
        1_850_000L, 8_000_000L, 2_450_000L, 3_000_000L, 950_000L, 12_500_000L, 7_800_000L, 1_650_000L, 850_000L, 28_000_000L, 6_200_000L, 2_000_000L
    };

    public MockTransactionStatementService(MockCustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    public Map<String, Object> getTransactionHistory(String accountNumber, String customerRef) {
        return getTransactionHistory(accountNumber, customerRef, 15);
    }

    public Map<String, Object> getTransactionHistory(String accountNumber, String customerRef, int requestedCount) {
        String num = accountNumber != null && !accountNumber.trim().isEmpty() ? accountNumber.trim() : "3456789";
        String cif = customerProfileService.normalizeCif(customerRef);

        int count = Math.clamp(requestedCount, 10, 30);
        List<Map<String, Object>> txs = new ArrayList<>();
        LocalDate today = LocalDate.now();
        Random rng = new Random(num.hashCode() + 777);

        long currentBal = 250_000_000L;
        int txCounter = 2001;

        for (int i = 1; i <= count; i++) {
            boolean isInflow = (i % 3 == 0) || (i == 1) || (i == count / 2);
            int dayOffset = i * 2 + (rng.nextInt(2));
            String txId = "TX-2026-" + (txCounter++);
            String date = today.minusDays(dayOffset).toString();

            if (isInflow) {
                int idx = (i + Math.abs(rng.nextInt())) % INFLOW_DESCRIPTIONS.length;
                long amt = INFLOW_BASE_AMOUNTS[idx] + (rng.nextInt(6) * 500_000L);
                currentBal += amt;
                txs.add(Map.of(
                    "txId", txId,
                    "date", date,
                    "description", INFLOW_DESCRIPTIONS[idx],
                    "amount", amt,
                    "type", "INFLOW",
                    "balanceAfter", currentBal
                ));
            } else {
                int idx = (i + Math.abs(rng.nextInt())) % OUTFLOW_DESCRIPTIONS.length;
                long amt = -(OUTFLOW_BASE_AMOUNTS[idx] + (rng.nextInt(4) * 200_000L));
                currentBal += amt;
                txs.add(Map.of(
                    "txId", txId,
                    "date", date,
                    "description", OUTFLOW_DESCRIPTIONS[idx],
                    "amount", amt,
                    "type", "OUTFLOW",
                    "balanceAfter", currentBal
                ));
            }
        }

        long totalInflow = 0;
        long totalOutflow = 0;
        int inflowCount = 0;
        int outflowCount = 0;
        Map<String, Object> largestInflow = null;
        Map<String, Object> largestOutflow = null;
        long maxInflowVal = -1;
        long maxOutflowVal = -1;

        for (Map<String, Object> tx : txs) {
            String type = (String) tx.get("type");
            long amt = (Long) tx.get("amount");
            if ("INFLOW".equals(type)) {
                totalInflow += amt;
                inflowCount++;
                if (amt > maxInflowVal) {
                    maxInflowVal = amt;
                    largestInflow = tx;
                }
            } else {
                long absAmt = Math.abs(amt);
                totalOutflow += absAmt;
                outflowCount++;
                if (absAmt > maxOutflowVal) {
                    maxOutflowVal = absAmt;
                    largestOutflow = tx;
                }
            }
        }

        long netCashflow = totalInflow - totalOutflow;
        long avgInflow = inflowCount > 0 ? totalInflow / inflowCount : 0;
        long avgOutflow = outflowCount > 0 ? totalOutflow / outflowCount : 0;

        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("totalInflow", totalInflow);
        analytics.put("totalOutflow", totalOutflow);
        analytics.put("netCashflow", netCashflow);
        analytics.put("inflowCount", inflowCount);
        analytics.put("outflowCount", outflowCount);
        analytics.put("largestInflowTransaction", largestInflow);
        analytics.put("largestOutflowTransaction", largestOutflow);
        analytics.put("averageInflowAmount", avgInflow);
        analytics.put("averageOutflowAmount", avgOutflow);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("accountNumber", num);
        res.put("customerRef", cif);
        res.put("transactionCount", txs.size());
        res.put("analytics", analytics);
        res.put("transactions", txs);
        res.put("summary", String.format(
            "Sao kê tài khoản %s (%d giao dịch): Tổng vào %s VND (%d GD, lớn nhất: %s VND - %s), Tổng ra %s VND (%d GD, lớn nhất: %s VND - %s), Dòng tiền ròng: %s VND.",
            num, txs.size(),
            String.format("%,d", totalInflow), inflowCount, String.format("%,d", maxInflowVal), largestInflow != null ? largestInflow.get("description") : "N/A",
            String.format("%,d", totalOutflow), outflowCount, String.format("%,d", maxOutflowVal), largestOutflow != null ? largestOutflow.get("description") : "N/A",
            String.format("%,d", netCashflow)
        ));

        return res;
    }
}
