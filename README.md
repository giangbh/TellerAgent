# B.Smart Teller Copilot (Java Spring Boot + MCP Layer + React TypeScript + DeepSeek AI)

Hệ thống Trợ lý AI Quầy Giao Dịch Ngân Hàng (**Smart Counter AI Copilot**) áp dụng nguyên lý **Tự chủ có kiểm soát (Bounded Autonomy)**, chuẩn giao thức **Model Context Protocol (MCP)** qua JSON-RPC 2.0, **Temporal-style Durable Workflow Engine**, **DeepSeek AI Function Calling & Reasoning Engine**, **8 Modular Banking Domain Services** và cơ sở dữ liệu **SQLite Bền Vững**.

---

## 🏛️ Các Thành Phần Kiến Trúc Cốt Lõi

1. **Backend & Tầng MCP (Java Spring Boot 3.4+ / Java 21)**:
   - Triển khai đầy đủ chuẩn **Model Context Protocol (JSON-RPC 2.0)**: `initialize`, `ping`, `tools/list`, `tools/call`, `resources/list`, `resources/read`.
   - **Hệ sinh thái 23 Banking Capabilities chuẩn hóa**:
     - *CIF & Portfolio:* `customer.profile.read`, `customer.accounts.list`, `customer.accounts.summary`.
     - *Phân tích Chân dung & Tín dụng:* `customer.persona.analytics`, `customer.credit.score.check`, `recommendation.nbo.products`.
     - *Sao kê & Tiền gửi:* `statement.transaction.history` (10-30 giao dịch ngẫu nhiên), `savings.product.advisor`.
     - *Dịch vụ Thẻ & Tiện ích:* `card.service.manage`.
     - *Tỷ giá & Tra cứu:* `fx.rate.lookup`, `branch.directory.lookup`, `bank.directory.lookup`, `pricing.transfer.fee`, `knowledge.policy.search`.
     - *Kiểm soát Hạn mức & AML:* `transfer.limit.check`, `cash.limit.check`, `risk.transfer.screen`, `risk.cash.screen`, `transaction.draft.validate`, `cash.draft.validate`, `account.resolve.by_number`.
     - *Hạch toán Tài chính (Financial Write):* `core.transfer.execute`, `core.cash.execute` (Enforce Idempotency Key & kiểm soát qua Internal API Gateway).

2. **Hệ Thống Dữ Liệu Mock Động 8 Phân Hệ (Decoupled Domain Services)**:
   - Tách biệt kiến trúc `MockBankingDataService` thành 8 Domain Services chuyên biệt:
     - `MockCustomerProfileService`: Định danh, KYC, phân khúc Standard / Gold / Priority / Diamond VIP.
     - `MockAccountService`: Tài khoản thanh toán, sổ tiết kiệm, tổng hợp tài sản AUM.
     - `MockTransactionStatementService`: Sinh 10-30 giao dịch sao kê ngẫu nhiên thực tế (lương, kiều hối, bảo hiểm, QR POS...).
     - `MockCustomerPersonaService`: Chân dung 360, khẩu vị đầu tư, thâm niên gắn bó, tần suất giao dịch hàng tháng.
     - `MockCreditScoreService`: Chấm điểm tín dụng CIC (680-820), xếp hạng AAA/AA/A, hạn mức vay/thấu chi cấp trước.
     - `MockNextBestOfferService`: Đề xuất sản phẩm ưu đãi cá nhân hóa (NBO).
     - `MockSavingsAdvisorService`: Cố vấn tối ưu lãi suất tiền gửi và mô phỏng tiền lãi đáo hạn.
     - `MockCardService`: Quản trị thẻ ATM, Debit Napas, Credit Visa Platinum / World Mastercard.
     - `MockMarketDirectoryService`: Tỷ giá 10 ngoại tệ & mạng lưới chi nhánh toàn quốc.

