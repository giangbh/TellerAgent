package com.dnse.teller.controller;

import com.dnse.teller.mcp.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/mcp")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:8799", "http://127.0.0.1:8799"})
public class McpController {
    private final McpServer mcpServer;
    private final McpSecurityPolicyProvider policyProvider;

    public McpController(McpServer mcpServer, McpSecurityPolicyProvider policyProvider) {
        this.mcpServer = mcpServer;
        this.policyProvider = policyProvider;
    }

    @PostMapping
    public ResponseEntity<JsonRpcResponse> handleMcpJsonRpc(@RequestBody JsonRpcRequest request) {
        JsonRpcResponse response = mcpServer.handleRequest(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/policies")
    public ResponseEntity<List<McpSecurityPolicy>> getAllPolicies() {
        return ResponseEntity.ok(policyProvider.getAllPolicies());
    }

    @GetMapping("/policies/{capabilityId}")
    public ResponseEntity<McpSecurityPolicy> getPolicy(@PathVariable String capabilityId) {
        McpSecurityPolicy policy = policyProvider.getPolicy(capabilityId);
        if (policy == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(policy);
    }

    @PutMapping("/policies/{capabilityId}")
    public ResponseEntity<McpSecurityPolicy> updatePolicy(
            @PathVariable String capabilityId,
            @RequestBody Map<String, Object> body
    ) {
        @SuppressWarnings("unchecked")
        List<String> allowedCallers = body.get("allowedCallers") instanceof List ? (List<String>) body.get("allowedCallers") : null;
        @SuppressWarnings("unchecked")
        List<String> allowedWorkflows = body.get("allowedWorkflows") instanceof List ? (List<String>) body.get("allowedWorkflows") : null;

        McpSecurityPolicy updated = policyProvider.updatePolicy(capabilityId, allowedCallers, allowedWorkflows);
        return ResponseEntity.ok(updated);
    }
}
