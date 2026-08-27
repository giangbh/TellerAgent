package com.dnse.teller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ControlGate {
    private boolean postingAllowed = false;
    private List<String> missingGates = new ArrayList<>();
    private String lastEvaluatedAt;

    public ControlGate() {}
    public ControlGate(boolean postingAllowed, List<String> missingGates, String lastEvaluatedAt) {
        this.postingAllowed = postingAllowed;
        this.missingGates = missingGates != null ? missingGates : new ArrayList<>();
        this.lastEvaluatedAt = lastEvaluatedAt;
    }

    public boolean isPostingAllowed() { return postingAllowed; }
    public void setPostingAllowed(boolean postingAllowed) { this.postingAllowed = postingAllowed; }

    public List<String> getMissingGates() { return missingGates; }
    public void setMissingGates(List<String> missingGates) { this.missingGates = missingGates; }

    public String getLastEvaluatedAt() { return lastEvaluatedAt; }
    public void setLastEvaluatedAt(String lastEvaluatedAt) { this.lastEvaluatedAt = lastEvaluatedAt; }
}
