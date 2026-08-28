package com.dnse.teller.controller;

import com.dnse.teller.mcp.*;
import com.dnse.teller.security.ActorResolver;
import com.dnse.teller.security.AuthenticatedActor;
import com.dnse.teller.security.AuthorizationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/mcp")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:8799", "http://127.0.0.1:8799"})
public class McpController {
    private final McpServer mcpServer;
    private final McpSecurityPolicyProvider policyProvider;
    private final ActorResolver actorResolver;

    public McpController(McpServer mcpServer, McpSecurityPolicyProvider policyProvider, ActorResolver actorResolver) {
        this.mcpServer = mcpServer;
        this.policyProvider = policyProvider;
        this.actorResolver = actorResolver;
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

    /**
     * P0-5: endpoint này trước đây không xác thực, cho phép bất kỳ ai ghi đè
     * allowedCallers của chính core.transfer.execute.
     */
    @PutMapping("/policies/{capabilityId}")
    public ResponseEntity<McpSecurityPolicy> updatePolicy(
            @PathVariable String capabilityId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        AuthenticatedActor actor = actorResolver.resolve(request);
        actor.requireRole(AuthenticatedActor.Role.SECURITY_ADMIN);
        @SuppressWarnings("unchecked")
        List<String> allowedCallers = body.get("allowedCallers") instanceof List ? (List<String>) body.get("allowedCallers") : null;
        @SuppressWarnings("unchecked")
        List<String> allowedWorkflows = body.get("allowedWorkflows") instanceof List ? (List<String>) body.get("allowedWorkflows") : null;

        McpSecurityPolicy updated = policyProvider.updatePolicy(capabilityId, allowedCallers, allowedWorkflows);
        return ResponseEntity.ok(updated);
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthorizationException(AuthorizationException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
            "error", Map.of("code", ex.getCode(), "message", ex.getMessage())
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(400).body(Map.of(
            "error", Map.of("code", "POLICY_UPDATE_REJECTED", "message", ex.getMessage())
        ));
    }
}
