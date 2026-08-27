package com.dnse.teller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Plan {
    private String objective;
    private List<PlanStep> steps = new ArrayList<>();
    private List<String> completionCriteria = new ArrayList<>();

    public Plan() {}
    public Plan(String objective, List<PlanStep> steps, List<String> completionCriteria) {
        this.objective = objective;
        this.steps = steps;
        this.completionCriteria = completionCriteria;
    }

    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }

    public List<PlanStep> getSteps() { return steps; }
    public void setSteps(List<PlanStep> steps) { this.steps = steps; }

    public List<String> getCompletionCriteria() { return completionCriteria; }
    public void setCompletionCriteria(List<String> completionCriteria) { this.completionCriteria = completionCriteria; }

    public static class PlanStep {
        private String id;
        private String kind = "DELEGATE";
        private String target;
        private List<String> dependsOn = new ArrayList<>();

        public PlanStep() {}
        public PlanStep(String id, String kind, String target, List<String> dependsOn) {
            this.id = id;
            this.kind = kind;
            this.target = target;
            this.dependsOn = dependsOn;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }

        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }

        public List<String> getDependsOn() { return dependsOn; }
        public void setDependsOn(List<String> dependsOn) { this.dependsOn = dependsOn; }
    }
}
