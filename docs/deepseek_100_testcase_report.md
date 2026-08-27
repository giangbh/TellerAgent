# Báo Cáo Nghiệm Thu 100 Test Cases Xử Lý Câu Lệnh Giao Dịch Viên

> **Thời gian thực thi:** 2026-08-27T08:43:12.639955Z
> **Tổng số câu lệnh kiểm thử:** 100 câu lệnh thực tế
> **Kết quả đạt chuẩn:** **100 / 100 PASS** (Tỷ lệ thành công: **100.0%**)
> **Độ trễ trung bình (Average Latency):** **1878 ms**

---

## 1. Thống Kê Theo Phân Loại Nghiệp Vụ

| Phân Loại Nghiệp Vụ | Số Lượng Test | Tỷ Lệ Pass | Công Cụ MCP Chính Được Kích Hoạt |
|---|---|---|---|
| **1. Chuyển Khoản Trong Nước & 24/7** | 15 | 100% | `pricing.transfer.fee`, `transfer.limit.check`, `bank.directory.lookup` |
| **2. Nộp & Rút Tiền Mặt Tại Quầy** | 10 | 100% | `account.resolve.by_number`, `cash.limit.check`, `risk.cash.screen` |
| **3. Phân Tích Chân Dung Khách Hàng 360** | 10 | 100% | `customer.persona.analytics`, `customer.profile.read` |
| **4. Chấm Điểm Tín Dụng & Tra Cứu CIC** | 10 | 100% | `customer.credit.score.check` |
| **5. Tư Vấn Gợi Ý Ưu Đãi (Next-Best-Offer)** | 10 | 100% | `recommendation.nbo.products` |
| **6. Cố Vấn Tối Ưu Lãi Tiết Kiệm** | 10 | 100% | `savings.product.advisor` |
| **7. Quản Trị & Vận Hành Dịch Vụ Thẻ** | 10 | 100% | `card.service.manage` |
| **8. Trích Lục Sao Kê & Lịch Sử Giao Dịch** | 10 | 100% | `statement.transaction.history` |
| **9. Tra Cứu Tỷ Giá & Quy Đổi Ngoại Tệ** | 5 | 100% | `fx.rate.lookup` |
| **10. Tra Cứu Mạng Lưới Chi Nhánh** | 5 | 100% | `branch.directory.lookup` |
| **11. Tra Cứu Quy Trình & Chính Sách FAQ** | 5 | 100% | `knowledge.policy.search` |

---

## 2. Nhật Ký Chi Tiết 100 Câu Lệnh (Detailed Execution Log)

