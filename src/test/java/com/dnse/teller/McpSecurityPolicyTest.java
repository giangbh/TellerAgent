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
        assertEquals(22, policies.size());

        McpSecurityPolicy profilePolicy = policyProvider.getPolicy("customer.profile.read");
        assertNotNull(profilePolicy);
        assertEquals("SENSITIVE_READ", profilePolicy.getRiskLevel());
        assertTrue(profilePolicy.getAllowedCallers().contains("customer_context_agent"));
        assertTrue(profilePolicy.getAllowedCallers().contains("dynamic_tool_agent"));
    }

    @Test
    void testHotReloadCanNarrowButNeverWiden() throws Exception {
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
        List<String> originalCallers = List.copyOf(existing.getAllowedCallers());
        List<String> newCallers = new ArrayList<>(existing.getAllowedCallers());
        newCallers.add(customCaller);

        policyProvider.updatePolicy(capabilityId, newCallers, existing.getAllowedWorkflows());

        // 3. Quyền hiệu lực = GIAO của policy trong DB và khai báo cứng của tool.
        //    Nới quyền qua API vận hành KHÔNG có tác dụng — đây là hành vi mong muốn (P0-5).
        assertThrows(McpServer.McpSecurityException.class, () -> mcpServer.executeDirect(
            customCaller, "dynamic_autonomous", capabilityId, Map.of("currency", "USD"), null));

        // 4. Ngược lại, thu hẹp quyền thì có hiệu lực ngay.
        List<String> original = List.copyOf(originalCallers);
        policyProvider.updatePolicy(capabilityId, List.of("nobody"), existing.getAllowedWorkflows());
        assertThrows(McpServer.McpSecurityException.class, () -> mcpServer.executeDirect(
            "dynamic_tool_agent", "dynamic_autonomous", capabilityId, Map.of("currency", "USD"), null));

        // 5. Khôi phục để không ảnh hưởng test khác.
        policyProvider.updatePolicy(capabilityId, original, existing.getAllowedWorkflows());
        assertNotNull(mcpServer.executeDirect(
            "dynamic_tool_agent", "dynamic_autonomous", capabilityId, Map.of("currency", "USD"), null));
    }

    @Test
    void testCorePostingToolsAreExcludedFromMcp() {
        assertNull(policyProvider.getPolicy("core.transfer.execute"));
        assertNull(policyProvider.getPolicy("core.cash.execute"));
    }
}
