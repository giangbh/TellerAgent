package com.dnse.teller.search;

import com.dnse.teller.mock.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GlobalSearchService {

    private final MockCustomerProfileService customerProfileService;
    private final MockAccountService accountService;
    private final MockMarketDirectoryService marketDirectoryService;

    public GlobalSearchService(
            MockCustomerProfileService customerProfileService,
            MockAccountService accountService,
            MockMarketDirectoryService marketDirectoryService
    ) {
        this.customerProfileService = customerProfileService;
        this.accountService = accountService;
        this.marketDirectoryService = marketDirectoryService;
    }

    public static class SearchResultCategory {
        private String type;
        private String title;
        private List<SearchResultItem> items = new ArrayList<>();

        public SearchResultCategory(String type, String title, List<SearchResultItem> items) {
            this.type = type;
            this.title = title;
            this.items = items != null ? items : new ArrayList<>();
        }

        public String getType() { return type; }
        public String getTitle() { return title; }
        public List<SearchResultItem> getItems() { return items; }
    }

    public static class SearchResultItem {
        private String id;
        private String title;
        private String subtitle;
        private String badge;
        private String badgeColor; // success, purple, warning, neutral
        private String icon; // user, credit-card, book-open, map-pin, zap
        private String actionType; // VIEW_ACCOUNT, VIEW_CUSTOMER, FILL_PROMPT, VIEW_BRANCH, VIEW_POLICY
        private String prompt;
        private Map<String, Object> payload;

        public SearchResultItem() {}

        public SearchResultItem(String id, String title, String subtitle, String badge, String badgeColor,
                                String icon, String actionType, String prompt, Map<String, Object> payload) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.badge = badge;
            this.badgeColor = badgeColor;
            this.icon = icon;
            this.actionType = actionType;
            this.prompt = prompt;
            this.payload = payload;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public String getBadge() { return badge; }
        public String getBadgeColor() { return badgeColor; }
        public String getIcon() { return icon; }
        public String getActionType() { return actionType; }
        public String getPrompt() { return prompt; }
        public Map<String, Object> getPayload() { return payload; }
    }

    public Map<String, Object> search(String query) {
        String q = query != null ? query.trim().toLowerCase() : "";
        List<SearchResultCategory> categories = new ArrayList<>();

        // 1. Tìm kiếm Tài khoản (Accounts)
        List<SearchResultItem> accountItems = searchAccounts(q);
        if (!accountItems.isEmpty()) {
            categories.add(new SearchResultCategory("ACCOUNTS", "Tài khoản thanh toán & Tiền gửi", accountItems));
        }

        // 2. Tìm kiếm Khách hàng / CIF (Customers)
        List<SearchResultItem> customerItems = searchCustomers(q);
        if (!customerItems.isEmpty()) {
            categories.add(new SearchResultCategory("CUSTOMERS", "Hồ sơ Khách hàng (CIF 360)", customerItems));
        }

        // 3. Tìm kiếm Quy trình & Biểu phí (Policies & Knowledge)
        List<SearchResultItem> policyItems = searchPolicies(q);
        if (!policyItems.isEmpty()) {
            categories.add(new SearchResultCategory("POLICIES", "Quy trình & Biểu phí nghiệp vụ", policyItems));
        }

        // 4. Mạng lưới Chi nhánh / PGD (Branches)
        List<SearchResultItem> branchItems = searchBranches(q);
        if (!branchItems.isEmpty()) {
            categories.add(new SearchResultCategory("BRANCHES", "Chi nhánh & Phòng giao dịch", branchItems));
        }

        // 5. Thao tác nhanh & Phím tắt AI (Quick Action Shortcuts)
        List<SearchResultItem> actionItems = searchActions(q);
        if (!actionItems.isEmpty()) {
            categories.add(new SearchResultCategory("ACTIONS", "Thao tác giao dịch nhanh", actionItems));
        }

        int totalResults = categories.stream().mapToInt(c -> c.getItems().size()).sum();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", query != null ? query : "");
        response.put("totalResults", totalResults);
        response.put("categories", categories);
        return response;
    }

    private List<SearchResultItem> searchAccounts(String q) {
        List<SearchResultItem> list = new ArrayList<>();
        List<Map<String, Object>> mockAccounts = List.of(
            Map.of("number", "3456789", "name", "Nguyễn Minh Anh", "cif", "CIF-0001842", "balance", "250.000.000 VND", "type", "Tài khoản thanh toán VND", "status", "Hoạt động"),
            Map.of("number", "012345678901", "name", "Nguyễn Văn An", "cif", "CIF-0001842", "balance", "42.500.000 VND", "type", "Tài khoản thanh toán 24/7", "status", "Hoạt động"),
            Map.of("number", "123456789999", "name", "Trần Thị Lan", "cif", "CIF-0009988", "balance", "18.200.000 VND", "type", "Tài khoản nguồn", "status", "Hoạt động"),
            Map.of("number", "TD-888999", "name", "Nguyễn Minh Anh", "cif", "CIF-0001842", "balance", "1.500.000.000 VND", "type", "Tiết kiệm có kỳ hạn 6T (5.8%)", "status", "Hoạt động")
        );

        for (Map<String, Object> acc : mockAccounts) {
            String num = (String) acc.get("number");
            String name = (String) acc.get("name");
            String cif = (String) acc.get("cif");
            String type = (String) acc.get("type");

            if (q.isEmpty() || num.toLowerCase().contains(q) || name.toLowerCase().contains(q) || cif.toLowerCase().contains(q) || q.contains("tài khoản") || q.contains("stk")) {
                list.add(new SearchResultItem(
                    num,
                    num + " · " + name,
                    acc.get("type") + " · Số dư: " + acc.get("balance"),
                    (String) acc.get("status"),
                    "success",
                    "credit-card",
                    "VIEW_ACCOUNT",
                    "tài khoản " + num,
                    acc
                ));
            }
        }
        return list;
    }

    private List<SearchResultItem> searchCustomers(String q) {
        List<SearchResultItem> list = new ArrayList<>();
        List<Map<String, Object>> mockCustomers = List.of(
            Map.of("cif", "CIF-0001842", "name", "Nguyễn Minh Anh", "cccd", "001092003847", "phone", "0987654321", "segment", "DIAMOND VIP", "job", "Chuyên gia CNTT · Hà Nội", "risk", "LOW"),
            Map.of("cif", "CIF-0002931", "name", "Trần Văn Hùng", "cccd", "079088001234", "phone", "0908123456", "segment", "GOLD PRIORITY", "job", "Giám đốc kinh doanh · TP.HCM", "risk", "LOW"),
            Map.of("cif", "CIF-0009988", "name", "Trần Thị Lan", "cccd", "036195009876", "phone", "0912345678", "segment", "STANDARD", "job", "Kế toán trưởng · Đà Nẵng", "risk", "MEDIUM")
        );

        for (Map<String, Object> cust : mockCustomers) {
            String cif = (String) cust.get("cif");
            String name = (String) cust.get("name");
            String cccd = (String) cust.get("cccd");
            String phone = (String) cust.get("phone");

            if (q.isEmpty() || cif.toLowerCase().contains(q) || name.toLowerCase().contains(q) || cccd.contains(q) || phone.contains(q) || q.contains("cif") || q.contains("khách hàng")) {
                list.add(new SearchResultItem(
                    cif,
                    cif + " · " + name,
                    "CCCD: " + cccd + " · SĐT: " + phone + " · " + cust.get("job"),
                    (String) cust.get("segment"),
                    "purple",
                    "user",
                    "VIEW_CUSTOMER",
                    "khách hàng " + cif,
                    cust
                ));
            }
        }
        return list;
    }

    private List<SearchResultItem> searchPolicies(String q) {
        List<SearchResultItem> list = new ArrayList<>();
        List<Map<String, String>> mockPolicies = List.of(
            Map.of("id", "POL-001", "title", "Quy trình nộp tiền mặt tại quầy", "sub", "Hạn mức tối đa GDV 500tr, kiểm soát viên duyệt trên 500tr", "prompt", "Quy trình nộp tiền mặt tại quầy như thế nào?"),
            Map.of("id", "POL-002", "title", "Quy trình rút tiền mặt & kiểm soát rủi ro", "sub", "Xác thực chữ ký/sinh trắc học, kiểm tra số dư và hạn mức chi quỹ", "prompt", "Quy trình rút tiền mặt tại quầy như thế nào?"),
            Map.of("id", "POL-003", "title", "Biểu phí chuyển tiền Napas 24/7 & Trong nước", "sub", "Miễn phí chuyển khoản nội bộ, biểu phí Napas 24/7 chi tiết", "prompt", "Biểu phí chuyển khoản tại quầy như thế nào?"),
            Map.of("id", "POL-004", "title", "Quy định nguyên tắc 4 mắt (Maker-Checker)", "sub", "Kiểm soát viên bắt buộc khác với GDV lập lệnh, không tự ký mắt thứ 2", "prompt", "Quy định nguyên tắc 4 mắt và phân quyền duyệt")
        );

        for (Map<String, String> pol : mockPolicies) {
            String title = pol.get("title");
            String sub = pol.get("sub");
            if (q.isEmpty() || title.toLowerCase().contains(q) || sub.toLowerCase().contains(q) || q.contains("quy trình") || q.contains("biểu phí") || q.contains("chính sách")) {
                list.add(new SearchResultItem(
                    pol.get("id"),
                    title,
                    sub,
                    "Chính sách",
                    "neutral",
                    "book-open",
                    "FILL_PROMPT",
                    pol.get("prompt"),
                    Map.of("prompt", pol.get("prompt"))
                ));
            }
        }
        return list;
    }

    private List<SearchResultItem> searchBranches(String q) {
        List<SearchResultItem> list = new ArrayList<>();
        List<Map<String, String>> mockBranches = List.of(
            Map.of("id", "CN-SGD-01", "name", "Chi nhánh Sở Giao Dịch Hà Nội", "addr", "54 Liễu Giai, Ba Đình, Hà Nội · Hotline: 1900-1088"),
            Map.of("id", "CN-HCM-01", "name", "Chi nhánh TP. Hồ Chí Minh", "addr", "128 Hai Bà Trưng, Quận 1, TP.HCM · Hotline: 1900-1089"),
            Map.of("id", "CN-DN-01", "name", "Chi nhánh Đà Nẵng", "addr", "218 Bạch Đằng, Hải Châu, Đà Nẵng · Hotline: 1900-1090")
        );

        for (Map<String, String> br : mockBranches) {
            String name = br.get("name");
            String addr = br.get("addr");
            if (q.isEmpty() || name.toLowerCase().contains(q) || addr.toLowerCase().contains(q) || q.contains("chi nhánh") || q.contains("pgd") || q.contains("địa chỉ")) {
                list.add(new SearchResultItem(
                    br.get("id"),
                    name,
                    addr,
                    "Mạng lưới",
                    "neutral",
                    "map-pin",
                    "FILL_PROMPT",
                    "Địa chỉ và hotline " + name,
                    Map.of("branchId", br.get("id"), "name", name, "address", addr)
                ));
            }
        }
        return list;
    }

    private List<SearchResultItem> searchActions(String q) {
        List<SearchResultItem> list = new ArrayList<>();
        List<Map<String, String>> actions = List.of(
            Map.of("id", "act-deposit", "title", "Nộp tiền mặt tự điền", "sub", "Tạo bản nháp nộp tiền mặt vào tài khoản", "prompt", "Nộp tiền 50tr vào tài khoản 3456789"),
            Map.of("id", "act-withdraw", "title", "Rút tiền mặt tự điền", "sub", "Tạo bản nháp rút tiền và kiểm tra số dư", "prompt", "Rút tiền 50tr từ tài khoản 3456789"),
            Map.of("id", "act-transfer", "title", "Chuyển tiền liên ngân hàng 24/7", "sub", "Chuyển tiền nhanh Napas 24/7", "prompt", "Chuyển 50 triệu cho Nguyễn Văn An tại Vietcombank, số tài khoản 012345678901"),
            Map.of("id", "act-fx", "title", "Tra cứu tỷ giá ngoại tệ & quy đổi", "sub", "Tra cứu tỷ giá USD, EUR, JPY thời gian thực", "prompt", "Tỷ giá 2000 EUR hôm nay đổi ra bao nhiêu VND?"),
            Map.of("id", "act-history", "title", "Sao kê & Biến động dòng tiền", "sub", "Trích lục 30 giao dịch gần nhất của khách hàng", "prompt", "Xem lịch sử giao dịch và biến động số dư của tài khoản 3456789"),
            Map.of("id", "act-persona", "title", "Phân tích chân dung 360 & Gợi ý NBO", "sub", "Phân tích hành vi, khẩu vị đầu tư và ưu đãi", "prompt", "Phân tích chân dung khách hàng CIF-0001842 và đề xuất sản phẩm phù hợp")
        );

        for (Map<String, String> act : actions) {
            String title = act.get("title");
            String sub = act.get("sub");
            if (q.isEmpty() || title.toLowerCase().contains(q) || sub.toLowerCase().contains(q) || q.contains("nộp") || q.contains("rút") || q.contains("chuyển") || q.contains("tỷ giá") || q.contains("sao kê")) {
                list.add(new SearchResultItem(
                    act.get("id"),
                    title,
                    sub,
                    "Phím tắt AI",
                    "success",
                    "zap",
                    "FILL_PROMPT",
                    act.get("prompt"),
                    Map.of("prompt", act.get("prompt"))
                ));
            }
        }
        return list;
    }
}
