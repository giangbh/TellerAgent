package com.dnse.teller.orchestrator;

import com.dnse.teller.mcp.McpServer;
import com.dnse.teller.mcp.McpToolRegistry;
import com.dnse.teller.model.*;
import com.dnse.teller.model.AgentRuntime.DelegationInfo;
import com.dnse.teller.model.BootstrapResponse.Scenario;
import com.dnse.teller.model.Session.ChatMessage;
import com.dnse.teller.orchestrator.AgentSuite.AgentResult;
import com.dnse.teller.persistence.WorkflowRepository;
import com.dnse.teller.workflow.WorkflowEngine;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Service
public class TellerOrchestrator {
    private final McpServer mcpServer;
    private final McpToolRegistry toolRegistry;
    private final ReasoningEngine reasoning;
    private final AgentSuite agentSuite;
    private final WorkflowRepository repository;
    private final WorkflowEngine workflowEngine;
    private final Map<String, Session> sessionCache = new ConcurrentHashMap<>();

    private static final Map<String, List<String>> ALLOWED_DELEGATIONS = Map.of(
        "policy_assistance", List.of("policy_agent"),
        "domestic_transfer", List.of("customer_context_agent", "policy_agent", "transaction_draft_agent", "risk_assistant"),
        "cash_deposit", List.of("customer_context_agent", "policy_agent", "transaction_draft_agent", "risk_assistant"),
        "cash_withdrawal", List.of("customer_context_agent", "policy_agent", "transaction_draft_agent", "risk_assistant")
    );

    public TellerOrchestrator(
            McpServer mcpServer,
            McpToolRegistry toolRegistry,
            ReasoningEngine reasoning,
            AgentSuite agentSuite,
            WorkflowRepository repository,
            WorkflowEngine workflowEngine
    ) {
        this.mcpServer = mcpServer;
        this.toolRegistry = toolRegistry;
        this.reasoning = reasoning;
        this.agentSuite = agentSuite;
        this.repository = repository;
        this.workflowEngine = workflowEngine;
    }

    public Session createSession(Map<String, Object> input) {
        String now = Instant.now().toString();
        Session session = new Session();
        session.setSessionId(UUID.randomUUID().toString());
        session.setCreatedAt(now);
        session.setUpdatedAt(now);

        if (input != null) {
            if (input.get("branchId") != null) session.setBranchId(String.valueOf(input.get("branchId")));
            if (input.get("counterId") != null) session.setCounterId(String.valueOf(input.get("counterId")));
            if (input.get("tellerId") != null) session.setTellerId(String.valueOf(input.get("tellerId")));
            if (input.get("customerRef") != null) session.setCustomerRef(String.valueOf(input.get("customerRef")));
        }

        addEvent(session, "SESSION", "Mở phiên B.Smart", session.getBranchId() + " · " + session.getCounterId());
        
        // Start Durable Workflow Execution & Persist Session to SQLite
        workflowEngine.startWorkflow(session.getSessionId(), "session_lifecycle", Map.of("customerRef", session.getCustomerRef()));
        repository.saveSession(session);
        sessionCache.put(session.getSessionId(), session);

        return cloneSession(session);
    }

    public Session getSession(String sessionId) {
        return findSession(sessionId);
    }

