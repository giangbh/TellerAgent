package com.dnse.teller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Session {
    private String sessionId;
    private String transactionId;
    private int revision = 1;
    private String createdAt;
    private String updatedAt;
    private String branchId = "CN-SGD-01";
    private String counterId = "Q-07";
    private String tellerId = "TELLER-1024";
    private String customerRef = "CIF-0001842";
    private String workflow;
    private String workflowVersion;
    private String status = "SESSION_OPEN";
    private Intent intent;
    private List<ChatMessage> messages = new CopyOnWriteArrayList<>();
    private Map<String, Object> customerContext = new LinkedHashMap<>();
    private PolicyFinding policyFindings = new PolicyFinding();
    private TransactionDraft transactionDraft = new TransactionDraft();
    private RiskAssessment risk = new RiskAssessment();
    private Approvals approvals = new Approvals();
    private ControlGate control = new ControlGate();
    private ExecutionResult execution;
    private AgentRuntime agentRuntime = new AgentRuntime();
    private List<SessionEvent> events = new CopyOnWriteArrayList<>();

    public Session() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public int getRevision() { return revision; }
    public void setRevision(int revision) { this.revision = revision; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }

    public String getCounterId() { return counterId; }
    public void setCounterId(String counterId) { this.counterId = counterId; }

    public String getTellerId() { return tellerId; }
    public void setTellerId(String tellerId) { this.tellerId = tellerId; }

    public String getCustomerRef() { return customerRef; }
    public void setCustomerRef(String customerRef) { this.customerRef = customerRef; }

    public String getWorkflow() { return workflow; }
    public void setWorkflow(String workflow) { this.workflow = workflow; }

    public String getWorkflowVersion() { return workflowVersion; }
    public void setWorkflowVersion(String workflowVersion) { this.workflowVersion = workflowVersion; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Intent getIntent() { return intent; }
    public void setIntent(Intent intent) { this.intent = intent; }

    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }

    public Map<String, Object> getCustomerContext() { return customerContext; }
    public void setCustomerContext(Map<String, Object> customerContext) { this.customerContext = customerContext; }

    public PolicyFinding getPolicyFindings() { return policyFindings; }
    public void setPolicyFindings(PolicyFinding policyFindings) { this.policyFindings = policyFindings; }

    public TransactionDraft getTransactionDraft() { return transactionDraft; }
    public void setTransactionDraft(TransactionDraft transactionDraft) { this.transactionDraft = transactionDraft; }

    public RiskAssessment getRisk() { return risk; }
    public void setRisk(RiskAssessment risk) { this.risk = risk; }

    public Approvals getApprovals() { return approvals; }
    public void setApprovals(Approvals approvals) { this.approvals = approvals; }

    public ControlGate getControl() { return control; }
    public void setControl(ControlGate control) { this.control = control; }

    public ExecutionResult getExecution() { return execution; }
    public void setExecution(ExecutionResult execution) { this.execution = execution; }

    public AgentRuntime getAgentRuntime() { return agentRuntime; }
    public void setAgentRuntime(AgentRuntime agentRuntime) { this.agentRuntime = agentRuntime; }

    public List<SessionEvent> getEvents() { return events; }
    public void setEvents(List<SessionEvent> events) { this.events = events; }

    public static class ChatMessage {
        private String role;
        private String text;
        private String at;
        private List<String> thinkingSteps = new ArrayList<>();
        private Long thinkingTimeMs;
        private String reasoningMode;
        private String traceId;

        public ChatMessage() {}
        public ChatMessage(String role, String text, String at) {
            this.role = role;
            this.text = text;
            this.at = at;
        }

        public ChatMessage(String role, String text, String at, List<String> thinkingSteps, Long thinkingTimeMs, String reasoningMode) {
            this(role, text, at, thinkingSteps, thinkingTimeMs, reasoningMode, null);
        }

        public ChatMessage(String role, String text, String at, List<String> thinkingSteps, Long thinkingTimeMs, String reasoningMode, String traceId) {
            this.role = role;
            this.text = text;
            this.at = at;
            this.thinkingSteps = thinkingSteps != null ? thinkingSteps : new ArrayList<>();
            this.thinkingTimeMs = thinkingTimeMs;
            this.reasoningMode = reasoningMode;
            this.traceId = traceId;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getAt() { return at; }
        public void setAt(String at) { this.at = at; }

        public List<String> getThinkingSteps() { return thinkingSteps; }
        public void setThinkingSteps(List<String> thinkingSteps) { this.thinkingSteps = thinkingSteps; }

        public Long getThinkingTimeMs() { return thinkingTimeMs; }
        public void setThinkingTimeMs(Long thinkingTimeMs) { this.thinkingTimeMs = thinkingTimeMs; }

        public String getReasoningMode() { return reasoningMode; }
        public void setReasoningMode(String reasoningMode) { this.reasoningMode = reasoningMode; }

        public String getTraceId() { return traceId; }
        public void setTraceId(String traceId) { this.traceId = traceId; }
    }
}
