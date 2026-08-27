package com.dnse.teller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RiskAssessment {
    private String decision;
    private List<String> alerts = new ArrayList<>();
    private String model;

    public RiskAssessment() {}
    public RiskAssessment(String decision, List<String> alerts, String model) {
        this.decision = decision;
        this.alerts = alerts != null ? alerts : new ArrayList<>();
        this.model = model;
    }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public List<String> getAlerts() { return alerts; }
    public void setAlerts(List<String> alerts) { this.alerts = alerts; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
