# B.Smart Teller Copilot (Java Spring Boot + MCP Layer + React TypeScript)

Hệ thống Trợ lý AI Quầy Giao Dịch Ngân Hàng (**Smart Counter AI Copilot**) áp dụng nguyên lý **Tự chủ có kiểm soát (Bounded Autonomy)**, chuẩn giao thức **Model Context Protocol (MCP)** qua JSON-RPC 2.0, **Temporal-style Durable Workflow Engine**, **Internal Banking API Gateway** và cơ sở dữ liệu **SQLite Bền Vững**.

---

## 🏛️ Các Thành Phần Kiến Trúc Cốt Lõi

1. **Backend & Tầng MCP (Java Spring Boot 3.4+ / Java 21/23)**:
   - Triển khai đầy đủ chuẩn **Model Context Protocol (JSON-RPC 2.0)**: `initialize`, `ping`, `tools/list`, `tools/call`, `resources/list`, `resources/read`.
   - **Danh mục 17 Banking Capabilities**:
     - *CIF & Portfolio:* `customer.profile.read`, `customer.accounts.list`, `customer.accounts.summary`.
     - *Tỷ giá & Tra cứu:* `fx.rate.lookup`, `branch.directory.lookup`, `bank.directory.lookup`, `pricing.transfer.fee`, `knowledge.policy.search`.
     - *Kiểm soát Hạn mức & AML:* `transfer.limit.check`, `cash.limit.check`, `risk.transfer.screen`, `risk.cash.screen`, `transaction.draft.validate`, `cash.draft.validate`, `account.resolve.by_number`.
     - *Hạch toán Tài chính (Financial Write):* `core.transfer.execute`, `core.cash.execute` (Enforce Idempotency Key & đi qua Internal API Gateway).
2. **Khám Phá Công Cụ Động (Dynamic Tool Discovery & Autonomous ReAct Engine)**:
   - Agent tự động quét danh mục 17 MCP Tools, bóc tách câu hỏi tự do của Teller và tự động ghép nối công cụ thích hợp mà không bị gò bó trong quy trình cố định.
3. **Temporal-style Durable Workflow Engine**:
   - Quản lý State Machine theo mô hình Workflow as Code, tiếp nhận tín hiệu **Signals** cho quy trình Maker-Checker và ghi nhận lịch sử dạng **Event Sourcing**.
4. **Internal Banking API Gateway**:
   - Cấp mã `traceId` duy nhất cho mỗi giao dịch và duy trì sổ cái chống trùng lặp (**Idempotency Ledger**) trên SQLite.
5. **Cơ Sở Dữ Liệu SQLite Bền Vững (`data/teller_workflows.db`)**:
   - Tự động tạo và lưu trữ snapshot phiên làm việc, lịch sử workflow và audit logs bất biến, đảm bảo phục hồi 100% sau sự cố (Crash Recovery).
6. **Frontend Smart Counter Hub (React 19 + TypeScript + Vite)**:
   - Thiết kế Dark Mode & Glassmorphism cao cấp, workspace 3 cột tương tác theo thời gian thực và tích hợp **MCP Explorer Modal**.

---

## 📚 Danh Mục Tài Liệu Kỹ Thuật

* **[Tài liệu Kiến trúc Hệ thống: docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**
* **[Tài liệu Kỹ thuật Chi tiết: docs/TECHNICAL_SPECIFICATION.md](docs/TECHNICAL_SPECIFICATION.md)**
* **[Báo cáo Nghiệm thu & Lịch sử Thực hiện: walkthrough.md](file:///Users/giangbh/.gemini/antigravity-ide/brain/87881fb4-814c-49f0-99ed-73f3d6fa7b03/walkthrough.md)**

---

## 🚀 Hướng Dẫn Khởi Chạy & Kiểm Thử

### 1. Chạy Bộ Test Tự Động (23 Tests PASS 100%)
```bash
mvn clean test
```

### 2. Khởi Động Ứng Dụng
```bash
mvn spring-boot:run
```

Truy cập ứng dụng tại: **[http://127.0.0.1:8799](http://127.0.0.1:8799)**

---

## 🛠️ Công Nghệ Sử Dụng
* **Backend:** Java 21/23, Spring Boot 3.4.3, Spring Data JDBC, SQLite-JDBC, Jackson Databind, JUnit 5.
* **Frontend:** React 19, TypeScript, Vite, Lucide Icons, Vanilla CSS Design System.
* **Giao thức:** Model Context Protocol (JSON-RPC 2.0), RESTful HTTP API.
* **Lưu trữ:** SQLite File-based Database (`data/teller_workflows.db`).