    public synchronized Session processMessage(String sessionId, String text) throws Exception {
        Session session = findSession(sessionId);
        if (text == null || text.trim().isEmpty()) {
            throw new OrchestratorException("Nội dung yêu cầu không được để trống.", "EMPTY_MESSAGE", 400);
        }
        if ("POSTED".equals(session.getStatus())) {
            throw new OrchestratorException("Giao dịch đã được posting. Hãy mở phiên mới.", "SESSION_ALREADY_POSTED", 400);
        }

        session.getMessages().add(new ChatMessage("user", text.trim(), Instant.now().toString()));
        addEvent(session, "INPUT", "Teller cung cấp yêu cầu", text.trim());

        Intent detected = reasoning.detectIntent(text, session.getIntent());
        session.setIntent(detected);
        session.setWorkflow(detected.getWorkflow());
        session.setWorkflowVersion("domestic_transfer".equals(session.getWorkflow()) ? "3.2-poc" : "2.0-dynamic");
        session.setStatus("INTENT_DETECTED");
        addEvent(session, "AGENT", "Intent Agent xác định ý định", detected.getType() + " · confidence " + detected.getConfidence());

        Plan plan = reasoning.proposePlan(session);
        validatePlan(session, plan);
        session.getAgentRuntime().setPlan(plan);
        session.getAgentRuntime().setCompletedSteps(new ArrayList<>());
        session.getAgentRuntime().setDelegations(new ArrayList<>());
        session.getAgentRuntime().setToolCalls(new ArrayList<>());
        session.getAgentRuntime().getBudget().setDelegationsUsed(0);
        session.getAgentRuntime().getBudget().setToolCallsUsed(0);
        addEvent(session, "PLAN", "Autonomous ReAct Planner lập kế hoạch", plan.getSteps().size() + " bước · " + plan.getObjective());

        // Update Durable Workflow state
        workflowEngine.getOrRestoreWorkflow(session.getSessionId(), session.getWorkflow());

        // Dynamic Autonomous Workflow handling
        if ("dynamic_autonomous".equals(session.getWorkflow())) {
            List<String> responses = new ArrayList<>();
            for (Plan.PlanStep step : plan.getSteps()) {
                AgentResult result = delegate(session, step.getTarget(), step.getId());
                mergePatch(session, result);
                if (session.getPolicyFindings() != null && session.getPolicyFindings().getAnswer() != null) {
                    responses.add(session.getPolicyFindings().getAnswer());
                }
            }

            session.setStatus("ASSISTANCE_READY");
            String fullAns = String.join("\n\n", responses);
            if (fullAns.isEmpty()) fullAns = "Đã hoàn thành khám phá và thực thi chuỗi công cụ MCP phù hợp.";
            session.getMessages().add(new ChatMessage("assistant", fullAns, Instant.now().toString()));
            addEvent(session, "RESULT", "Dynamic ReAct hoàn thành", plan.getSteps().size() + " công cụ đã thực thi thành công.");
            return save(session);
        }

        if ("policy_assistance".equals(session.getWorkflow())) {
            AgentResult result = delegate(session, "policy_agent", "S1");
            mergePatch(session, result);
            session.setStatus("ASSISTANCE_READY");
            String ans = session.getPolicyFindings() != null ? session.getPolicyFindings().getAnswer() : "Đã hoàn thành tra cứu.";
            session.getMessages().add(new ChatMessage("assistant", ans, Instant.now().toString()));
            addEvent(session, "RESULT", "Hoàn thành hỗ trợ nghiệp vụ", "Câu trả lời có citation mock.");
            return save(session);
        }

        // Evidence Fan-out: run Customer Context Agent & Policy Agent in parallel
        CompletableFuture<AgentResult> customerFuture = CompletableFuture.supplyAsync(() -> {
            try { return delegate(session, "customer_context_agent", "S1"); }
            catch (Exception e) { throw new CompletionException(e); }
        });

        CompletableFuture<AgentResult> policyFuture = CompletableFuture.supplyAsync(() -> {
            try { return delegate(session, "policy_agent", "S2"); }
            catch (Exception e) { throw new CompletionException(e); }
        });

        CompletableFuture.allOf(customerFuture, policyFuture).join();
        mergePatch(session, customerFuture.get());
        mergePatch(session, policyFuture.get());
        addEvent(session, "BARRIER", "Evidence Barrier đã mở", "Customer context và policy evidence đã sẵn sàng.");

        // Step 3: Transaction Draft Agent
        AgentResult draftResult = delegate(session, "transaction_draft_agent", "S3");
        mergePatch(session, draftResult);
        session.setApprovals(new Approvals());

        List<String> missing = session.getTransactionDraft().getValidation() != null
            ? session.getTransactionDraft().getValidation().getMissingFields() : List.of();

        if (!missing.isEmpty()) {
            session.setStatus("WAITING_HUMAN_INPUT");
            List<String> missingGates = new ArrayList<>();
            for (String f : missing) missingGates.add("FIELD:" + f);
            session.setControl(new ControlGate(false, missingGates, Instant.now().toString()));
            session.getMessages().add(new ChatMessage("assistant", humanizeMissingFields(missing), Instant.now().toString()));
            addEvent(session, "INTERRUPT", "Cần Teller bổ sung thông tin", String.join(", ", missing));
            return save(session);
        }

        // Step 4: Risk Assistant
        AgentResult riskResult = delegate(session, "risk_assistant", "S4");
        mergePatch(session, riskResult);
        boolean riskPassed = session.getRisk() != null && "PASS".equals(session.getRisk().getDecision());
        session.setStatus(riskPassed ? "DRAFT_READY" : "RISK_REVIEW");

        String botMsg;
        if (session.getStatus().equals("DRAFT_READY")) {
            if (session.getWorkflow().startsWith("cash_")) {
                botMsg = session.getTransactionDraft().getScreenTitle() + " đã được mở và tự điền. GDV kiểm tra lại rồi bấm Xác nhận & Submit Mock.";
            } else {
                botMsg = "Bản nháp đã hoàn tất. Vui lòng cho khách hàng và Teller kiểm tra trước khi gửi core mock.";
            }
        } else {
            botMsg = "Giao dịch cần kiểm soát viên rà soát cảnh báo mock trước khi tiếp tục.";
        }
        session.getMessages().add(new ChatMessage("assistant", botMsg, Instant.now().toString()));

        evaluateControl(session);
        addEvent(session, "RESULT", "Hoàn thành bản nháp", session.getStatus() + " · không có posting tự động.");
        return save(session);
    }