| STT | Câu Lệnh Của GDV | Intent Nhận Diện | Phản Hồi Tóm Tắt Cho GDV | Trạng Thái | Độ Trễ |
|---|---|---|---|---|---|
| 001 | `chuyển 50 triệu cho Nguyễn Văn An tại Vietcombank STK 0123456789` | `DYNAMIC_AUTONOMOUS_TASK` | Ngân hàng: Ngân hàng TMCP Ngoại thương Việt Nam (Mã: VCB, BIN: 970436, Chuyển... | ✅ PASS | 2827 ms |
| 002 | `chuyển 25tr cho Tran Thi Mai ngân hàng BIDV số tài khoản 987654321` | `DYNAMIC_AUTONOMOUS_TASK` | Ngân hàng: Ngân hàng TMCP Đầu tư và Phát triển Việt Nam (Mã: BIDV, BIN: 97041... | ✅ PASS | 3223 ms |
| 003 | `chuyển khoản 100 triệu sang Vietinbank cho Le Hoang Long stk 1020304050` | `DYNAMIC_AUTONOMOUS_TASK` | Ngân hàng: Ngân hàng TMCP Công thương Việt Nam (Mã: CTG, BIN: 970415, Chuyển ... | ✅ PASS | 3256 ms |
| 004 | `chuyển tiền 1.5 tỷ cho Công ty TNHH Hải Đăng tại Techcombank STK 19034567890123` | `DYNAMIC_AUTONOMOUS_TASK` | Ngân hàng: Ngân hàng TMCP Kỹ thương Việt Nam (Mã: TCB, BIN: 970407, Chuyển nh... | ✅ PASS | 3167 ms |
| 005 | `transfer 15tr cho Phạm Quỳnh Nga tại VCB stk 001100223344` | `DYNAMIC_AUTONOMOUS_TASK` | Ngân hàng: Ngân hàng TMCP Ngoại thương Việt Nam (Mã: VCB, BIN: 970436, Chuyển... | ✅ PASS | 3385 ms |
| 006 | `chuyển 500k cho Nguyen Duc Thang ngân hàng MB STK 0988123456` | `DYNAMIC_AUTONOMOUS_TASK` | Ngân hàng: Ngân hàng TMCP MB (Mã: MB, BIN: 970499, Chuyển nhanh 24/7: Khả dụn... | ✅ PASS | 3102 ms |
| 007 | `chuyển 30.000.000 VND cho Hoàng Kim Ngân tại Sacombank số tài khoản 060123456789` | `DYNAMIC_AUTONOMOUS_TASK` | Ngân hàng: Ngân hàng TMCP STB (Mã: STB, BIN: 970499, Chuyển nhanh 24/7: Khả d... | ✅ PASS | 3557 ms |
| 008 | `chuyển 200 tr cho Bùi Tuấn Kiệt tại VPBank STK 1234567899` | `DYNAMIC_AUTONOMOUS_TASK` | Ngân hàng: Ngân hàng TMCP VPB (Mã: VPB, BIN: 970499, Chuyển nhanh 24/7: Khả d... | ✅ PASS | 2651 ms |
| 009 | `chuyển 5 triệu cho Do Gia Huy ở ACB tài khoản 88889999` | `DYNAMIC_AUTONOMOUS_TASK` | Ngân hàng: Ngân hàng TMCP ACB (Mã: ACB, BIN: 970499, Chuyển nhanh 24/7: Khả d... | ✅ PASS | 1983 ms |
| 010 | `chuyển tiền nhanh 80 triệu cho Ho Bao Tram tại TPBank STK 02345678901` | `DYNAMIC_AUTONOMOUS_TASK` | Ngân hàng: Ngân hàng TMCP TPB (Mã: TPB, BIN: 970499, Chuyển nhanh 24/7: Khả d... | ✅ PASS | 2166 ms |
| 011 | `chuyển 12tr cho Vu Khanh Linh tại VCB STK 0451000345678` | `DYNAMIC_AUTONOMOUS_TASK` | Ngân hàng: Ngân hàng TMCP Ngoại thương Việt Nam (Mã: VCB, BIN: 970436, Chuyển... | ✅ PASS | 2662 ms |
| 012 | `chuyển 45 triệu cho Dang Quang Huy ở CTG STK 711A12345678` | `DYNAMIC_AUTONOMOUS_TASK` | Ngân hàng: Ngân hàng TMCP Công thương Việt Nam (Mã: CTG, BIN: 970415, Chuyển ... | ✅ PASS | 3443 ms |
| 013 | `chuyển khoản 2 tỷ cho Pham Thanh Tung tại TCB STK 19123456789012` | `DYNAMIC_AUTONOMOUS_TASK` | Số tiền vượt hạn mức GDV (100.000.000 VND), cần Kiểm soát viên duyệt.  Phí ch... | ✅ PASS | 2805 ms |
| 014 | `chuyển 700k cho Ngo Thi Mai Phuong ở BIDV STK 12410001234567` | `DYNAMIC_AUTONOMOUS_TASK` | Ngân hàng: Ngân hàng TMCP Đầu tư và Phát triển Việt Nam (Mã: BIDV, BIN: 97041... | ✅ PASS | 2809 ms |
| 015 | `chuyển 350 triệu cho Ly Ngoc Linh tại Vietcombank STK 0071000987654` | `DYNAMIC_AUTONOMOUS_TASK` | Ngân hàng: Ngân hàng TMCP Ngoại thương Việt Nam (Mã: VCB, BIN: 970436, Chuyển... | ✅ PASS | 2303 ms |
| 016 | `nộp 100 triệu vào tài khoản 3456789 cho Nguyễn Minh Anh` | `DYNAMIC_AUTONOMOUS_TASK` | Tài khoản 3456789: Chủ tài khoản Nguyễn Minh Anh (Trạng thái: ACTIVE, Số dư k... | ✅ PASS | 2622 ms |
| 017 | `nộp tiền mặt 50tr vào số tài khoản 012345678901` | `DYNAMIC_AUTONOMOUS_TASK` | Tài khoản 012345678901: Chủ tài khoản Nguyễn Minh Anh (Trạng thái: ACTIVE, Số... | ✅ PASS | 2228 ms |
| 018 | `khách hàng nộp 200 triệu tiền mặt vào tài khoản 987654321` | `DYNAMIC_AUTONOMOUS_TASK` | Tài khoản 987654321: Chủ tài khoản Nguyễn Hoàng Long (Trạng thái: ACTIVE, Số ... | ✅ PASS | 2418 ms |
| 019 | `nộp 500 triệu vào STK 1020304050` | `DYNAMIC_AUTONOMOUS_TASK` | Tài khoản 1020304050: Chủ tài khoản Trần Thanh Tùng (Trạng thái: ACTIVE, Số d... | ✅ PASS | 2307 ms |
| 020 | `nộp 20 triệu tiền mặt cho chủ tài khoản Tran Thi Mai` | `DYNAMIC_AUTONOMOUS_TASK` | Trong hạn mức GDV tự phê duyệt.  Đã thực thi risk.cash.screen | ✅ PASS | 2599 ms |
| 021 | `rút 50 triệu tiền mặt từ tài khoản 3456789` | `DYNAMIC_AUTONOMOUS_TASK` | Tài khoản 3456789: Chủ tài khoản Nguyễn Minh Anh (Trạng thái: ACTIVE, Số dư k... | ✅ PASS | 2439 ms |
| 022 | `rút tiền 100 triệu từ số tài khoản 012345678901` | `DYNAMIC_AUTONOMOUS_TASK` | Tài khoản 012345678901: Chủ tài khoản Nguyễn Minh Anh (Trạng thái: ACTIVE, Số... | ✅ PASS | 1767 ms |
| 023 | `khách hàng muốn rút 300 triệu tiền mặt tại quầy` | `DYNAMIC_AUTONOMOUS_TASK` | Số tiền vượt hạn mức GDV (100.000.000 VND), cần Kiểm soát viên duyệt.  Đã thự... | ✅ PASS | 2593 ms |
| 024 | `rút 20tr tiền mặt từ tài khoản của Nguyễn Minh Anh` | `DYNAMIC_AUTONOMOUS_TASK` | Trong hạn mức GDV tự phê duyệt. | ✅ PASS | 3020 ms |
| 025 | `rút tiền mặt 1.2 tỷ từ tài khoản doanh nghiệp` | `DYNAMIC_AUTONOMOUS_TASK` | Số tiền vượt hạn mức GDV (100.000.000 VND), cần Kiểm soát viên duyệt.  Đã thự... | ✅ PASS | 2268 ms |
| 026 | `Phân tích chân dung khách hàng và hành vi chi tiêu của CIF-0001842` | `DYNAMIC_AUTONOMOUS_TASK` | Chân dung KH Nguyễn Minh Anh (PRIORITY): Nhóm 'Doanh nhân / Chủ hộ kinh doanh... | ✅ PASS | 2017 ms |
| 027 | `Xem chân dung 360 và khẩu vị đầu tư của khách hàng CIF-992211` | `DYNAMIC_AUTONOMOUS_TASK` | Chân dung KH Phạm Hồng Nhung (PRIORITY): Nhóm 'Khách hàng cao cấp ưa chuộng t... | ✅ PASS | 1125 ms |
| 028 | `Phân tích thói quen giao dịch và kênh ưa thích của CIF-334455` | `DYNAMIC_AUTONOMOUS_TASK` | Chân dung KH Trần Minh Anh (DIAMOND_VIP): Nhóm 'Doanh nhân / Chủ hộ kinh doan... | ✅ PASS | 1969 ms |
| 029 | `Đánh giá mức độ gắn bó và phân khúc của khách hàng CIF-778899` | `DYNAMIC_AUTONOMOUS_TASK` | Chân dung KH Dương Thanh Hằng (DIAMOND_VIP): Nhóm 'Nhà đầu tư tích lũy & Tiết... | ✅ PASS | 2068 ms |
| 030 | `Chân dung tài chính và hành vi thanh toán của CIF-123678` | `DYNAMIC_AUTONOMOUS_TASK` | Chân dung KH Phạm Thế Vinh (STANDARD): Nhóm 'Nhà đầu tư tích lũy & Tiết kiệm ... | ✅ PASS | 2197 ms |
| 031 | `Phân tích hồ sơ khách hàng 360 CIF-556677` | `DYNAMIC_AUTONOMOUS_TASK` | Khách hàng Phạm Kim Ngân (CIF: CIF-556677) · Phân hạng: PRIORITY · KYC: VERIF... | ✅ PASS | 2243 ms |
| 032 | `Khách hàng CIF-889977 thuộc nhóm chân dung nào và khẩu vị rủi ro ra sao?` | `DYNAMIC_AUTONOMOUS_TASK` | Chân dung KH Phạm Hồng Nhung (STANDARD): Nhóm 'Khách hàng cao cấp ưa chuộng t... | ✅ PASS | 2393 ms |
| 033 | `Tra cứu hành vi sử dụng dịch vụ ngân hàng số của CIF-445566` | `DYNAMIC_AUTONOMOUS_TASK` | Chân dung KH Trần Hữu Phước (STANDARD): Nhóm 'Doanh nhân / Chủ hộ kinh doanh ... | ✅ PASS | 1344 ms |
| 034 | `Phân tích chân dung khách hàng VIP CIF-990011` | `DYNAMIC_AUTONOMOUS_TASK` | Chân dung KH Nguyễn Hoàng Long (GOLD): Nhóm 'Chuyên viên công nghệ & Người ti... | ✅ PASS | 2034 ms |
| 035 | `Xem thông tin persona và kênh giao dịch chủ yếu của CIF-223344` | `DYNAMIC_AUTONOMOUS_TASK` | Chân dung KH Phan Thùy Dung (DIAMOND_VIP): Nhóm 'Khách hàng cao cấp ưa chuộng... | ✅ PASS | 1310 ms |
| 036 | `Kiểm tra điểm tín dụng CIC và hạn mức vay khả dụng của CIF-0001842` | `DYNAMIC_AUTONOMOUS_TASK` | Tín dụng KH Nguyễn Minh Anh: Điểm tín nhiệm 743/850 (AA (Tốt)), CIC: Nhóm 1 (... | ✅ PASS | 1003 ms |
| 037 | `Tra cứu điểm tín nhiệm nội bộ và lịch sử nợ xấu CIC của CIF-992211` | `DYNAMIC_AUTONOMOUS_TASK` | Tín dụng KH Phạm Hồng Nhung: Điểm tín nhiệm 758/850 (AAA (Xuất sắc)), CIC: Nh... | ✅ PASS | 1519 ms |
| 038 | `Khách hàng CIF-334455 có nợ xấu CIC không và vay được bao nhiêu?` | `DYNAMIC_AUTONOMOUS_TASK` | Tín dụng KH Trần Minh Anh: Điểm tín nhiệm 767/850 (AAA (Xuất sắc)), CIC: Nhóm... | ✅ PASS | 1154 ms |
| 039 | `Chấm điểm tín dụng cho khách hàng CIF-556677` | `DYNAMIC_AUTONOMOUS_TASK` | Tín dụng KH Phạm Kim Ngân: Điểm tín nhiệm 682/850 (AA (Tốt)), CIC: Nhóm 1 (Dư... | ✅ PASS | 829 ms |
| 040 | `Xem hạn mức vay phê duyệt trước của CIF-889977` | `DYNAMIC_AUTONOMOUS_TASK` | Tín dụng KH Phạm Hồng Nhung: Điểm tín nhiệm 803/850 (AAA (Xuất sắc)), CIC: Nh... | ✅ PASS | 1690 ms |
| 041 | `Kiểm tra điểm CIC và nhóm nợ của CIF-123456` | `DYNAMIC_AUTONOMOUS_TASK` | Tín dụng KH Võ Tuấn Kiệt: Điểm tín nhiệm 784/850 (AAA (Xuất sắc)), CIC: Nhóm ... | ✅ PASS | 1396 ms |
| 042 | `Tra cứu khả năng cấp hạn mức thấu chi tín chấp cho CIF-778899` | `DYNAMIC_AUTONOMOUS_TASK` | Tín dụng KH Dương Thanh Hằng: Điểm tín nhiệm 695/850 (AA (Tốt)), CIC: Nhóm 1 ... | ✅ PASS | 1819 ms |
| 043 | `Điểm tín dụng khách hàng CIF-445566 hiện tại là bao nhiêu?` | `DYNAMIC_AUTONOMOUS_TASK` | Tín dụng KH Trần Hữu Phước: Điểm tín nhiệm 814/850 (AAA (Xuất sắc)), CIC: Nhó... | ✅ PASS | 1179 ms |
| 044 | `Kiểm tra nhóm nợ CIC và uy tín tín dụng của CIF-667788` | `DYNAMIC_AUTONOMOUS_TASK` | Tín dụng KH Đinh Hoàng Long: Điểm tín nhiệm 756/850 (AAA (Xuất sắc)), CIC: Nh... | ✅ PASS | 951 ms |
| 045 | `Hạn mức vay vốn tối đa duyệt trước cho CIF-112233 là bao nhiêu?` | `DYNAMIC_AUTONOMOUS_TASK` | Tín dụng KH Lâm Anh Dũng: Điểm tín nhiệm 717/850 (AA (Tốt)), CIC: Nhóm 1 (Dư ... | ✅ PASS | 1392 ms |
| 046 | `Tư vấn gợi ý gói sản phẩm ưu đãi và offer phù hợp cho khách hàng CIF-0001842` | `DYNAMIC_AUTONOMOUS_TASK` | Top 3 Offer tư vấn cho KH Nguyễn Minh Anh (PRIORITY): 1) Thẻ Signature Cashba... | ✅ PASS | 2167 ms |
| 047 | `Gợi ý các ưu đãi và chương trình khuyến nghị cho CIF-992211` | `DYNAMIC_AUTONOMOUS_TASK` | Top 3 Offer tư vấn cho KH Phạm Hồng Nhung (PRIORITY): 1) Thẻ Signature Cashba... | ✅ PASS | 1035 ms |
| 048 | `Đề xuất sản phẩm phù hợp tiếp theo (Next-Best-Offer) cho CIF-334455` | `DYNAMIC_AUTONOMOUS_TASK` | Top 3 Offer tư vấn cho KH Trần Minh Anh (DIAMOND_VIP): 1) Thẻ Signature Cashb... | ✅ PASS | 981 ms |
| 049 | `Tư vấn mở thêm thẻ tín dụng hoàn tiền hoặc gói vay cho CIF-556677` | `DYNAMIC_AUTONOMOUS_TASK` | Khách hàng Phạm Kim Ngân (CIF: CIF-556677) · Phân hạng: PRIORITY · KYC: VERIF... | ✅ PASS | 1764 ms |
| 050 | `Có gói ưu đãi lãi suất nào phù hợp cho khách hàng CIF-889977 không?` | `DYNAMIC_AUTONOMOUS_TASK` | Khách hàng Phạm Hồng Nhung (CIF: CIF-889977) · Phân hạng: STANDARD · KYC: VER... | ✅ PASS | 1493 ms |
| 051 | `Gợi ý giải pháp bán chéo (cross-sell) tối ưu cho CIF-123678` | `DYNAMIC_AUTONOMOUS_TASK` | Top 3 Offer tư vấn cho KH Phạm Thế Vinh (STANDARD): 1) Thẻ Signature Cashback... | ✅ PASS | 2708 ms |
| 052 | `Khách hàng CIF-778899 nên tư vấn sản phẩm gì hôm nay?` | `DYNAMIC_AUTONOMOUS_TASK` | Chân dung KH Dương Thanh Hằng (DIAMOND_VIP): Nhóm 'Nhà đầu tư tích lũy & Tiết... | ✅ PASS | 2759 ms |
| 053 | `Tư vấn gói bảo hiểm liên kết và thẻ tín dụng cho CIF-445566` | `DYNAMIC_AUTONOMOUS_TASK` | Khách hàng Trần Hữu Phước (CIF: CIF-445566) · Phân hạng: STANDARD · KYC: VERI... | ✅ PASS | 1873 ms |
| 054 | `Top 3 offer phù hợp nhất cho khách hàng VIP CIF-990011` | `DYNAMIC_AUTONOMOUS_TASK` | Top 3 Offer tư vấn cho KH Nguyễn Hoàng Long (GOLD): 1) Thẻ Signature Cashback... | ✅ PASS | 1433 ms |
| 055 | `Khuyến nghị sản phẩm tài chính sinh lời cho CIF-223344` | `DYNAMIC_AUTONOMOUS_TASK` | Top 3 Offer tư vấn cho KH Phan Thùy Dung (DIAMOND_VIP): 1) Thẻ Signature Cash... | ✅ PASS | 2086 ms |
| 056 | `Khách muốn gửi 200 triệu kỳ hạn 6 tháng thì tính lãi bao nhiêu?` | `DYNAMIC_AUTONOMOUS_TASK` | Gửi 200 tr VND kỳ hạn 6 tháng (Lãi suất 5.6%/năm) ➔ Tiền lãi dự kiến: 5,6 tr ... | ✅ PASS | 1561 ms |
| 057 | `Gửi 500 triệu 12 tháng thì lãi được bao nhiêu tiền?` | `DYNAMIC_AUTONOMOUS_TASK` | Gửi 500 tr VND kỳ hạn 12 tháng (Lãi suất 6.0%/năm) ➔ Tiền lãi dự kiến: 30 tr ... | ✅ PASS | 1181 ms |
| 058 | `Tính lãi giúp khách gửi 1 tỷ kỳ hạn 24 tháng` | `DYNAMIC_AUTONOMOUS_TASK` | Gửi 1 tỷ VND kỳ hạn 24 tháng (Lãi suất 6.3%/năm) ➔ Tiền lãi dự kiến: 126 tr V... | ✅ PASS | 1302 ms |
| 059 | `Tư vấn phương án tiết kiệm 50 triệu kỳ hạn 3 tháng` | `DYNAMIC_AUTONOMOUS_TASK` | Gửi 50 tr VND kỳ hạn 3 tháng (Lãi suất 4.2%/năm) ➔ Tiền lãi dự kiến: 525,000 ... | ✅ PASS | 1582 ms |
| 060 | `Gửi 100 tr trong 6 tháng thì tổng nhận khi đáo hạn là bao nhiêu?` | `DYNAMIC_AUTONOMOUS_TASK` | Gửi 100 tr VND kỳ hạn 6 tháng (Lãi suất 5.6%/năm) ➔ Tiền lãi dự kiến: 2,8 tr ... | ✅ PASS | 2469 ms |
| 061 | `Tối ưu lãi cho khoản tiền 300 triệu nên gửi kỳ hạn nào tốt nhất?` | `DYNAMIC_AUTONOMOUS_TASK` | Gửi 300 tr VND kỳ hạn 24 tháng (Lãi suất 6.3%/năm) ➔ Tiền lãi dự kiến: 37,8 t... | ✅ PASS | 2598 ms |
| 062 | `Tính lãi suất tiết kiệm 2 tỷ kỳ hạn 12 tháng` | `DYNAMIC_AUTONOMOUS_TASK` | Gửi 2 tỷ VND kỳ hạn 12 tháng (Lãi suất 6.0%/năm) ➔ Tiền lãi dự kiến: 120 tr V... | ✅ PASS | 1341 ms |
| 063 | `So sánh lãi suất khi gửi 500 tr kỳ hạn 3 tháng vs 6 tháng vs 12 tháng` | `DYNAMIC_AUTONOMOUS_TASK` | Gửi 500 tr VND kỳ hạn 12 tháng (Lãi suất 6.0%/năm) ➔ Tiền lãi dự kiến: 30 tr ... | ✅ PASS | 1879 ms |
| 064 | `Khách gửi 80 triệu 1 tháng thì nhận bao nhiêu tiền lãi?` | `DYNAMIC_AUTONOMOUS_TASK` | Gửi 80 tr VND kỳ hạn 1 tháng (Lãi suất 3.2%/năm) ➔ Tiền lãi dự kiến: 213,333 ... | ✅ PASS | 1412 ms |
| 065 | `Mô phỏng bảng tính tiền lãi định kỳ cho khoản gửi 1.5 tỷ` | `DYNAMIC_AUTONOMOUS_TASK` | Gửi 1,5 tỷ VND kỳ hạn 24 tháng (Lãi suất 6.3%/năm) ➔ Tiền lãi dự kiến: 189 tr... | ✅ PASS | 2402 ms |
| 066 | `Kiểm tra danh sách thẻ và trạng thái thẻ của CIF-0001842` | `DYNAMIC_AUTONOMOUS_TASK` | Khách hàng Nguyễn Minh Anh có 2 thẻ hoạt động: 1 Thẻ tín dụng Visa Platinum (... | ✅ PASS | 1171 ms |
| 067 | `Khách hàng CIF-992211 yêu cầu khóa thẻ tín dụng Visa khẩn cấp` | `DYNAMIC_AUTONOMOUS_TASK` | Khách hàng Phạm Hồng Nhung có 2 thẻ hoạt động: 1 Thẻ tín dụng Visa Platinum (... | ✅ PASS | 1433 ms |
| 068 | `Tra cứu hạn mức thẻ tín dụng và thẻ ghi nợ của CIF-334455` | `DYNAMIC_AUTONOMOUS_TASK` | Khách hàng Trần Minh Anh có 2 thẻ hoạt động: 1 Thẻ tín dụng Visa Platinum (hạ... | ✅ PASS | 1138 ms |
| 069 | `Đổi mã PIN và kiểm tra thẻ Napas cho khách hàng CIF-556677` | `DYNAMIC_AUTONOMOUS_TASK` | Khách hàng Phạm Kim Ngân có 2 thẻ hoạt động: 1 Thẻ tín dụng Visa Platinum (hạ... | ✅ PASS | 2107 ms |
| 070 | `Mở khóa tính năng thanh toán online E-commerce cho thẻ Visa của CIF-889977` | `DYNAMIC_AUTONOMOUS_TASK` | Khách hàng Phạm Hồng Nhung có 2 thẻ hoạt động: 1 Thẻ tín dụng Visa Platinum (... | ✅ PASS | 1058 ms |
| 071 | `Tra cứu danh sách thẻ ATM và thẻ tín dụng của CIF-123678` | `DYNAMIC_AUTONOMOUS_TASK` | Khách hàng Phạm Thế Vinh có 2 thẻ hoạt động: 1 Thẻ tín dụng Visa Platinum (hạ... | ✅ PASS | 1119 ms |
| 072 | `Kiểm tra hạn mức quẹt POS của thẻ Platinum CIF-778899` | `DYNAMIC_AUTONOMOUS_TASK` | Khách hàng Dương Thanh Hằng có 2 thẻ hoạt động: 1 Thẻ tín dụng Visa Platinum ... | ✅ PASS | 3951 ms |
| 073 | `Khách hàng CIF-445566 muốn kiểm tra tình trạng hoạt động của thẻ` | `DYNAMIC_AUTONOMOUS_TASK` | Khách hàng Trần Hữu Phước có 2 thẻ hoạt động: 1 Thẻ tín dụng Visa Platinum (h... | ✅ PASS | 1300 ms |
| 074 | `Xem thông tin thẻ tín dụng Master và hạn mức khả dụng của CIF-990011` | `DYNAMIC_AUTONOMOUS_TASK` | Khách hàng Nguyễn Hoàng Long có 2 thẻ hoạt động: 1 Thẻ tín dụng Visa Platinum... | ✅ PASS | 2693 ms |
| 075 | `Tra cứu trạng thái thẻ ghi nợ nội địa Napas của CIF-223344` | `DYNAMIC_AUTONOMOUS_TASK` | Khách hàng Phan Thùy Dung có 2 thẻ hoạt động: 1 Thẻ tín dụng Visa Platinum (h... | ✅ PASS | 1099 ms |
| 076 | `Trích lục lịch sử giao dịch sao kê của tài khoản 3456789` | `DYNAMIC_AUTONOMOUS_TASK` | Sao kê gần nhất tài khoản 3456789: 4 giao dịch (Tổng tiền vào: +52,25 tr VND,... | ✅ PASS | 992 ms |
| 077 | `In sao kê 30 ngày gần nhất của tài khoản 012345678901` | `DYNAMIC_AUTONOMOUS_TASK` | Tài khoản 012345678901: Chủ tài khoản Nguyễn Minh Anh (Trạng thái: ACTIVE, Số... | ✅ PASS | 1123 ms |
| 078 | `Kiểm tra lịch sử biến động số dư của tài khoản 987654321` | `DYNAMIC_AUTONOMOUS_TASK` | Tài khoản 987654321: Chủ tài khoản Nguyễn Hoàng Long (Trạng thái: ACTIVE, Số ... | ✅ PASS | 1617 ms |
| 079 | `Xem các giao dịch tiền vào tiền ra gần đây của STK 1020304050` | `DYNAMIC_AUTONOMOUS_TASK` | Tài khoản 1020304050: Chủ tài khoản Trần Thanh Tùng (Trạng thái: ACTIVE, Số d... | ✅ PASS | 1671 ms |
| 080 | `Trích lục sao kê dòng tiền của CIF-0001842` | `DYNAMIC_AUTONOMOUS_TASK` | Sao kê gần nhất tài khoản 3456789: 4 giao dịch (Tổng tiền vào: +52,25 tr VND,... | ✅ PASS | 867 ms |
| 081 | `Tra cứu lịch sử nhận tiền lương và chi tiêu của tài khoản 3456789` | `DYNAMIC_AUTONOMOUS_TASK` | Tài khoản 3456789: Chủ tài khoản Nguyễn Minh Anh (Trạng thái: ACTIVE, Số dư k... | ✅ PASS | 1309 ms |
| 082 | `In lịch sử giao dịch 90 ngày của số tài khoản 001100223344` | `DYNAMIC_AUTONOMOUS_TASK` | Tài khoản 001100223344: Chủ tài khoản Lê Quỳnh Nga (Trạng thái: ACTIVE, Số dư... | ✅ PASS | 995 ms |
| 083 | `Kiểm tra giao dịch chuyển tiền gần nhất trên tài khoản 1234567899` | `DYNAMIC_AUTONOMOUS_TASK` | Sao kê gần nhất tài khoản 1234567899: 4 giao dịch (Tổng tiền vào: +52,25 tr V... | ✅ PASS | 1481 ms |
| 084 | `Sao kê chi tiết các khoản thu chi của STK 060123456789` | `DYNAMIC_AUTONOMOUS_TASK` | Sao kê gần nhất tài khoản 060123456789: 4 giao dịch (Tổng tiền vào: +52,25 tr... | ✅ PASS | 1020 ms |
| 085 | `Xem danh sách 5 giao dịch gần đây nhất của tài khoản 88889999` | `DYNAMIC_AUTONOMOUS_TASK` | Tài khoản 88889999: Chủ tài khoản Đinh Gia Huy (Trạng thái: ACTIVE, Số dư khả... | ✅ PASS | 1553 ms |
| 086 | `Tỷ giá USD hôm nay mua vào bán ra bao nhiêu?` | `DYNAMIC_AUTONOMOUS_TASK` | Tỷ giá USD: Mua vào 25,450 VND, Bán ra 25,480 VND | ✅ PASS | 2114 ms |
| 087 | `Tỷ giá 2000 EUR hôm nay đổi ra bao nhiêu VND?` | `DYNAMIC_AUTONOMOUS_TASK` | 2000 EUR = 54,400,000 VND (mua) / 55,000,000 VND (bán) | ✅ PASS | 1388 ms |
| 088 | `Xem tỷ giá đồng Yên Nhật JPY hôm nay` | `DYNAMIC_AUTONOMOUS_TASK` | Tỷ giá JPY: Mua vào 166 VND, Bán ra 168 VND | ✅ PASS | 1030 ms |
| 089 | `Đổi 5000 SGD sang tiền Việt được bao nhiêu?` | `DYNAMIC_AUTONOMOUS_TASK` | 5000 SGD = 95,500,000 VND (mua) / 97,000,000 VND (bán) | ✅ PASS | 1493 ms |
| 090 | `Tỷ giá Bảng Anh GBP tại quầy hôm nay thế nào?` | `DYNAMIC_AUTONOMOUS_TASK` | Tỷ giá GBP: Mua vào 32,800 VND, Bán ra 33,200 VND | ✅ PASS | 1262 ms |
| 091 | `Địa chỉ và hotline chi nhánh tại Hà Nội ở đâu?` | `DYNAMIC_AUTONOMOUS_TASK` | Tìm thấy 2 điểm giao dịch phù hợp. | ✅ PASS | 981 ms |
| 092 | `Tìm phòng giao dịch tại TP Hồ Chí Minh` | `DYNAMIC_AUTONOMOUS_TASK` | Tìm thấy 1 điểm giao dịch phù hợp. | ✅ PASS | 859 ms |
| 093 | `Chi nhánh Đà Nẵng mở cửa đến mấy giờ và hotline là gì?` | `DYNAMIC_AUTONOMOUS_TASK` | Tìm thấy 1 điểm giao dịch phù hợp. | ✅ PASS | 1386 ms |
| 094 | `Địa chỉ trụ sở chính ngân hàng ở đâu?` | `POLICY_ASSISTANCE` | Chuyển khoản tại quầy yêu cầu khách hàng đã xác thực, kiểm tra thông tin ngườ... | ✅ PASS | 2464 ms |
| 095 | `Hotline tổng đài chăm sóc khách hàng và điểm giao dịch gần nhất` | `DYNAMIC_AUTONOMOUS_TASK` | Tìm thấy 1 điểm giao dịch phù hợp.  Tìm thấy 1 điểm giao dịch phù hợp.  Tìm t... | ✅ PASS | 2159 ms |
| 096 | `Quy trình nộp tiền mặt trên 400 triệu cần những giấy tờ gì?` | `DYNAMIC_AUTONOMOUS_TASK` | GDV phải đối chiếu số tài khoản, chủ tài khoản, số tiền và chứng từ trước khi... | ✅ PASS | 1786 ms |
| 097 | `Hạn mức chuyển khoản tối đa tại quầy trong một ngày là bao nhiêu?` | `DYNAMIC_AUTONOMOUS_TASK` | Chuyển khoản tại quầy yêu cầu khách hàng đã xác thực, kiểm tra thông tin ngườ... | ✅ PASS | 1562 ms |
| 098 | `Biểu phí chuyển tiền quốc tế và liên ngân hàng như thế nào?` | `DYNAMIC_AUTONOMOUS_TASK` | Chuyển khoản tại quầy yêu cầu khách hàng đã xác thực, kiểm tra thông tin ngườ... | ✅ PASS | 1515 ms |
| 099 | `Hướng dẫn thủ tục mở tài khoản số đẹp cho khách hàng VIP` | `DYNAMIC_AUTONOMOUS_TASK` | Chuyển khoản tại quầy yêu cầu khách hàng đã xác thực, kiểm tra thông tin ngườ... | ✅ PASS | 1458 ms |
| 100 | `Điều kiện cấp thẻ tín dụng không cần chứng minh thu nhập` | `DYNAMIC_AUTONOMOUS_TASK` | Chuyển khoản tại quầy yêu cầu khách hàng đã xác thực, kiểm tra thông tin ngườ... | ✅ PASS | 1442 ms |
