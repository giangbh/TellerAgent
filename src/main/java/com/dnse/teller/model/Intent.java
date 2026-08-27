package com.dnse.teller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Intent {
    private String type;
    private String workflow;
    private double confidence;
    private Map<String, Object> entities = new LinkedHashMap<>();
    private String query;

    public Intent() {}
    public Intent(String type, String workflow, double confidence, Map<String, Object> entities) {
        this.type = type;
        this.workflow = workflow;
        this.confidence = confidence;
        this.entities = entities;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getWorkflow() { return workflow; }
    public void setWorkflow(String workflow) { this.workflow = workflow; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public Map<String, Object> getEntities() { return entities; }
    public void setEntities(Map<String, Object> entities) { this.entities = entities; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
}
