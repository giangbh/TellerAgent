# B.Smart Teller Copilot — Tài Liệu Kỹ Thuật Chi Tiết (Technical Specification)

---

## 1. Cấu Trúc Module & Mã Nguồn (Codebase Structure)

Dự án được xây dựng trên nền tảng **Java Spring Boot 3.4+** (Java 21/23) kết hợp **Frontend React 19 + TypeScript + Vite**, tích hợp **Temporal-style Workflow Engine**, **Internal Banking API Gateway** và **Cơ sở dữ liệu SQLite cục bộ**:

```text
teller-agent-poc/
├── pom.xml                                   # Quản lý dependencies (Spring Boot, sqlite-jdbc, jackson-jsr310)
├── data/
│   └── teller_workflows.db                   # CSDL SQLite lưu trữ bền vững sessions, workflows, events & audit
├── docs/
│   ├── ARCHITECTURE.md                       # Tài liệu Kiến trúc Hệ thống (System Architecture Document)
│   └── TECHNICAL_SPECIFICATION.md            # Tài liệu Kỹ thuật Chi tiết (Technical Specification)
├── src/main/
│   ├── java/com/dnse/teller/
│   │   ├── TellerApplication.java           # Entrypoint khởi chạy Spring Boot application
│   │   ├── config/
│   │   │   └── SecurityHeadersFilter.java   # HTTP security headers (nosniff, X-Frame-Options: SAMEORIGIN)
│   │   ├── controller/
│   │   │   ├── SessionController.java       # REST API quản lý Session, Tin nhắn, Phê duyệt & Hạch toán
│   │   │   └── McpController.java           # REST API tiếp nhận bản tin JSON-RPC 2.0 giao thức MCP
│   │   ├── gateway/
│   │   │   └── InternalApiGateway.java      # Cổng Gateway nội bộ cấp Trace ID và kiểm soát Idempotency Ledger
│   │   ├── persistence/
│   │   │   ├── DatabaseConfig.java          # Cấu hình DataSource SQLite và tự động tạo Schema DDL
│   │   │   └── WorkflowRepository.java      # Thao tác CSDL (Prepared Statements) cho sessions, workflows, audit
│   │   ├── workflow/
│   │   │   ├── WorkflowExecution.java       # Model đại diện phiên thực thi Temporal workflow
│   │   │   └── WorkflowEngine.java          # Engine điều phối activities, checkpoints, signals và event sourcing
│   │   ├── mcp/
│   │   │   ├── JsonRpcRequest.java          # Model DTO chuẩn JSON-RPC 2.0 Request
│   │   │   ├── JsonRpcResponse.java         # Model DTO chuẩn JSON-RPC 2.0 Response
│   │   │   ├── McpTool.java                 # Interface định nghĩa Capability Tool cho MCP
│   │   │   ├── McpToolRegistry.java         # Quản lý danh mục 17 banking capabilities
│   │   │   ├── McpServer.java               # Engine điều phối MCP, kiểm tra RBAC Policy Guard và thực thi
│   │   │   └── tools/                       # 17 Lớp cài đặt cụ thể từng Capability Tool
│   │   │       ├── CustomerProfileTool.java
│   │   │       ├── CustomerAccountsTool.java
│   │   │       ├── CustomerAccountsSummaryTool.java   # [NEW] Tổng hợp toàn diện tài khoản & tiết kiệm
│   │   │       ├── FxRateLookupTool.java              # [NEW] Tra cứu và quy đổi tỷ giá ngoại tệ live
│   │   │       ├── BranchDirectoryTool.java           # [NEW] Tra cứu mạng lưới chi nhánh & hotline
│   │   │       ├── PolicySearchTool.java
│   │   │       ├── TransferPricingTool.java
│   │   │       ├── BankDirectoryTool.java
│   │   │       ├── TransferLimitCheckTool.java
│   │   │       ├── TransactionDraftValidateTool.java
│   │   │       ├── RiskTransferScreenTool.java
│   │   │       ├── AccountResolveTool.java
│   │   │       ├── CashLimitCheckTool.java
│   │   │       ├── CashDraftValidateTool.java
│   │   │       ├── RiskCashScreenTool.java
│   │   │       ├── CoreTransferExecuteTool.java       # Gọi Core qua InternalApiGateway
│   │   │       └── CoreCashExecuteTool.java           # Gọi Core qua InternalApiGateway
│   │   ├── model/                           # Domain Entities & Value Objects
│   │   │   ├── Session.java                 # Đại diện phiên giao dịch quầy đầy đủ
│   │   │   ├── Intent.java                  # Ý định giao dịch & entities trích xuất
│   │   │   ├── Plan.java                    # Kế hoạch điều phối đa agent (Structured / Dynamic Plan)
│   │   │   ├── TransactionDraft.java        # Biểu mẫu giao dịch tự điền (Live Draft)
│   │   │   ├── RiskAssessment.java          # Đánh giá rủi ro & AML
│   │   │   ├── PolicyFinding.java           # Trích dẫn quy chế & biểu phí
│   │   │   ├── ControlGate.java             # Rào chắn kiểm soát điều kiện posting
│   │   │   ├── Approvals.java               # Chữ ký phê duyệt Maker-Checker
│   │   │   ├── ExecutionResult.java         # Kết quả hạch toán từ Core Banking
│   │   │   ├── AgentRuntime.java            # Thông tin giám sát thời gian chạy của Agents
│   │   │   ├── SessionEvent.java            # Nhật ký sự kiện phiên (Audit Trail)
│   │   │   └── BootstrapResponse.java       # Dữ liệu cấu hình ban đầu cho Client
│   │   └── orchestrator/                    # Tầng điều phối nghiệp vụ
│   │       ├── ReasoningEngine.java         # Dynamic Tool Discovery, Intent Detection & ReAct Planner
│   │       ├── AgentSuite.java              # Điều phối các agent và dynamic_tool_agent qua MCP
│   │       └── TellerOrchestrator.java      # State Machine, Fan-out, Maker-Checker, Gateway & SQLite Store
│   └── resources/
│       ├── application.properties           # Cấu hình cổng 8799 và binding 127.0.0.1
│       └── static/                          # Production bundle của React Frontend (tự động serve)
├── frontend/                                # Ứng dụng React 19 + TypeScript + Vite
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts                       # Cấu hình build vào src/main/resources/static & proxy dev
│   └── src/
│       ├── main.tsx                         # Entrypoint React 19
│       ├── App.tsx                          # Workspace 3 cột Smart Counter Hub
│       ├── index.css                        # Hệ thống giao diện FinTech Dark Mode & Glassmorphism
│       ├── types/teller.ts                  # Type definitions khớp 1:1 với Spring Boot models
│       ├── api/tellerApi.ts                 # HTTP Client gọi REST API và MCP JSON-RPC
│       └── components/
│           ├── Header.tsx                   # Thanh thông tin quầy, khách hàng & nút MCP Explorer
│           ├── ScenariosBar.tsx             # Thanh kích hoạt kịch bản mẫu & tác vụ động (Dynamic)
│           ├── ChatAssistant.tsx            # Trợ lý Copilot hội thoại tự nhiên với GDV
│           ├── PlanViewer.tsx               # Trực quan hóa tiến trình thực thi của các agent
│           ├── LiveDraftForm.tsx            # Form tự điền giao dịch chuyển khoản & tiền mặt
│           ├── ControlGatePanel.tsx         # Bảng Maker-Checker, rủi ro AML & Nút Submit Core
│           └── McpInspectorModal.tsx        # Modal khám phá 17 MCP tools & live JSON-RPC runner
└── src/test/java/com/dnse/teller/           # Toàn bộ 23 Unit & Integration Tests (JUnit 5 & MockMvc)
    ├── McpServerTest.java                   # Kiểm thử giao thức MCP, 17 tools, RBAC, Idempotency (6 tests)
    ├── OrchestratorTest.java                # Kiểm thử State Machine và toàn bộ 6 kịch bản (7 tests)
    ├── DynamicReasoningTest.java            # Kiểm thử Dynamic FX, Branch và Account Summary (3 tests)
    ├── WorkflowEngineTest.java              # Kiểm thử Temporal workflow engine & SQLite persistence (2 tests)
    ├── InternalApiGatewayTest.java          # Kiểm thử Gateway security, Trace ID, Idempotency (2 tests)
    └── SessionControllerTest.java           # Kiểm thử tích hợp REST API (3 tests)
```

