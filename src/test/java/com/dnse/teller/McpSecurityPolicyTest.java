package com.dnse.teller;

import com.dnse.teller.mcp.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class McpSecurityPolicyTest {

    @Autowired
    private McpServer mcpServer;

    @Autowired
    private McpSecurityPolicyProvider policyProvider;

    @Test
    void testPoliciesLoadedFromSeedJsonAndDatabase() {
        List<McpSecurityPolicy> policies = policyProvider.getAllPolicies();
        assertNotNull(policies);
        assertEquals(23, policies.size());

        McpSecurityPolicy profilePolicy = policyProvider.getPolicy("customer.profile.read");
        assertNotNull(profilePolicy);
        assertEquals("SENSITIVE_READ", profilePolicy.getRiskLevel());
        assertTrue(profilePolicy.getAllowedCallers().contains("customer_context_agent"));
        assertTrue(profilePolicy.getAllowedCallers().contains("dynamic_tool_agent"));
    }

    @Test
    void testDynamicPolicyHotReloadWithoutRestart() throws Exception {
        String capabilityId = "fx.rate.lookup";
        String customCaller = "special_auditor_agent_" + UUID.randomUUID().toString().substring(0, 6);

        // 1. Initially, custom caller is rejected
        assertThrows(McpServer.McpSecurityException.class, () -> {
            mcpServer.executeDirect(
                customCaller,
                "dynamic_autonomous",
                capabilityId,
                Map.of("currency", "USD"),
                null
            );
        });

        // 2. Hot-Reload: Admin grants permission to custom caller dynamically
        McpSecurityPolicy existing = policyProvider.getPolicy(capabilityId);
        List<String> newCallers = new ArrayList<>(existing.getAllowedCallers());
        newCallers.add(customCaller);

        policyProvider.updatePolicy(capabilityId, newCallers, existing.getAllowedWorkflows());

        // 3. Immediately after hot-reload, custom caller is accepted without server restart!
        Map<String, Object> result = mcpServer.executeDirect(
            customCaller,
            "dynamic_autonomous",
            capabilityId,
            Map.of("currency", "USD"),
            null
        );

        assertNotNull(result);
        assertEquals(capabilityId, result.get("capabilityId"));
        assertEquals(customCaller, result.get("caller"));
        assertNotNull(result.get("result"));
    }

    @Test
    void testFinancialWritePolicyEnforcesIdempotencyAndCaller() {
        McpSecurityPolicy transferPolicy = policyProvider.getPolicy("core.transfer.execute");
        assertNotNull(transferPolicy);
        assertEquals("FINANCIAL_WRITE", transferPolicy.getRiskLevel());
        assertTrue(transferPolicy.isRequiresIdempotency());
        assertTrue(transferPolicy.isSideEffect());
        assertEquals(List.of("business_orchestrator"), transferPolicy.getAllowedCallers());
    }
}
