package com.dnse.teller.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BootstrapResponse {
    private String product = "B.Smart Teller Agent POC (Spring Boot + MCP)";
    private String mode = "SPRING_BOOT_MCP";
    private List<Map<String, Object>> capabilities = new ArrayList<>();
    private List<Scenario> scenarios = new ArrayList<>();

    public BootstrapResponse() {}

    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public List<Map<String, Object>> getCapabilities() { return capabilities; }
    public void setCapabilities(List<Map<String, Object>> capabilities) { this.capabilities = capabilities; }

    public List<Scenario> getScenarios() { return scenarios; }
    public void setScenarios(List<Scenario> scenarios) { this.scenarios = scenarios; }

    public static class Scenario {
        private String id;
        private String name;
        private String prompt;

        public Scenario() {}
        public Scenario(String id, String name, String prompt) {
            this.id = id;
            this.name = name;
            this.prompt = prompt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
    }
}