    public synchronized Session approve(String sessionId, String actor) {
        Session session = findSession(sessionId);
        if (!List.of("DRAFT_READY", "AWAITING_APPROVAL", "READY_TO_POST").contains(session.getStatus())) {
            throw new OrchestratorException("Bản nháp chưa sẵn sàng để phê duyệt.", "DRAFT_NOT_READY", 400);
        }
        if (!List.of("customer", "teller", "supervisor").contains(actor)) {
            throw new OrchestratorException("Actor phê duyệt không hợp lệ: " + actor, "INVALID_APPROVER", 400);
        }
        if ("teller".equals(actor) && !session.getApprovals().isCustomer()) {
            throw new OrchestratorException("Khách hàng phải xác nhận trước Teller.", "CUSTOMER_APPROVAL_REQUIRED", 400);
        }

        boolean supReq = false;
        if (session.getTransactionDraft().getLimit() != null) {
            Object req = session.getTransactionDraft().getLimit().get("supervisorRequired");
            if (Boolean.TRUE.equals(req)) supReq = true;
        }

        if ("supervisor".equals(actor) && !supReq) {
            throw new OrchestratorException("Giao dịch này không yêu cầu kiểm soát viên.", "SUPERVISOR_NOT_REQUIRED", 400);
        }

        if ("customer".equals(actor)) session.getApprovals().setCustomer(true);
        if ("teller".equals(actor)) session.getApprovals().setTeller(true);
        if ("supervisor".equals(actor)) session.getApprovals().setSupervisor(true);

        // Signal Temporal Workflow Engine
        workflowEngine.signal("WF-" + sessionId, "Approval_" + actor, true);

        addEvent(session, "APPROVAL", labelActor(actor) + " đã xác nhận", "Approval được ghi tại checkpoint.");
        evaluateControl(session);
        session.setStatus(session.getControl().isPostingAllowed() ? "READY_TO_POST" : "AWAITING_APPROVAL");
        return save(session);
    }