3. **Hybrid Reasoning Engine (DeepSeek AI + ReAct Autonomous Planner + Rule-Based Engine)**:
   - **DeepSeek AI Function Calling**: Tự động bóc tách thực thể, phân tích ý định và lập kế hoạch ReAct nhiều bước.
   - **Reasoning Synthesis**: LLM tự động đọc dữ liệu thô (Raw JSON) từ MCP Tools và tự tính toán số liệu (tổng dòng tiền vào/ra, giao dịch lớn nhất, phân tích chi tiêu...).
   - **Rule-Based Engine**: Dự phòng 100% thời gian thực khi không có API Key hoặc mất kết nối mạng.

4. **Giao Diện Thinking Process UI (DeepSeek-R1 Style)**:
   - Trực quan hóa toàn bộ tiến trình suy nghĩ của AI: `🧠 Phân tích ngữ cảnh`, `🔍 Bóc tách thực thể`, `📋 Lập kế hoạch ReAct`, `⚡ Gọi MCP Tool`, `🛡️ Kiểm tra an toàn & RBAC`, `✨ Tổng hợp phản hồi nghiệp vụ`.
   - Card hiển thị dạng accordion thu gọn/mở rộng, kèm thời gian suy nghĩ (`Đã suy nghĩ trong Xs/ms`).

5. **Temporal-style Durable Workflow Engine & Internal API Gateway**:
   - Quản lý State Machine theo mô hình Workflow as Code, cơ chế Maker-Checker 4 mắt và Event Sourcing bất biến.
   - Cấp mã `traceId` duy nhất cho mỗi giao dịch và duy trì sổ cái chống trùng lặp (**Idempotency Ledger**) trên SQLite.

6. **Frontend Smart Counter Hub (React 19 + TypeScript + Vite)**:
   - Giao diện Dark Mode & Glassmorphism chuyên nghiệp, hiển thị Live Draft nghiệp vụ, Audit Timeline và tích hợp **MCP Explorer Modal**.

---

## 📚 Danh Mục Tài Liệu Kỹ Thuật

* **[Tài liệu Kiến trúc Hệ thống: docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**
* **[Tài liệu Kỹ thuật Chi tiết: docs/TECHNICAL_SPECIFICATION.md](docs/TECHNICAL_SPECIFICATION.md)**
* **[Hệ sinh thái 23 Công cụ MCP: docs/MCP_ECOSYSTEM.md](docs/MCP_ECOSYSTEM.md)**
* **[Báo cáo Benchmark 100 Test Cases: docs/deepseek_100_testcase_report.md](docs/deepseek_100_testcase_report.md)**

---

## 🚀 Hướng Dẫn Khởi Chạy & Cấu Hình

### 1. Cấu Hình DeepSeek API (Tùy chọn)
Điền API Key vào file `src/main/resources/application.properties` (hoặc biến môi trường `DEEPSEEK_API_KEY`):
```properties
deepseek.api.key=sk-your-deepseek-api-key
deepseek.api.base-url=https://api.deepseek.com/v1
deepseek.api.model=deepseek-chat
deepseek.api.enabled=true
```
*(Lưu ý: Hệ thống có cơ chế Fallback Rule-Based tự động nếu không cấu hình API Key).*

### 2. Build Frontend (React + Vite)
```bash
cd frontend
npm install
npm run build
cd ..
```

### 3. Chạy Toàn Bộ Bộ Test Tự Động (JUnit 5 Suite)
```bash
mvn test
```

### 4. Khởi Động Ứng Dụng
```bash
mvn spring-boot:run
```

Truy cập ứng dụng tại: **[http://127.0.0.1:8799](http://127.0.0.1:8799)**

---

## 🛠️ Công Nghệ Sử Dụng
* **Backend:** Java 21, Spring Boot 3.4.3, Spring Data JDBC, SQLite-JDBC, Jackson Databind, JUnit 5, Mockito.
* **AI & LLM:** DeepSeek-V3 / DeepSeek-R1 API, OpenAI Function Calling standard, ReAct Autonomous Loop.
* **Frontend:** React 19, TypeScript, Vite, Lucide Icons, Vanilla CSS Design System (Glassmorphism + Dark Mode).
* **Giao thức:** Model Context Protocol (MCP - JSON-RPC 2.0), RESTful HTTP API.
* **Lưu trữ:** SQLite File-based Database (`data/teller_workflows.db`).
