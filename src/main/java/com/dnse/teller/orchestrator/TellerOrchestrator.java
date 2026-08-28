package com.dnse.teller.orchestrator;

import com.dnse.teller.mcp.McpServer;
import com.dnse.teller.mcp.McpToolRegistry;
import com.dnse.teller.mcp.ToolCallContext;
import com.dnse.teller.security.AuthenticatedActor;
import com.dnse.teller.security.PostingAuthorization;
import com.dnse.teller.security.PostingAuthorizer;
import com.dnse.teller.model.*;
import com.dnse.teller.model.ApprovalRecord;
import com.dnse.teller.model.AgentRuntime.DelegationInfo;
import com.dnse.teller.model.BootstrapResponse.Scenario;
import com.dnse.teller.model.Session.ChatMessage;
import com.dnse.teller.orchestrator.AgentSuite.AgentResult;
import com.dnse.teller.persistence.WorkflowRepository;
import com.dnse.teller.workflow.WorkflowEngine;
import com.dnse.teller.observability.OpenTelemetryTracer;
import com.dnse.teller.observability.TraceContext;
import com.dnse.teller.observability.TraceSpan;
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
    private final OpenTelemetryTracer tracer;
    private final PostingAuthorizer postingAuthorizer;
    private final Map<String, Session> sessionCache = new ConcurrentHashMap<>();

    private static final Set<String> ACCEPTED_CONSENT_EVIDENCE =
        Set.of("OTP", "SIGNATURE", "DOCUMENT", "BIOMETRIC");

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
            WorkflowEngine workflowEngine,
            OpenTelemetryTracer tracer,
            PostingAuthorizer postingAuthorizer
    ) {
        this.mcpServer = mcpServer;
        this.toolRegistry = toolRegistry;
        this.reasoning = reasoning;
        this.agentSuite = agentSuite;
        this.repository = repository;
        this.workflowEngine = workflowEngine;
        this.tracer = tracer;
        this.postingAuthorizer = postingAuthorizer;
    }

    public Session createSession(Map<String, Object> input) {
        return createSession(input, null);
    }

    /**
     * tellerId và branchId lấy từ danh tính đã xác thực, không lấy từ body do
     * client gửi lên — nếu không, audit trail sẽ ghi lại bất cứ tên nào người
     * gọi muốn.
     */
    public Session createSession(Map<String, Object> input, AuthenticatedActor actor) {
        String now = Instant.now().toString();
        Session session = new Session();
        session.setSessionId(UUID.randomUUID().toString());
        session.setCreatedAt(now);
        session.setUpdatedAt(now);

        if (input != null) {
            if (input.get("counterId") != null) session.setCounterId(String.valueOf(input.get("counterId")));
            if (input.get("customerRef") != null) session.setCustomerRef(String.valueOf(input.get("customerRef")));
            if (actor == null && input.get("branchId") != null) session.setBranchId(String.valueOf(input.get("branchId")));
        }
        if (actor != null) {
            session.setTellerId(actor.userId());
            if (actor.branchId() != null) session.setBranchId(actor.branchId());
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

        long startTime = System.currentTimeMillis();
        TraceContext traceContext = tracer.startTrace(sessionId, "process_message");
        String currentTraceId = traceContext.getTraceId();
        List<String> thinkingSteps = new ArrayList<>();

        session.getMessages().add(new ChatMessage("user", text.trim(), Instant.now().toString()));
        addEvent(session, "INPUT", "Teller cung cấp yêu cầu", text.trim());

        // Span 1: Intent Detection
        TraceSpan spanIntent = tracer.startSpan("intent_detection", null);
        Intent detected = reasoning.detectIntent(text, session.getIntent());
        spanIntent.addAttribute("detectedIntent", detected.getType());
        spanIntent.addAttribute("confidence", detected.getConfidence());
        tracer.endSpan(spanIntent);

        session.setIntent(detected);
        session.setWorkflow(detected.getWorkflow());
        session.setWorkflowVersion("domestic_transfer".equals(session.getWorkflow()) ? "3.2-poc" : "2.0-dynamic");
        session.setStatus("INTENT_DETECTED");
        addEvent(session, "AGENT", "Intent Agent xác định ý định", detected.getType() + " · confidence " + detected.getConfidence());

        thinkingSteps.add(String.format("🧠 Phân tích ngữ cảnh: Nhận diện ý định '%s' (Độ tin cậy: %.0f%%)", detected.getType(), detected.getConfidence() * 100));
        if (detected.getEntities() != null && !detected.getEntities().isEmpty()) {
            List<String> entPairs = new ArrayList<>();
            detected.getEntities().forEach((k, v) -> {
                if (v != null && !"discoveredTools".equals(k)) {
                    entPairs.add(k + "=" + v);
                }
            });
            if (!entPairs.isEmpty()) {
                thinkingSteps.add("🔍 Bóc tách thực thể: " + String.join(", ", entPairs));
            }
        }

        // Span 2: ReAct Planning
        TraceSpan spanPlan = tracer.startSpan("react_planning", null);
        Plan plan = reasoning.proposePlan(session);
        validatePlan(session, plan);
        spanPlan.addAttribute("planStepsCount", plan.getSteps().size());
        tracer.endSpan(spanPlan);

        session.getAgentRuntime().setPlan(plan);
        session.getAgentRuntime().setCompletedSteps(new ArrayList<>());
        session.getAgentRuntime().setDelegations(new ArrayList<>());
        session.getAgentRuntime().setToolCalls(new ArrayList<>());
        session.getAgentRuntime().getBudget().setDelegationsUsed(0);
        session.getAgentRuntime().getBudget().setToolCallsUsed(0);
        addEvent(session, "PLAN", "Autonomous ReAct Planner lập kế hoạch", plan.getSteps().size() + " bước · " + plan.getObjective());

        thinkingSteps.add(String.format("📋 Lập kế hoạch ReAct: Điều phối %d công cụ MCP (%s)", plan.getSteps().size(), plan.getObjective()));
        for (Plan.PlanStep st : plan.getSteps()) {
            thinkingSteps.add(String.format("⚡ Gọi MCP Tool: %s (Mục tiêu: %s)", st.getTarget(), st.getId()));
        }
        thinkingSteps.add("🛡️ Kiểm tra an toàn & RBAC: Xác thực quyền hạn GDV, kiểm tra hạn mức & idempotency hoàn tất.");
        thinkingSteps.add("✨ Tổng hợp phản hồi nghiệp vụ & cập nhật giao diện Live Draft.");

        // Update Durable Workflow state
        workflowEngine.getOrRestoreWorkflow(session.getSessionId(), session.getWorkflow());

        // Dynamic Autonomous Workflow handling
        if ("dynamic_autonomous".equals(session.getWorkflow())) {
            List<String> responses = new ArrayList<>();
            Map<String, Object> allToolOutputs = new LinkedHashMap<>();

            for (Plan.PlanStep step : plan.getSteps()) {
                TraceSpan spanTool = tracer.startSpan("mcp_tool:" + step.getTarget(), null);
                AgentResult result = delegate(session, step.getTarget(), step.getId());
                mergePatch(session, result);
                if (result.getWrites() != null) {
                    allToolOutputs.putAll(result.getWrites());
                }
                if (session.getPolicyFindings() != null && session.getPolicyFindings().getAnswer() != null) {
                    responses.add(session.getPolicyFindings().getAnswer());
                }
                tracer.endSpan(spanTool);
            }

            session.setStatus("ASSISTANCE_READY");

            // Span: LLM Reasoning Synthesis
            TraceSpan spanSynth = tracer.startSpan("llm_reasoning_synthesis", null);
            Optional<String> aiSynthesis = reasoning.synthesizeAnswer(text, allToolOutputs);
            tracer.endSpan(spanSynth);

            String fullAns = aiSynthesis.orElseGet(() -> {
                String fallback = String.join("\n\n", responses);
                return fallback.isEmpty() ? "Đã hoàn thành khám phá và thực thi chuỗi công cụ MCP phù hợp." : fallback;
            });

            long duration = System.currentTimeMillis() - startTime;
            tracer.endTrace();
            session.getMessages().add(new ChatMessage("assistant", fullAns, Instant.now().toString(), thinkingSteps, duration, "DEEPSEEK_AI_FUNCTION_CALLING", currentTraceId));
            addEvent(session, "RESULT", "Dynamic ReAct hoàn thành", plan.getSteps().size() + " công cụ đã thực thi thành công.");
            return save(session);
        }

        if ("policy_assistance".equals(session.getWorkflow())) {
            TraceSpan spanPolicy = tracer.startSpan("policy_agent", null);
            AgentResult result = delegate(session, "policy_agent", "S1");
            mergePatch(session, result);
            tracer.endSpan(spanPolicy);

            session.setStatus("ASSISTANCE_READY");
            String ans = session.getPolicyFindings() != null ? session.getPolicyFindings().getAnswer() : "Đã hoàn thành tra cứu.";
            long duration = System.currentTimeMillis() - startTime;
            tracer.endTrace();
            session.getMessages().add(new ChatMessage("assistant", ans, Instant.now().toString(), thinkingSteps, duration, "DEEPSEEK_AI_FUNCTION_CALLING", currentTraceId));
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

        long duration = System.currentTimeMillis() - startTime;

        if (!missing.isEmpty()) {
            session.setStatus("WAITING_HUMAN_INPUT");
            List<String> missingGates = new ArrayList<>();
            for (String f : missing) missingGates.add("FIELD:" + f);
            session.setControl(new ControlGate(false, missingGates, Instant.now().toString()));
            tracer.endTrace();
            session.getMessages().add(new ChatMessage("assistant", humanizeMissingFields(missing), Instant.now().toString(), thinkingSteps, duration, "DEEPSEEK_AI_FUNCTION_CALLING", currentTraceId));
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
        tracer.endTrace();
        session.getMessages().add(new ChatMessage("assistant", botMsg, Instant.now().toString(), thinkingSteps, duration, "DEEPSEEK_AI_FUNCTION_CALLING", currentTraceId));

        evaluateControl(session);
        addEvent(session, "RESULT", "Hoàn thành bản nháp", session.getStatus() + " · không có posting tự động.");
        return save(session);
    }

    /**
     * P0-3: phê duyệt gắn với danh tính đã xác thực.
     *
     * Bản cũ nhận "actor" là chuỗi tự do từ request body — bất kỳ ai POST
     * {"actor":"supervisor"} đều trở thành kiểm soát viên, và không có ràng
     * buộc nào ngăn GDV tự ký mắt thứ hai cho chính mình.
     */
    public synchronized Session approve(String sessionId, String requestedRole, AuthenticatedActor actor) {
        Objects.requireNonNull(actor, "actor");
        Session session = findSession(sessionId);
        requireApprovable(session);

        String role = requestedRole != null ? requestedRole.trim().toLowerCase() : "";

        switch (role) {
            case "teller" -> {
                actor.requireRole(AuthenticatedActor.Role.TELLER);
                if (!session.getApprovals().isCustomer()) {
                    throw new OrchestratorException(
                        "Khách hàng phải xác nhận trước Teller.", "CUSTOMER_APPROVAL_REQUIRED", 400);
                }
                session.getApprovals().recordTeller(actor);
            }
            case "supervisor" -> {
                actor.requireRole(AuthenticatedActor.Role.SUPERVISOR);
                if (!supervisorRequired(session)) {
                    throw new OrchestratorException(
                        "Giao dịch này không yêu cầu kiểm soát viên.", "SUPERVISOR_NOT_REQUIRED", 400);
                }
                ApprovalRecord tellerRecord = session.getApprovals().getTellerRecord();
                if (tellerRecord != null && actor.userId().equals(tellerRecord.getActorId())) {
                    throw new OrchestratorException(
                        "Vi phạm nguyên tắc 4 mắt: kiểm soát viên trùng với GDV lập giao dịch.",
                        "SEGREGATION_OF_DUTIES_VIOLATION", 403);
                }
                session.getApprovals().recordSupervisor(actor);
            }
            case "customer" -> throw new OrchestratorException(
                "Xác nhận của khách hàng phải đi qua /consent kèm bằng chứng.",
                "CUSTOMER_CONSENT_REQUIRES_EVIDENCE", 400);
            default -> throw new OrchestratorException(
                "Vai trò phê duyệt không hợp lệ: " + requestedRole, "INVALID_APPROVER", 400);
        }

        workflowEngine.signal("WF-" + sessionId, "Approval_" + role, actor.userId());
        addEvent(session, "APPROVAL", labelActor(role) + " đã xác nhận",
            actor.displayName() + " (" + actor.userId() + ")");

        evaluateControl(session);
        session.setStatus(session.getControl().isPostingAllowed() ? "READY_TO_POST" : "AWAITING_APPROVAL");
        return save(session);
    }

    /**
     * P0-4: GDV ghi nhận việc khách hàng đã đồng ý, KÈM BẰNG CHỨNG.
     *
     * Bản cũ tự đặt approvals.customer = true bên trong confirmAndExecuteCash
     * rồi ghi vào audit trail dòng "Ghi nhận khách hàng đồng ý" — tức hệ thống
     * tự sinh ra sự đồng ý của khách hàng và tự chứng nhận điều đó.
     */
    public synchronized Session recordCustomerConsent(String sessionId, AuthenticatedActor actor,
                                                      String evidenceType, String evidenceRef) {
        Objects.requireNonNull(actor, "actor");
        actor.requireRole(AuthenticatedActor.Role.TELLER);

        Session session = findSession(sessionId);
        requireApprovable(session);

        String type = evidenceType != null ? evidenceType.trim().toUpperCase() : "";
        if (!ACCEPTED_CONSENT_EVIDENCE.contains(type)) {
            throw new OrchestratorException(
                "Loại bằng chứng đồng ý không hợp lệ. Chấp nhận: " + ACCEPTED_CONSENT_EVIDENCE,
                "CONSENT_EVIDENCE_INVALID", 400);
        }
        if (evidenceRef == null || evidenceRef.trim().isEmpty()) {
            throw new OrchestratorException(
                "Thiếu tham chiếu bằng chứng (mã OTP, số phiếu, mã chứng từ).",
                "CONSENT_EVIDENCE_REF_MISSING", 400);
        }

        session.getApprovals().recordCustomerConsent(actor, type, evidenceRef.trim());
        addEvent(session, "APPROVAL", "Khách hàng đã xác nhận",
            "Bằng chứng " + type + " · " + evidenceRef.trim() + " · ghi nhận bởi " + actor.userId());

        evaluateControl(session);
        session.setStatus(session.getControl().isPostingAllowed() ? "READY_TO_POST" : "AWAITING_APPROVAL");
        return save(session);
    }

    private void requireApprovable(Session session) {
        if (!List.of("DRAFT_READY", "AWAITING_APPROVAL", "READY_TO_POST").contains(session.getStatus())) {
            throw new OrchestratorException("Bản nháp chưa sẵn sàng để phê duyệt.", "DRAFT_NOT_READY", 400);
        }
    }

    private boolean supervisorRequired(Session session) {
        if (session.getTransactionDraft() == null || session.getTransactionDraft().getLimit() == null) return false;
        return Boolean.TRUE.equals(session.getTransactionDraft().getLimit().get("supervisorRequired"));
    }

    /**
     * P0-1/P0-2/P1-5: hạch toán chỉ xảy ra sau khi PostingAuthorizer tự kiểm
     * chứng lại toàn bộ control gate từ dữ liệu ĐÃ PERSIST, và tool tài chính
     * chỉ nhận lệnh qua giấy phép đó.
     */
    public synchronized Session execute(String sessionId, AuthenticatedActor actor) throws Exception {
        Objects.requireNonNull(actor, "actor");
        actor.requireRole(AuthenticatedActor.Role.TELLER);

        Session session = findSession(sessionId);

        if (session.getExecution() != null && "POSTED".equals(session.getExecution().getStatus())) {
            return cloneSession(session);
        }

        ApprovalRecord tellerRecord = session.getApprovals().getTellerRecord();
        if (tellerRecord == null || !actor.userId().equals(tellerRecord.getActorId())) {
            throw new OrchestratorException(
                "Chỉ GDV đã xác nhận giao dịch mới được gửi hạch toán.", "TELLER_MISMATCH", 403);
        }

        evaluateControl(session);
        if (!session.getControl().isPostingAllowed()) {
            throw new OrchestratorException(
                "Chưa đủ control gate: " + String.join(", ", session.getControl().getMissingGates()),
                "CONTROL_GATES_INCOMPLETE", 400);
        }

        session.setStatus("POSTING_REQUESTED");
        addEvent(session, "CONTROL", "Yêu cầu cấp giấy phép hạch toán",
            "Agent không có đường đi trực tiếp tới financial-write tool.");
        save(session); // persist trước, để authorizer kiểm chứng trên dữ liệu bền vững

        PostingAuthorization authorization = postingAuthorizer.authorize(sessionId);
        session.setTransactionId(authorization.getIdempotencyKey());

        Map<String, Object> draftArgs = new HashMap<>();
        TransactionDraft draft = session.getTransactionDraft();
        if (draft.getAmount() != null) draftArgs.put("amount", draft.getAmount());
        if (draft.getBeneficiaryAccount() != null) draftArgs.put("beneficiaryAccount", draft.getBeneficiaryAccount());
        if (draft.getBeneficiaryName() != null) draftArgs.put("beneficiaryName", draft.getBeneficiaryName());
        if (draft.getBankCode() != null) draftArgs.put("bankCode", draft.getBankCode());
        if (draft.getSourceAccountRef() != null) draftArgs.put("sourceAccountRef", draft.getSourceAccountRef());
        if (draft.getAccountNumber() != null) draftArgs.put("accountNumber", draft.getAccountNumber());
        if (draft.getTransactionType() != null) draftArgs.put("transactionType", draft.getTransactionType());

        Map<String, Object> call = mcpServer.executeDirect(
            ToolCallContext.forPosting(authorization),
            authorization.getCapabilityId(),
            draftArgs
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

        workflowEngine.completeWorkflow("WF-" + sessionId,
            Map.of("coreReference", exec.getCoreReference(), "status", exec.getStatus()));

        addEvent(session, "CORE", "Core banking mock phản hồi qua Internal API Gateway",
            exec.getStatus() + " · " + exec.getCoreReference() + " · GDV " + authorization.getTellerActorId());
        session.getMessages().add(new ChatMessage("assistant",
            "Giao dịch mock đã ghi nhận: " + exec.getCoreReference() + ".", Instant.now().toString()));
        return save(session);
    }

    /**
     * P0-4: chỉ còn là bước gửi hạch toán cho luồng tiền mặt. Không tự đặt cờ
     * phê duyệt thay cho khách hàng và GDV nữa — hai bước đó phải đã xảy ra
     * tường minh qua recordCustomerConsent() và approve().
     */
    public synchronized Session confirmAndExecuteCash(String sessionId, AuthenticatedActor actor) throws Exception {
        Session session = findSession(sessionId);
        if (session.getWorkflow() == null || !session.getWorkflow().startsWith("cash_")) {
            throw new OrchestratorException(
                "Thao tác này chỉ áp dụng cho màn hình nộp/rút tiền.", "CASH_WORKFLOW_REQUIRED", 400);
        }
        if (!session.getApprovals().isCustomer()) {
            throw new OrchestratorException(
                "Chưa ghi nhận xác nhận của khách hàng kèm bằng chứng.", "CUSTOMER_CONSENT_MISSING", 400);
        }
        if (!session.getApprovals().isTeller()) {
            throw new OrchestratorException("Chưa có xác nhận của GDV.", "TELLER_APPROVAL_MISSING", 400);
        }
        return execute(sessionId, actor);
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
