package com.dnse.teller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentRuntime {
    private String reasoningMode = "MOCK_STRUCTURED_PLANNER";
    private Plan plan;
    private List<String> completedSteps = new ArrayList<>();
    private List<DelegationInfo> delegations = new ArrayList<>();
    private List<Map<String, Object>> toolCalls = new ArrayList<>();
    private Budget budget = new Budget();

    public AgentRuntime() {}

    public String getReasoningMode() { return reasoningMode; }
    public void setReasoningMode(String reasoningMode) { this.reasoningMode = reasoningMode; }

    public Plan getPlan() { return plan; }
    public void setPlan(Plan plan) { this.plan = plan; }

    public List<String> getCompletedSteps() { return completedSteps; }
    public void setCompletedSteps(List<String> completedSteps) { this.completedSteps = completedSteps; }

    public List<DelegationInfo> getDelegations() { return delegations; }
    public void setDelegations(List<DelegationInfo> delegations) { this.delegations = delegations; }

    public List<Map<String, Object>> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<Map<String, Object>> toolCalls) { this.toolCalls = toolCalls; }

    public Budget getBudget() { return budget; }
    public void setBudget(Budget budget) { this.budget = budget; }

    public static class Budget {
        private int maxDelegations = 12;
        private int maxToolCalls = 32;
        private int delegationsUsed = 0;
        private int toolCallsUsed = 0;

        public Budget() {}

        public int getMaxDelegations() { return maxDelegations; }
        public void setMaxDelegations(int maxDelegations) { this.maxDelegations = maxDelegations; }

        public int getMaxToolCalls() { return maxToolCalls; }
        public void setMaxToolCalls(int maxToolCalls) { this.maxToolCalls = maxToolCalls; }

        public int getDelegationsUsed() { return delegationsUsed; }
        public void setDelegationsUsed(int delegationsUsed) { this.delegationsUsed = delegationsUsed; }

        public int getToolCallsUsed() { return toolCallsUsed; }
        public void setToolCallsUsed(int toolCallsUsed) { this.toolCallsUsed = toolCallsUsed; }
    }

    public static class DelegationInfo {
        private String stepId;
        private String agent;
        private String status = "RUNNING";
        private String startedAt;
        private String completedAt;

        public DelegationInfo() {}
        public DelegationInfo(String stepId, String agent, String status, String startedAt) {
            this.stepId = stepId;
            this.agent = agent;
            this.status = status;
            this.startedAt = startedAt;
        }

        public String getStepId() { return stepId; }
        public void setStepId(String stepId) { this.stepId = stepId; }

        public String getAgent() { return agent; }
        public void setAgent(String agent) { this.agent = agent; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getStartedAt() { return startedAt; }
        public void setStartedAt(String startedAt) { this.startedAt = startedAt; }

        public String getCompletedAt() { return completedAt; }
        public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
    }
}
