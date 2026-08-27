package com.dnse.teller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolicyFinding {
    private String answer;
    private List<Citation> citations = new ArrayList<>();
    private Object fee;

    public PolicyFinding() {}
    public PolicyFinding(String answer, List<Citation> citations, Object fee) {
        this.answer = answer;
        this.citations = citations != null ? citations : new ArrayList<>();
        this.fee = fee;
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<Citation> getCitations() { return citations; }
    public void setCitations(List<Citation> citations) { this.citations = citations; }

    public Object getFee() { return fee; }
    public void setFee(Object fee) { this.fee = fee; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Citation {
        private String document;
        private String section;
        private String title;
        private String effectiveDate;

        public Citation() {}
        public Citation(String document, String section, String title, String effectiveDate) {
            this.document = document;
            this.section = section;
            this.title = title;
            this.effectiveDate = effectiveDate;
        }

        public String getDocument() { return document; }
        public void setDocument(String document) { this.document = document; }

        public String getSection() { return section; }
        public void setSection(String section) { this.section = section; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getEffectiveDate() { return effectiveDate; }
        public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }
    }
}