---

## 2. Bảng Ma Trận Danh Mục 17 Banking Capabilities & MCP Tools

| STT | Capability ID | Tool Name | Mức độ Rủi ro | Caller được phép | Workflows áp dụng | Idempotency | Mục đích & Mô tả |
| :--- | :--- | :--- | :--- | :--- | :--- | :---: | :--- |
| 1 | `customer.profile.read` | `customer_profile_read` | `SENSITIVE_READ` | `customer_context_agent`, `dynamic_tool_agent` | Tất cả workflows | Không | Đọc hồ sơ CIF khách hàng giới hạn cho phiên tại quầy. |
| 2 | `customer.accounts.list` | `customer_accounts_list` | `SENSITIVE_READ` | `customer_context_agent` | `domestic_transfer`, `cash_withdrawal` | Không | Liệt kê danh sách tài khoản nguồn khả dụng (đã che số). |
| 3 | `customer.accounts.summary` | `customer_accounts_summary` | `SENSITIVE_READ` | `dynamic_tool_agent`, `customer_context_agent` | `dynamic_autonomous`, tất cả | Không | Tổng hợp toàn diện tài khoản thanh toán, số dư và sổ tiết kiệm. |
| 4 | `fx.rate.lookup` | `fx_rate_lookup` | `READ_SAFE` | `dynamic_tool_agent`, `policy_agent` | `dynamic_autonomous`, tất cả | Không | Tra cứu tỷ giá ngoại tệ trực tiếp và tính toán quy đổi tiền tệ. |
| 5 | `branch.directory.lookup` | `branch_directory_lookup` | `READ_SAFE` | `dynamic_tool_agent`, `policy_agent` | `dynamic_autonomous`, tất cả | Không | Tra cứu mạng lưới chi nhánh, phòng giao dịch, địa chỉ và hotline. |
| 6 | `knowledge.policy.search` | `knowledge_policy_search` | `READ_SAFE` | `policy_agent`, `dynamic_tool_agent` | Tất cả workflows | Không | Tra cứu kho tài liệu quy trình nội bộ và trả về trích dẫn quy chế. |
| 7 | `pricing.transfer.fee` | `pricing_transfer_fee` | `READ_SAFE` | `policy_agent`, `dynamic_tool_agent` | `domestic_transfer`, `dynamic_autonomous` | Không | Tính toán mức phí và thuế VAT giao dịch chuyển khoản tại quầy. |
| 8 | `bank.directory.lookup` | `bank_directory_lookup` | `READ_SAFE` | `transaction_draft_agent`, `dynamic_tool_agent` | `domestic_transfer`, `dynamic_autonomous` | Không | Tra cứu mã định danh ngân hàng và tên ngân hàng thụ hưởng. |
| 9 | `transfer.limit.check` | `transfer_limit_check` | `CONTROL_READ` | `transaction_draft_agent`, `dynamic_tool_agent` | `domestic_transfer`, `dynamic_autonomous` | Không | Kiểm tra hạn mức giao dịch của GDV và xác định có cần KSV duyệt không. |
| 10 | `transaction.draft.validate` | `transaction_draft_validate` | `DRAFT` | `transaction_draft_agent` | `domestic_transfer` | Không | Kiểm tra tính đầy đủ và hợp lệ của các trường trong bản nháp chuyển khoản. |
| 11 | `risk.transfer.screen` | `risk_transfer_screen` | `CONTROL_READ` | `risk_assistant` | `domestic_transfer` | Không | Rà soát AML, phát hiện số tiền lớn hoặc tài khoản thụ hưởng cảnh báo. |
| 12 | `account.resolve.by_number` | `account_resolve_by_number` | `SENSITIVE_READ` | `transaction_draft_agent` | `cash_deposit`, `cash_withdrawal` | Không | Đối chiếu số tài khoản tiền mặt và truy xuất tên chủ tài khoản mock. |
| 13 | `cash.limit.check` | `cash_limit_check` | `CONTROL_READ` | `transaction_draft_agent` | `cash_deposit`, `cash_withdrawal` | Không | Kiểm tra hạn mức GDV và đối chiếu số dư khả dụng khi rút tiền mặt. |
| 14 | `cash.draft.validate` | `cash_draft_validate` | `DRAFT` | `transaction_draft_agent` | `cash_deposit`, `cash_withdrawal` | Không | Kiểm tra tính hợp lệ của màn hình nộp/rút tiền mặt. |
| 15 | `risk.cash.screen` | `risk_cash_screen` | `CONTROL_READ` | `risk_assistant` | `cash_deposit`, `cash_withdrawal` | Không | Rà soát rủi ro giao dịch tiền mặt giá trị cao hoặc vượt số dư khả dụng. |
| 16 | `core.transfer.execute` | `core_transfer_execute` | `FINANCIAL_WRITE` | `business_orchestrator` | `domestic_transfer` | **CÓ** | Hạch toán chuyển khoản sang Core Banking qua Internal API Gateway. |
| 17 | `core.cash.execute` | `core_cash_execute` | `FINANCIAL_WRITE` | `business_orchestrator` | `cash_deposit`, `cash_withdrawal` | **CÓ** | Hạch toán nộp/rút tiền sang Core Banking qua Internal API Gateway. |

