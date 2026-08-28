package com.dnse.teller.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class McpServer {
    private final McpToolRegistry registry;
    private final McpSecurityPolicyProvider policyProvider;
    private final ObjectMapper objectMapper;

    public McpServer(McpToolRegistry registry, McpSecurityPolicyProvider policyProvider, ObjectMapper objectMapper) {
        this.registry = registry;
        this.policyProvider = policyProvider;
        this.objectMapper = objectMapper;
    }

    public JsonRpcResponse handleRequest(JsonRpcRequest request) {
        if (request == null || !"2.0".equals(request.getJsonrpc())) {
            return JsonRpcResponse.error(null, -32600, "Invalid Request: jsonrpc must be '2.0'", null);
        }

        Object id = request.getId();
        String method = request.getMethod();
        Map<String, Object> params = request.getParams() != null ? request.getParams() : Map.of();

        try {
            if ("initialize".equals(method)) {
                return JsonRpcResponse.success(id, handleInitialize(params));
            } else if ("ping".equals(method)) {
                return JsonRpcResponse.success(id, Map.of());
            } else if ("tools/list".equals(method)) {
                return JsonRpcResponse.success(id, handleToolsList(params));
            } else if ("tools/call".equals(method)) {
                return JsonRpcResponse.success(id, handleToolsCall(params));
            } else if ("resources/list".equals(method)) {
                return JsonRpcResponse.success(id, handleResourcesList(params));
            } else if ("resources/read".equals(method)) {
                return JsonRpcResponse.success(id, handleResourcesRead(params));
            } else {
                return JsonRpcResponse.error(id, -32601, "Method not found: " + method, null);
            }
        } catch (McpSecurityException ex) {
            return JsonRpcResponse.error(id, -32001, ex.getMessage(), Map.of("code", ex.getCode()));
        } catch (Exception ex) {
            return JsonRpcResponse.error(id, -32603, "Internal error: " + ex.getMessage(), null);
        }
    }

    /**
     * Overload tương thích ngược cho các agent chỉ-đọc (AgentSuite). Ngữ cảnh
     * tạo ra ở đây KHÔNG mang PostingAuthorization, nên tool tài chính vẫn bị
     * từ chối — agent không thể tự post core dù gọi qua đường này.
     */
    public Map<String, Object> executeDirect(String caller, String workflow, String capabilityId,
                                             Map<String, Object> args, String idempotencyKey) throws Exception {
        return executeDirect(new ToolCallContext(caller, workflow, idempotencyKey, null), capabilityId, args);
    }

    /**
     * Đường gọi nội bộ, chỉ dành cho orchestrator. Không phơi ra HTTP.
     */
    public Map<String, Object> executeDirect(ToolCallContext context, String capabilityId, Map<String, Object> args) throws Exception {
        if (context == null || isBlank(context.caller()) || isBlank(context.workflow())) {
            throw new McpSecurityException("Thiếu ngữ cảnh gọi tool (caller/workflow).", "TOOL_CONTEXT_MISSING");
        }

        Optional<McpTool> toolOpt = registry.resolve(capabilityId);
        if (toolOpt.isEmpty()) {
            throw new McpSecurityException("Capability không tồn tại: " + capabilityId, "UNKNOWN_CAPABILITY");
        }

        McpTool tool = toolOpt.get();
        McpSecurityPolicy policy = policyProvider.getPolicy(tool.getId());
        validatePolicy(tool, policy, context.caller(), context.workflow(), context.idempotencyKey());

        if (tool.isFinancialWrite() && !context.hasPostingAuthorization()) {
            throw new McpSecurityException(
                    tool.getId() + " yêu cầu PostingAuthorization đã được kiểm chứng.",
                    "POSTING_AUTHORIZATION_MISSING");
        }

        String startedAt = Instant.now().toString();
        Map<String, Object> result = tool.execute(args, context.idempotencyKey(), context);
        String completedAt = Instant.now().toString();

        Map<String, Object> callRecord = new LinkedHashMap<>();
        callRecord.put("capabilityId", tool.getId());
        callRecord.put("toolName", tool.getName());
        callRecord.put("caller", context.caller());
        callRecord.put("risk", policy != null ? policy.getRiskLevel() : tool.getRisk());
        callRecord.put("sideEffect", policy != null ? policy.isSideEffect() : tool.isSideEffect());
        callRecord.put("startedAt", startedAt);
        callRecord.put("completedAt", completedAt);
        callRecord.put("args", maskArgs(args));
        callRecord.put("result", result);

        return callRecord;
    }

    private Map<String, Object> handleInitialize(Map<String, Object> params) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", "2024-11-05");
        result.put("serverInfo", Map.of(
            "name", "bsmart-teller-mcp-server",
            "version", "2.0.0-dynamic-policy",
            "environment", "Java-Spring-Boot-3.4"
        ));
        result.put("capabilities", Map.of(
            "tools", Map.of("listChanged", false),
            "resources", Map.of("subscribe", false, "listChanged", false)
        ));
        return result;
    }

    private Map<String, Object> handleToolsList(Map<String, Object> params) {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (McpTool tool : registry.getAllTools()) {
            McpSecurityPolicy policy = policyProvider.getPolicy(tool.getId());

            Map<String, Object> toolDef = new LinkedHashMap<>();
            toolDef.put("name", tool.getName());
            toolDef.put("id", tool.getId());
            toolDef.put("description", tool.getDescription());
            toolDef.put("inputSchema", tool.getInputSchema());
            toolDef.put("metadata", Map.of(
                "risk", policy != null ? policy.getRiskLevel() : tool.getRisk(),
                "sideEffect", policy != null ? policy.isSideEffect() : tool.isSideEffect(),
                "requiresIdempotency", policy != null ? policy.isRequiresIdempotency() : tool.isRequiresIdempotency(),
                "allowedCallers", policy != null ? policy.getAllowedCallers() : tool.getAllowedCallers(),
                "workflows", policy != null ? policy.getAllowedWorkflows() : tool.getWorkflows()
            ));
            tools.add(toolDef);
        }
        return Map.of("tools", tools);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> handleToolsCall(Map<String, Object> params) throws Exception {
        String name = params.get("name") != null ? String.valueOf(params.get("name")) : null;
        if (name == null) {
            throw new IllegalArgumentException("Missing tool name in params");
        }

        Optional<McpTool> toolOpt = registry.resolve(name);
        if (toolOpt.isEmpty()) {
            throw new McpSecurityException("Tool không tồn tại: " + name, "UNKNOWN_TOOL");
        }

        McpTool tool = toolOpt.get();
        McpSecurityPolicy policy = policyProvider.getPolicy(tool.getId());

        // P0-1: tool ghi sổ tài chính KHÔNG BAO GIỜ được phơi ra qua JSON-RPC công khai.
        // Chúng chỉ đến được core qua orchestrator, sau khi qua đủ control gate.
        if (tool.isFinancialWrite() || tool.isSideEffect() || (policy != null && policy.isSideEffect())) {
            throw new McpSecurityException(
                    tool.getId() + " không được phơi ra qua endpoint MCP. Hãy dùng luồng nghiệp vụ /api/sessions.",
                    "FINANCIAL_WRITE_NOT_EXPOSED");
        }

        Map<String, Object> arguments = params.get("arguments") instanceof Map ? (Map<String, Object>) params.get("arguments") : Map.of();

        // P0-1: không còn giá trị mặc định. Trước đây caller mặc định là
        // "business_orchestrator" — tức danh tính đặc quyền cao nhất được cấp
        // cho một request trống rỗng.
        String caller = firstNonBlank(params.get("caller"), arguments.get("_caller"));
        String workflow = firstNonBlank(params.get("workflow"), arguments.get("_workflow"));
        String idempotencyKey = firstNonBlank(params.get("idempotencyKey"), arguments.get("_idempotencyKey"));

        if (isBlank(caller) || isBlank(workflow)) {
            throw new McpSecurityException(
                    "Lời gọi tool phải khai báo tường minh 'caller' và 'workflow'.", "TOOL_CONTEXT_MISSING");
        }

        validatePolicy(tool, policy, caller, workflow, idempotencyKey);

        Map<String, Object> executionResult = tool.execute(
                arguments, idempotencyKey, ToolCallContext.readOnly(caller, workflow));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", List.of(
            Map.of("type", "text", "text", objectMapper.writeValueAsString(executionResult))
        ));
        response.put("result", executionResult);
        response.put("metadata", Map.of(
            "capabilityId", tool.getId(),
            "toolName", tool.getName(),
            "risk", policy != null ? policy.getRiskLevel() : tool.getRisk(),
            "sideEffect", policy != null ? policy.isSideEffect() : tool.isSideEffect()
        ));
        return response;
    }

    private Map<String, Object> handleResourcesList(Map<String, Object> params) {
        return Map.of(
            "resources", List.of(
                Map.of(
                    "uri", "policy://documents/QTTM-2026",
                    "name", "Quy trình giao dịch tiền mặt tại quầy (QTTM-2026)",
                    "mimeType", "text/markdown"
                ),
                Map.of(
                    "uri", "policy://documents/QTTT-CK-2026",
                    "name", "Quy trình chuyển khoản trong nước (QTTT-CK-2026)",
                    "mimeType", "text/markdown"
                ),
                Map.of(
                    "uri", "policy://documents/BIEUPHI-2026",
                    "name", "Biểu phí dịch vụ tại quầy (BIEUPHI-2026)",
                    "mimeType", "text/markdown"
                )
            )
        );
    }

    private Map<String, Object> handleResourcesRead(Map<String, Object> params) {
        String uri = params.get("uri") != null ? String.valueOf(params.get("uri")) : "";
        String text;
        if ("policy://documents/QTTM-2026".equals(uri)) {
            text = "# QTTM-2026: Quy trình giao dịch tiền mặt tại quầy\n- Mục 3.1: GDV phải đối chiếu STK, tên chủ tài khoản, số tiền và lập bảng kê trước khi xác nhận.";
        } else if ("policy://documents/QTTT-CK-2026".equals(uri)) {
            text = "# QTTT-CK-2026: Quy trình chuyển khoản trong nước\n- Mục 4.2: Yêu cầu khách hàng xác thực, kiểm tra người thụ hưởng và xác nhận phí trước khi post core.";
        } else if ("policy://documents/BIEUPHI-2026".equals(uri)) {
            text = "# BIEUPHI-2026: Biểu phí dịch vụ tại quầy\n- Mục 2.1: Phí chuyển khoản 0.03%, tối thiểu 10.000 VND, tối đa 1.000.000 VND (chưa VAT).";
        } else {
            throw new IllegalArgumentException("Resource not found: " + uri);
        }

        return Map.of(
            "contents", List.of(
                Map.of(
                    "uri", uri,
                    "mimeType", "text/markdown",
                    "text", text
                )
            )
        );
    }

    /**
     * P0-5: fail-closed.
     *
     * Bản cũ: policy == null thì return (cho qua), và danh sách rỗng cũng cho qua.
     * Bản mới: không có policy tường minh thì từ chối; danh sách rỗng nghĩa là
     * không ai được gọi.
     *
     * Ngoài ra quyền hiệu lực = GIAO của policy trong DB và khai báo cứng trong
     * code của tool. Nhờ vậy một lần sửa policy trong DB không bao giờ nới rộng
     * được quyền vượt quá thứ tác giả tool đã cho phép.
     */
    private void validatePolicy(McpTool tool, McpSecurityPolicy policy, String caller, String workflow, String idempotencyKey) {
        String capabilityId = tool.getId();

        if (policy == null) {
            throw new McpSecurityException(
                    "Không có policy tường minh cho " + capabilityId + " — từ chối theo nguyên tắc fail-closed.",
                    "POLICY_NOT_DEFINED");
        }

        Set<String> effectiveCallers = intersect(policy.getAllowedCallers(), tool.getAllowedCallers());
        if (effectiveCallers.isEmpty() || !effectiveCallers.contains(caller)) {
            throw new McpSecurityException(caller + " không được phép gọi " + capabilityId, "TOOL_POLICY_DENIED");
        }

        Set<String> effectiveWorkflows = intersect(policy.getAllowedWorkflows(), tool.getWorkflows());
        if (effectiveWorkflows.isEmpty() || !effectiveWorkflows.contains(workflow)) {
            throw new McpSecurityException(capabilityId + " không thuộc workflow " + workflow, "INVALID_WORKFLOW");
        }

        boolean needsIdempotency = policy.isRequiresIdempotency() || tool.isRequiresIdempotency();
        if (needsIdempotency && isBlank(idempotencyKey)) {
            throw new McpSecurityException(capabilityId + " yêu cầu idempotency key", "IDEMPOTENCY_REQUIRED");
        }
    }

    private static Set<String> intersect(List<String> a, List<String> b) {
        if (a == null || b == null) return Set.of();
        Set<String> result = new LinkedHashSet<>(a);
        result.retainAll(b);
        return result;
    }

    private static String firstNonBlank(Object... candidates) {
        for (Object c : candidates) {
            if (c != null) {
                String s = String.valueOf(c).trim();
                if (!s.isEmpty()) return s;
            }
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private Map<String, Object> maskArgs(Map<String, Object> args) {
        if (args == null) return Map.of();
        Map<String, Object> copy = new LinkedHashMap<>(args);
        if (copy.containsKey("beneficiaryAccount")) {
            copy.put("beneficiaryAccount", maskAccount(String.valueOf(copy.get("beneficiaryAccount"))));
        }
        if (copy.containsKey("accountNumber")) {
            copy.put("accountNumber", maskAccount(String.valueOf(copy.get("accountNumber"))));
        }
        if (copy.containsKey("customerRef")) {
            copy.put("customerRef", String.valueOf(copy.get("customerRef")).replaceAll(".(?=.{4})", "•"));
        }
        return copy;
    }

    private String maskAccount(String value) {
        if (value == null || value.length() <= 4) return value;
        return "•".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }

    public static class McpSecurityException extends RuntimeException {
        private final String code;
        public McpSecurityException(String message, String code) {
            super(message);
            this.code = code;
        }
        public String getCode() { return code; }
    }
}
