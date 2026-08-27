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

### 1.5. Giám Định Phản Hồi Bằng LLM-as-a-Judge (Judge Evaluation)
- **Method:** `POST`
- **Path:** `/api/evaluations/judge`
- **Request Body:**
  ```json
  {
    "sessionId": "31f2c79a-66d5-443e-844d-44998b46d605",
    "traceId": "trace-70b65936-b0b",
    "userPrompt": "Trích lục sao kê của tài khoản 3456789 và cho biết giao dịch lớn nhất",
    "assistantResponse": "Dựa trên dữ liệu từ hệ thống...",
    "latencyMs": 1850
  }
  ```
- **Response Structure (HTTP 200):**
  ```json
  {
    "evaluationId": "eval-ac23916e-c",
    "sessionId": "31f2c79a-66d5-443e-844d-44998b46d605",
    "traceId": "trace-70b65936-b0b",
    "overallScore": 8.9,
    "hallucinationScore": 10.0,
    "politenessScore": 8.0,
    "complianceScore": 9.0,
    "latencyScore": 5.0,
    "latencyMs": 27335,
    "verdict": "PASS",
    "critique": "Phản hồi trích lục sao kê chính xác tuyệt đối với dữ liệu gốc: đúng số tài khoản, số giao dịch, tổng tiền vào/ra, dòng tiền ròng và giao dịch lớn nhất...",
    "evaluatedAt": "2026-08-27T10:19:16.511Z",
    "judgeModel": "deepseek-chat (LLM-as-a-Judge)"
  }
  ```

### 1.6. Danh Sách Dấu Vết Phân Tán (OpenTelemetry Traces)
- **Method:** `GET`
- **Path:** `/api/observability/traces?limit=20`
- **Response Structure (HTTP 200):**
  ```json
  [
    {
      "traceId": "trace-70b65936-b0b",
      "sessionId": "31f2c79a-66d5-443e-844d-44998b46d605",
      "rootOperation": "process_message",
      "startTimeMs": 1787825232311,
      "endTimeMs": 1787825240267,
      "totalDurationMs": 7956,
      "status": "OK",
      "spans": [
        {
          "spanId": "span-d8e62fa7",
          "name": "intent_detection",
          "durationMs": 1834,
          "status": "OK",
          "attributes": { "detectedIntent": "DYNAMIC_AUTONOMOUS_TASK" }
        },
        {
          "spanId": "span-4907af06",
          "name": "mcp_tool:statement.transaction.history",
          "durationMs": 12,
          "status": "OK"
        },
        {
          "spanId": "span-a4c1482b",
          "name": "llm_reasoning_synthesis",
          "durationMs": 6109,
          "status": "OK"
        }
      ]
    }
  ]
  ```

### 1.7. Chi Tiết Một Dấu Vết Phân Tán (Trace Detail)
- **Method:** `GET`
- **Path:** `/api/observability/traces/{traceId}`

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