---

## 3. Cấu Trúc Cơ Sở Dữ Liệu SQLite (`data/teller_workflows.db`)

Hệ thống tự động khởi tạo và duy trì 4 bảng cấu trúc:

1. **`sessions`:** Lưu trữ toàn bộ snapshot trạng thái phiên làm việc (mã GDV, mã quầy, khách hàng, intent, plan, draft, control gates).
2. **`workflow_executions`:** Quản lý metadata của các Temporal workflows (execution ID, workflow name, trạng thái, thời gian chạy).
3. **`workflow_history_events`:** Chuỗi sự kiện lịch sử phục vụ Event Sourcing và History Replay (`WorkflowExecutionStarted`, `ActivityTaskCompleted`, `WorkflowExecutionSignaled`, `WorkflowExecutionCompleted`).
4. **`audit_logs`:** Sổ cái ghi vết bất biến, chứa `trace_id`, `caller`, `capability_id`, `idempotency_key`, kết quả hạch toán và payload JSON.

---

## 4. Đặc Tả Giao Tiếp REST API & MCP JSON-RPC 2.0

### 4.1. REST Endpoints
* `GET /api/health`: Kiểm tra trạng thái hệ thống.
* `GET /api/bootstrap`: Trả về 17 MCP Capabilities và danh mục kịch bản mẫu.
* `POST /api/sessions`: Khởi tạo phiên giao dịch quầy mới.
* `POST /api/sessions/{id}/messages`: Tiếp nhận câu lệnh tự nhiên của Teller (tự động nhận diện kịch bản cố định hoặc kích hoạt Dynamic Discovery).
* `POST /api/sessions/{id}/approvals`: Phê duyệt Maker-Checker (`customer`, `teller`, `supervisor`).
* `POST /api/sessions/{id}/execute`: Gửi lệnh hạch toán qua Internal API Gateway khi đủ điều kiện.
* `POST /api/sessions/{id}/confirm-and-execute`: Hoàn tất nhanh giao dịch nộp/rút tiền mặt trong hạn mức.