    public synchronized Session execute(String sessionId) throws Exception {
        Session session = findSession(sessionId);
        evaluateControl(session);
        if (!session.getControl().isPostingAllowed()) {
            throw new OrchestratorException("Chưa đủ control gate: " + String.join(", ", session.getControl().getMissingGates()), "CONTROL_GATES_INCOMPLETE", 400);
        }

        if (session.getExecution() != null && "POSTED".equals(session.getExecution().getStatus())) {
            return cloneSession(session);
        }

        if (session.getTransactionId() == null) {
            session.setTransactionId(UUID.randomUUID().toString());
        }

        session.setStatus("POSTING_REQUESTED");
        addEvent(session, "CONTROL", "Business Orchestrator cho phép posting", "Agent không trực tiếp gọi financial-write tool.");

        String capabilityId = session.getWorkflow().startsWith("cash_") ? "core.cash.execute" : "core.transfer.execute";

        Map<String, Object> draftArgs = new HashMap<>();
        if (session.getTransactionDraft().getAmount() != null) draftArgs.put("amount", session.getTransactionDraft().getAmount());
        if (session.getTransactionDraft().getBeneficiaryAccount() != null) draftArgs.put("beneficiaryAccount", session.getTransactionDraft().getBeneficiaryAccount());
        if (session.getTransactionDraft().getBeneficiaryName() != null) draftArgs.put("beneficiaryName", session.getTransactionDraft().getBeneficiaryName());
        if (session.getTransactionDraft().getBankCode() != null) draftArgs.put("bankCode", session.getTransactionDraft().getBankCode());
        if (session.getTransactionDraft().getSourceAccountRef() != null) draftArgs.put("sourceAccountRef", session.getTransactionDraft().getSourceAccountRef());
        if (session.getTransactionDraft().getAccountNumber() != null) draftArgs.put("accountNumber", session.getTransactionDraft().getAccountNumber());
        if (session.getTransactionDraft().getTransactionType() != null) draftArgs.put("transactionType", session.getTransactionDraft().getTransactionType());

        Map<String, Object> call = mcpServer.executeDirect(
            "business_orchestrator",
            session.getWorkflow(),
            capabilityId,
            draftArgs,
            session.getTransactionId()
        );

        session.getAgentRuntime().getToolCalls().add(call);

        @SuppressWarnings("unchecked")
        Map<String, Object> res = (Map<String, Object>) call.get("result");
        ExecutionResult exec = new ExecutionResult(
            (String) res.get("status"),
            (String) res.get("coreReference"),
            (String) res.get("postedAt"),
            res.get("amount") instanceof Number ? ((Number) res.get("amount")).longValue() : null,
            (String) res.get("transactionType"),
            true
        );
        session.setExecution(exec);
        session.setStatus(exec.getStatus());

        // Complete Durable Workflow Execution in SQLite
        workflowEngine.completeWorkflow("WF-" + sessionId, Map.of("coreReference", exec.getCoreReference(), "status", exec.getStatus()));

        addEvent(session, "CORE", "Core banking mock phản hồi qua Internal API Gateway", exec.getStatus() + " · " + exec.getCoreReference());
        session.getMessages().add(new ChatMessage("assistant", "Giao dịch mock đã ghi nhận: " + exec.getCoreReference() + ".", Instant.now().toString()));
        return save(session);
    }

    public synchronized Session confirmAndExecuteCash(String sessionId) throws Exception {
        Session session = findSession(sessionId);
        if (session.getWorkflow() == null || !session.getWorkflow().startsWith("cash_")) {
            throw new OrchestratorException("Thao tác này chỉ áp dụng cho màn hình nộp/rút tiền.", "CASH_WORKFLOW_REQUIRED", 400);
        }
        if (!"DRAFT_READY".equals(session.getStatus())) {
            throw new OrchestratorException("Bản nháp tiền mặt chưa sẵn sàng để GDV xác nhận.", "DRAFT_NOT_READY", 400);
        }

        boolean supReq = false;
        if (session.getTransactionDraft().getLimit() != null) {
            Object req = session.getTransactionDraft().getLimit().get("supervisorRequired");
            if (Boolean.TRUE.equals(req)) supReq = true;
        }

        if (supReq) {
            throw new OrchestratorException("Giao dịch vượt hạn mức GDV và cần kiểm soát viên xác nhận riêng.", "SUPERVISOR_APPROVAL_REQUIRED", 400);
        }

        session.getApprovals().setCustomer(true);
        session.getApprovals().setTeller(true);
        addEvent(session, "APPROVAL", "GDV đã verify và xác nhận Submit", "Ghi nhận khách hàng đồng ý và GDV đã kiểm tra dữ liệu tự điền.");
        evaluateControl(session);
        return execute(sessionId);
    }

