package com.dnse.teller.workflow;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowExecution {
    private String executionId;
    private String sessionId;
    private String workflowName;
    private String status = "RUNNING"; // RUNNING, WAITING_SIGNAL, COMPLETED, FAILED
    private String startTime;
    private String closeTime;
    private Map<String, Object> state = new LinkedHashMap<>();
    private List<String> history = new ArrayList<>();

    public WorkflowExecution() {}

    public WorkflowExecution(String executionId, String sessionId, String workflowName, String startTime) {
        this.executionId = executionId;
        this.sessionId = sessionId;
        this.workflowName = workflowName;
        this.startTime = startTime;
    }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }

    public Map<String, Object> getState() { return state; }
    public void setState(Map<String, Object> state) { this.state = state; }

    public List<String> getHistory() { return history; }
    public void setHistory(List<String> history) { this.history = history; }
}
