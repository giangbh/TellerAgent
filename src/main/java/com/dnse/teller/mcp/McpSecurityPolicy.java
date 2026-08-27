package com.dnse.teller.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpSecurityPolicy {
    private String capabilityId;
    private String riskLevel = "READ_SAFE";

    @JsonProperty("isSideEffect")
    private boolean isSideEffect = false;

    @JsonProperty("requiresIdempotency")
    private boolean requiresIdempotency = false;

    private List<String> allowedCallers = new ArrayList<>();
    private List<String> allowedWorkflows = new ArrayList<>();
    private String updatedAt;

    public McpSecurityPolicy() {}

    public McpSecurityPolicy(String capabilityId, String riskLevel, boolean isSideEffect, boolean requiresIdempotency, List<String> allowedCallers, List<String> allowedWorkflows) {
        this.capabilityId = capabilityId;
        this.riskLevel = riskLevel;
        this.isSideEffect = isSideEffect;
        this.requiresIdempotency = requiresIdempotency;
        this.allowedCallers = allowedCallers != null ? new ArrayList<>(allowedCallers) : new ArrayList<>();
        this.allowedWorkflows = allowedWorkflows != null ? new ArrayList<>(allowedWorkflows) : new ArrayList<>();
    }

    public String getCapabilityId() { return capabilityId; }
    public void setCapabilityId(String capabilityId) { this.capabilityId = capabilityId; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    @JsonProperty("isSideEffect")
    public boolean isSideEffect() { return isSideEffect; }

    @JsonProperty("isSideEffect")
    public void setSideEffect(boolean sideEffect) { isSideEffect = sideEffect; }

    @JsonProperty("requiresIdempotency")
    public boolean isRequiresIdempotency() { return requiresIdempotency; }

    @JsonProperty("requiresIdempotency")
    public void setRequiresIdempotency(boolean requiresIdempotency) { this.requiresIdempotency = requiresIdempotency; }

    public List<String> getAllowedCallers() { return allowedCallers; }
    public void setAllowedCallers(List<String> allowedCallers) { this.allowedCallers = allowedCallers; }

    public List<String> getAllowedWorkflows() { return allowedWorkflows; }
    public void setAllowedWorkflows(List<String> allowedWorkflows) { this.allowedWorkflows = allowedWorkflows; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