    public BootstrapResponse bootstrap() {
        BootstrapResponse resp = new BootstrapResponse();
        resp.setCapabilities(toolRegistry.getCapabilitiesList());
        resp.setScenarios(List.of(
            new Scenario("dynamic-fx", "Tra cứu tỷ giá & quy đổi (Dynamic)", "Tỷ giá 2000 EUR hôm nay đổi ra bao nhiêu VND?"),
            new Scenario("dynamic-branch", "Tìm mạng lưới chi nhánh (Dynamic)", "Địa chỉ và hotline chi nhánh ngân hàng tại Hà Nội ở đâu?"),
            new Scenario("dynamic-summary", "Tổng hợp tài khoản (Dynamic)", "Tổng hợp toàn bộ tài khoản thanh toán và sổ tiết kiệm của khách hàng"),
            new Scenario("cash-deposit", "Nộp tiền tự điền", "Nộp tiền 50tr vào tài khoản 3456789"),
            new Scenario("cash-withdrawal", "Rút tiền tự điền", "Rút tiền 50tr từ tài khoản 3456789"),
            new Scenario("transfer-complete", "Chuyển khoản hoàn chỉnh", "Chuyển 50 triệu cho Nguyễn Văn An tại Vietcombank, số tài khoản 012345678901"),
            new Scenario("risk-review", "Cảnh báo rủi ro mock", "Chuyển 220 triệu cho Trần Thị Lan tại BIDV, số tài khoản 123456789999"),
            new Scenario("policy", "Hỏi quy trình", "Quy trình và biểu phí chuyển khoản tại quầy như thế nào?")
        ));
        return resp;
    }

    private AgentResult delegate(Session session, String agent, String stepId) throws Exception {
        if (!"dynamic_autonomous".equals(session.getWorkflow())) {
            List<String> allowed = ALLOWED_DELEGATIONS.getOrDefault(session.getWorkflow(), List.of());
            if (!allowed.contains(agent) && !agent.startsWith("dynamic_tool_agent:")) {
                throw new OrchestratorException("Không được delegate sang " + agent, "DELEGATION_DENIED", 400);
            }
        }

        if (session.getAgentRuntime().getBudget().getDelegationsUsed() >= session.getAgentRuntime().getBudget().getMaxDelegations()) {
            throw new OrchestratorException("Đã vượt ngân sách delegation.", "DELEGATION_BUDGET_EXCEEDED", 400);
        }

        session.getAgentRuntime().getBudget().setDelegationsUsed(session.getAgentRuntime().getBudget().getDelegationsUsed() + 1);
        DelegationInfo delegation = new DelegationInfo(stepId, agent, "RUNNING", Instant.now().toString());
        session.getAgentRuntime().getDelegations().add(delegation);
        addEvent(session, "DELEGATE", "Orchestrator gọi " + agent, "Bước " + stepId);

        AgentResult result = agentSuite.run(agent, session);

        delegation.setStatus("COMPLETED");
        delegation.setCompletedAt(Instant.now().toString());
        session.getAgentRuntime().getCompletedSteps().add(stepId);
        session.getAgentRuntime().getToolCalls().addAll(result.getToolCalls());
        session.getAgentRuntime().getBudget().setToolCallsUsed(
            session.getAgentRuntime().getBudget().getToolCallsUsed() + result.getToolCalls().size()
        );

        if (session.getAgentRuntime().getBudget().getToolCallsUsed() > session.getAgentRuntime().getBudget().getMaxToolCalls()) {
            throw new OrchestratorException("Đã vượt ngân sách tool call.", "TOOL_BUDGET_EXCEEDED", 400);
        }

        // Record Activity in Durable Workflow Engine
        workflowEngine.recordActivityExecution("WF-" + session.getSessionId(), agent + "_" + stepId, "COMPLETED", Map.of("toolCallsCount", result.getToolCalls().size()));

        return result;
    }

    @SuppressWarnings("unchecked")
    private void mergePatch(Session session, AgentResult result) {
        for (Map.Entry<String, Object> entry : result.getWrites().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if ("customerContext".equals(key) && value instanceof Map<?, ?> m) {
                session.setCustomerContext((Map<String, Object>) m);
            } else if ("policyFindings".equals(key) && value instanceof PolicyFinding pf) {
                session.setPolicyFindings(pf);
            } else if ("transactionDraft".equals(key) && value instanceof TransactionDraft td) {
                session.setTransactionDraft(td);
            } else if ("risk".equals(key) && value instanceof RiskAssessment ra) {
                session.setRisk(ra);
            }
        }
        addEvent(session, "PATCH", result.getAgent() + " cập nhật state", String.join(", ", result.getWrites().keySet()) + " · evidence " + String.join(", ", result.getEvidence()));
    }

