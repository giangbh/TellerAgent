# Đặc Tả Kỹ Thuật Chi Tiết (Technical Specification)

## 1. REST API Endpoints

### 1.1. Khởi Tạo Phiên Làm Việc (Create Session)
- **Method:** `POST`
- **Path:** `/api/sessions`
- **Request Body:**
  ```json
  {
    "branchId": "CN-SGD-01",
    "counterId": "Q-07",
    "tellerId": "GDV-088",
    "customerRef": "CIF-0001842"
  }
  ```
- **Response:** Session object (HTTP 200).

### 1.2. Tiếp Nhận Lệnh Nghiệp Vụ (Process Message)
- **Method:** `POST`
- **Path:** `/api/sessions/{sessionId}/messages`
- **Request Body:**
  ```json
  {
    "text": "Trích lục lịch sử giao dịch sao kê của tài khoản 3456789, phân tích dòng tiền vào, ra, giao dịch nào lớn nhất về mặt giá trị"
  }
  ```
- **Response Structure:**
  ```json
  {
    "sessionId": "uuid-v4",
    "status": "ASSISTANCE_READY",
    "workflow": "dynamic_autonomous",
    "intent": {
      "type": "DYNAMIC_AUTONOMOUS_TASK",
      "confidence": 0.99,
      "entities": {
        "accountNumber": "3456789"
      },
      "query": "..."
    },
    "messages": [
      {
        "role": "assistant",
        "text": "Dựa trên dữ liệu MCP Tools trả về, tôi xin trích lục và phân tích...",
        "at": "2026-08-27T09:30:05.238Z",
        "thinkingSteps": [
          "🧠 Phân tích ngữ cảnh: Nhận diện ý định 'DYNAMIC_AUTONOMOUS_TASK' (Độ tin cậy: 99%)",
          "🔍 Bóc tách thực thể: accountNumber=3456789",
          "📋 Lập kế hoạch ReAct: Điều phối 1 công cụ MCP (statement.transaction.history)",
          "⚡ Gọi MCP Tool: dynamic_tool_agent:statement.transaction.history (Mục tiêu: S1)",
          "🛡️ Kiểm tra an toàn & RBAC: Xác thực quyền hạn GDV hoàn tất.",
          "✨ Tổng hợp phản hồi nghiệp vụ & cập nhật giao diện Live Draft."
        ],
        "thinkingTimeMs": 7814,
        "reasoningMode": "DEEPSEEK_AI_FUNCTION_CALLING"
      }
    ]
  }
  ```

### 1.3. Phê Duyệt Kiểm Soát Viên (Supervisor Approval Signal)
- **Method:** `POST`
- **Path:** `/api/sessions/{sessionId}/approvals`
- **Request Body:**
  ```json
  {
    "action": "APPROVE",
    "approverId": "KSV-001",
    "reason": "Hồ sơ hợp lệ, đã đối chiếu chữ ký mẫu"
  }
  ```

### 1.4. Hạch Toán Tài Chính (Execute Core Financial Write)
- **Method:** `POST`
- **Path:** `/api/sessions/{sessionId}/execute`
- **Headers:** `Idempotency-Key: uuid-v4`

---

## 2. Đặc Tả Giao Thức Model Context Protocol (MCP JSON-RPC 2.0)

Hệ thống cung cấp Endpoint chuẩn MCP tại `/mcp`:

### 2.1. Liệt Kê Công Cụ (`tools/list`)
- **Request:**
  ```json
  {
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list",
    "params": {}
  }
  ```
- **Response:** Danh sách 23 Tools kèm `id`, `name`, `description`, `inputSchema`, `risk`, `sideEffect`.

### 2.2. Gọi Công Cụ (`tools/call`)
- **Request:**
  ```json
  {
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "statement_transaction_history",
      "arguments": {
        "accountNumber": "3456789",
        "customerRef": "CIF-0001842"
      }
    }
  }
  ```

---

## 3. Ma Trận Phân Quyền & Kiểm Soát Rủi Ro (RBAC & Risk Matrix)

| Năng Lực / MCP Tool | Risk Level | Yêu Cầu Idempotency | Quyền Truy Cập (Allowed Callers) |
|---|---|---|---|
| `customer.profile.read` | `READ_SAFE` | Không | `customer_context_agent`, `dynamic_tool_agent`, `business_orchestrator` |
| `customer.accounts.list` | `READ_SAFE` | Không | `customer_context_agent`, `dynamic_tool_agent`, `business_orchestrator` |
| `customer.accounts.summary` | `READ_SAFE` | Không | `customer_context_agent`, `dynamic_tool_agent`, `business_orchestrator` |
| `customer.persona.analytics` | `READ_SAFE` | Không | `dynamic_tool_agent`, `policy_agent`, `business_orchestrator` |
| `customer.credit.score.check` | `READ_SAFE` | Không | `dynamic_tool_agent`, `policy_agent`, `business_orchestrator` |
| `recommendation.nbo.products` | `READ_SAFE` | Không | `dynamic_tool_agent`, `policy_agent`, `business_orchestrator` |
| `statement.transaction.history` | `READ_SAFE` | Không | `dynamic_tool_agent`, `policy_agent`, `business_orchestrator` |
| `savings.product.advisor` | `READ_SAFE` | Không | `dynamic_tool_agent`, `policy_agent`, `business_orchestrator` |
| `card.service.manage` | `READ_SAFE` | Không | `dynamic_tool_agent`, `policy_agent`, `business_orchestrator` |
| `fx.rate.lookup` | `READ_SAFE` | Không | `policy_agent`, `dynamic_tool_agent`, `business_orchestrator` |
| `branch.directory.lookup` | `READ_SAFE` | Không | `policy_agent`, `dynamic_tool_agent`, `business_orchestrator` |
| `bank.directory.lookup` | `READ_SAFE` | Không | `policy_agent`, `dynamic_tool_agent`, `business_orchestrator` |
| `pricing.transfer.fee` | `READ_SAFE` | Không | `policy_agent`, `dynamic_tool_agent`, `business_orchestrator` |
| `knowledge.policy.search` | `READ_SAFE` | Không | `policy_agent`, `dynamic_tool_agent`, `business_orchestrator` |
| `transfer.limit.check` | `READ_SAFE` | Không | `policy_agent`, `dynamic_tool_agent`, `business_orchestrator` |
| `cash.limit.check` | `READ_SAFE` | Không | `policy_agent`, `dynamic_tool_agent`, `business_orchestrator` |
| `risk.transfer.screen` | `READ_SAFE` | Không | `risk_assistant`, `dynamic_tool_agent`, `business_orchestrator` |
| `risk.cash.screen` | `READ_SAFE` | Không | `risk_assistant`, `dynamic_tool_agent`, `business_orchestrator` |
| `transaction.draft.validate` | `WRITE_DRAFT` | Không | `transaction_draft_agent`, `dynamic_tool_agent`, `business_orchestrator` |
| `cash.draft.validate` | `WRITE_DRAFT` | Không | `transaction_draft_agent`, `dynamic_tool_agent`, `business_orchestrator` |
| `account.resolve.by_number` | `READ_SAFE` | Không | `customer_context_agent`, `transaction_draft_agent`, `dynamic_tool_agent`, `business_orchestrator` |
| `core.transfer.execute` | `FINANCIAL_WRITE` | **BẮT BUỘC** | `business_orchestrator`, `execution_gateway` |
| `core.cash.execute` | `FINANCIAL_WRITE` | **BẮT BUỘC** | `business_orchestrator`, `execution_gateway` |
