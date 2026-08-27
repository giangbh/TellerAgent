# Hệ Sinh Thái 23 Công Cụ Model Context Protocol (MCP Ecosystem)

Dưới đây là danh mục chi tiết 23 công cụ ngân hàng chuẩn hóa được triển khai trên nền tảng **B.Smart Teller Copilot**:

---

## 1. Phân Nhóm Năng Lực & 23 Công Cụ

### 1.1. Nhóm CIF, Khách Hàng & Portfolio (3 Tools)
1. `customer.profile.read` (`customer_profile_read`): Đọc hồ sơ định danh KYC, phân khúc khách hàng (`STANDARD`, `GOLD`, `PRIORITY`, `DIAMOND_VIP`), CCCD masked, nghề nghiệp.
2. `customer.accounts.list` (`customer_accounts_list`): Liệt kê danh sách các tài khoản thanh toán đang hoạt động và số dư khả dụng.
3. `customer.accounts.summary` (`customer_accounts_summary`): Tổng hợp toàn bộ tài sản AUM bao gồm tài khoản thanh toán và sổ tiết kiệm tiền gửi.

### 1.2. Nhóm Phân Tích Chân Dung 360, Tín Dụng & NBO (3 Tools)
4. `customer.persona.analytics` (`customer_persona_analytics`): Phân tích chân dung khách hàng 360, khẩu vị rủi ro đầu tư (Tăng trưởng mạo hiểm / Bảo toàn vốn), tần suất giao dịch hàng tháng, kênh giao dịch ưa chuộng (Mobile / Quầy / POS).
5. `customer.credit.score.check` (`customer_credit_score_check`): Chấm điểm tín dụng CIC (680-820), xếp hạng tín nhiệm AAA/AA/A, kiểm tra nhóm nợ CIC 1 và phê duyệt hạn mức vay/thấu chi trước.
6. `recommendation.nbo.products` (`recommendation_nbo_products`): Đề xuất danh mục sản phẩm tiếp theo Next-Best-Offer (NBO) cá nhân hóa cho từng khách hàng.

### 1.3. Nhóm Sao Kê, Tiền Gửi & Dịch Vụ Thẻ (3 Tools)
7. `statement.transaction.history` (`statement_transaction_history`): Trích lục 10-30 giao dịch gần nhất dạng dữ liệu thô (raw JSON) để AI Copilot tự động tính toán tổng dòng tiền vào/ra và tìm giao dịch lớn nhất.
8. `savings.product.advisor` (`savings_product_advisor`): Cố vấn tối ưu hóa lãi suất tiền gửi và mô phỏng tiền lãi khi đáo hạn theo đa kỳ hạn (1, 3, 6, 9, 12, 18, 24, 36 tháng).
9. `card.service.manage` (`card_service_manage`): Quản trị danh mục thẻ ATM, Debit Napas, Credit Visa Platinum/World Mastercard (hạn mức, ngày hết hạn, eCom, contactless, đổi PIN, khóa/mở thẻ).

### 1.4. Nhóm Tra Cứu Thị Trường, Tỷ Giá & Chính Sách (5 Tools)
10. `fx.rate.lookup` (`fx_rate_lookup`): Tra cứu tỷ giá mua/bán và quy đổi tương đương của 10 ngoại tệ phổ biến (USD, EUR, GBP, JPY, SGD, AUD, CAD, CHF, CNY, KRW).
11. `branch.directory.lookup` (`branch_directory_lookup`): Tra cứu danh bạ chi nhánh, phòng giao dịch, địa chỉ, hotline và giờ làm việc.
12. `bank.directory.lookup` (`bank_directory_lookup`): Tra cứu mã ngân hàng và định tuyến NAPAS 24/7.
13. `pricing.transfer.fee` (`pricing_transfer_fee`): Tra cứu biểu phí giao dịch chuyển khoản nội bộ và liên ngân hàng.
14. `knowledge.policy.search` (`knowledge_policy_search`): Tra cứu quy trình nghiệp vụ nội bộ, hạn mức giao dịch và văn bản chính sách.

### 1.5. Nhóm Kiểm Soát Hạn Mức, Rủi Ro & Soạn Thảo Live Draft (7 Tools)
15. `transfer.limit.check` (`transfer_limit_check`): Kiểm tra hạn mức giao dịch chuyển khoản tại quầy.
16. `cash.limit.check` (`cash_limit_check`): Kiểm tra hạn mức nộp/rút tiền mặt tại quầy.
17. `risk.transfer.screen` (`risk_transfer_screen`): Sàng lọc rủi ro AML và cảnh báo tài khoản danh sách đen chuyển tiền.
18. `risk.cash.screen` (`risk_cash_screen`): Sàng lọc phòng chống rửa tiền giao dịch tiền mặt giá trị lớn.
19. `transaction.draft.validate` (`transaction_draft_validate`): Soạn thảo và kiểm tra hợp lệ Live Draft lệnh chuyển tiền.
20. `cash.draft.validate` (`cash_draft_validate`): Soạn thảo và kiểm tra hợp lệ Live Draft lệnh nộp/rút tiền mặt.
21. `account.resolve.by_number` (`account_resolve_by_number`): Xác thực và đối soát số tài khoản thụ hưởng / trích nợ.

### 1.6. Nhóm Hạch Toán Tài Chính Core (2 Financial Write Tools)
22. `core.transfer.execute` (`core_transfer_execute`): Hạch toán lệnh chuyển tiền vào Core Banking (Enforce Idempotency Key & Kiểm soát 4 mắt).
23. `core.cash.execute` (`core_cash_execute`): Hạch toán lệnh nộp/rút tiền mặt vào Core Banking (Enforce Idempotency Key & Kiểm soát 4 mắt).