    private void validatePlan(Session session, Plan plan) {
        if (plan.getSteps() == null || plan.getSteps().isEmpty()) {
            throw new OrchestratorException("Plan không có bước thực hiện.", "INVALID_PLAN", 400);
        }
        if (plan.getSteps().size() > session.getAgentRuntime().getBudget().getMaxDelegations()) {
            throw new OrchestratorException("Plan vượt ngân sách.", "INVALID_PLAN", 400);
        }
    }

    private void evaluateControl(Session session) {
        List<String> missing = new ArrayList<>();
        boolean draftValid = session.getTransactionDraft().getValidation() != null && Boolean.TRUE.equals(session.getTransactionDraft().getValidation().getValid());
        if (!draftValid) missing.add("DRAFT_VALID");

        boolean riskPass = session.getRisk() != null && "PASS".equals(session.getRisk().getDecision());
        if (!riskPass) missing.add("RISK_PASS");

        if (!session.getApprovals().isCustomer()) missing.add("CUSTOMER_CONFIRMED");
        if (!session.getApprovals().isTeller()) missing.add("TELLER_CONFIRMED");

        boolean supReq = false;
        if (session.getTransactionDraft().getLimit() != null) {
            Object req = session.getTransactionDraft().getLimit().get("supervisorRequired");
            if (Boolean.TRUE.equals(req)) supReq = true;
        }
        if (supReq && !session.getApprovals().isSupervisor()) {
            missing.add("SUPERVISOR_CONFIRMED");
        }

        session.setControl(new ControlGate(missing.isEmpty(), missing, Instant.now().toString()));
    }

    private Session findSession(String sessionId) {
        if (sessionCache.containsKey(sessionId)) {
            return sessionCache.get(sessionId);
        }

        Optional<Session> persisted = repository.findSessionById(sessionId);
        if (persisted.isPresent()) {
            Session s = persisted.get();
            sessionCache.put(sessionId, s);
            return s;
        }

        throw new OrchestratorException("Không tìm thấy phiên: " + sessionId, "SESSION_NOT_FOUND", 404);
    }

    private void addEvent(Session session, String type, String title, String detail) {
        int seq = session.getEvents().size() + 1;
        session.getEvents().add(new SessionEvent(seq, Instant.now().toString(), type, title, detail));
    }

    private Session save(Session session) {
        session.setRevision(session.getRevision() + 1);
        session.setUpdatedAt(Instant.now().toString());
        sessionCache.put(session.getSessionId(), session);
        repository.saveSession(session);
        return cloneSession(session);
    }

    private String humanizeMissingFields(List<String> fields) {
        Map<String, String> labels = Map.of(
            "sourceAccountRef", "tài khoản nguồn",
            "beneficiaryAccount", "số tài khoản người nhận",
            "beneficiaryName", "tên người nhận",
            "bankCode", "ngân hàng thụ hưởng",
            "accountNumber", "số tài khoản",
            "amount", "số tiền"
        );
        List<String> humanized = new ArrayList<>();
        for (String f : fields) {
            humanized.add(labels.getOrDefault(f, f));
        }
        return "Cần Teller bổ sung: " + String.join(", ", humanized) + ".";
    }

    private String labelActor(String actor) {
        return switch (actor) {
            case "customer" -> "Khách hàng";
            case "teller" -> "Teller";
            case "supervisor" -> "Kiểm soát viên";
            default -> actor;
        };
    }

    private Session cloneSession(Session s) {
        return s;
    }

    public static class OrchestratorException extends RuntimeException {
        private final String code;
        private final int status;

        public OrchestratorException(String message, String code, int status) {
            super(message);
            this.code = code;
            this.status = status;
        }

        public String getCode() { return code; }
        public int getStatus() { return status; }
    }
}