### 4.2. MCP Endpoint (`POST /api/mcp`)
Tiếp nhận các phương thức JSON-RPC 2.0 chuẩn:
* `initialize`
* `ping`
* `tools/list` (Trả về 17 công cụ kèm JSON Schema)
* `tools/call` (Thực thi công cụ có kiểm soát phân quyền RBAC & Idempotency)
* `resources/list` & `resources/read`

---

## 5. Kết Quả Kiểm Thử Tự Động (JUnit 5 & MockMvc)

Toàn bộ **23/23 automated test cases** đạt tỷ lệ thành công 100%:
* `McpServerTest` (6 tests): Kiểm thử giao thức MCP, 17 công cụ, RBAC và Idempotency.
* `OrchestratorTest` (7 tests): Kiểm thử State Machine và toàn bộ 6 kịch bản nghiệp vụ.
* `DynamicReasoningTest` (3 tests): Kiểm thử Dynamic Tool Discovery cho Tỷ giá ngoại tệ, Chi nhánh và Tổng hợp tài khoản.
* `WorkflowEngineTest` (2 tests): Kiểm thử Temporal workflow lifecycle, signals, event sourcing và SQLite crash recovery.
* `InternalApiGatewayTest` (2 tests): Kiểm thử phân quyền Gateway, cấp Trace ID và Idempotency Ledger.
* `SessionControllerTest` (3 tests): Kiểm thử tích hợp REST API endpoints.
