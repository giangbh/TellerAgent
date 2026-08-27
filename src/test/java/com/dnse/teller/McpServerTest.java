package com.dnse.teller;

import com.dnse.teller.mcp.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class McpServerTest {

    @Autowired
    private McpServer mcpServer;

    @Autowired
    private McpToolRegistry registry;

    @Test
    void testInitialize() {
        JsonRpcRequest req = new JsonRpcRequest(1, "initialize", Map.of());
        JsonRpcResponse res = mcpServer.handleRequest(req);

        assertNotNull(res);
        assertEquals(1, res.getId());
        assertNull(res.getError());
        assertNotNull(res.getResult());

        Map<?, ?> result = (Map<?, ?>) res.getResult();
        assertEquals("2024-11-05", result.get("protocolVersion"));
    }

    @Test
    void testToolsListContainsAllCapabilities() {
        JsonRpcRequest req = new JsonRpcRequest(2, "tools/list", Map.of());
        JsonRpcResponse res = mcpServer.handleRequest(req);

        assertNotNull(res);
        assertNull(res.getError());

        Map<?, ?> result = (Map<?, ?>) res.getResult();
        List<?> tools = (List<?>) result.get("tools");
        assertEquals(23, tools.size());
    }

    @Test
    void testCallToolSafeRead() {
        JsonRpcRequest req = new JsonRpcRequest(3, "tools/call", Map.of(
            "name", "pricing_transfer_fee",
            "arguments", Map.of("amount", 50_000_000L),
            "caller", "policy_agent",
            "workflow", "domestic_transfer"
        ));
        JsonRpcResponse res = mcpServer.handleRequest(req);

        assertNotNull(res);
        assertNull(res.getError());

        Map<?, ?> result = (Map<?, ?>) res.getResult();
        Map<?, ?> data = (Map<?, ?>) result.get("result");
        assertEquals(50_000_000L, ((Number) data.get("amount")).longValue());
        assertTrue(((Number) data.get("totalFee")).longValue() > 0);
    }

    @Test
    void testRejectUnauthorizedCaller() {
        JsonRpcRequest req = new JsonRpcRequest(4, "tools/call", Map.of(
            "name", "core_transfer_execute",
            "arguments", Map.of("amount", 50_000_000L),
            "caller", "customer_context_agent", // Unauthorized!
            "workflow", "domestic_transfer"
        ));
        JsonRpcResponse res = mcpServer.handleRequest(req);

        assertNotNull(res);
        assertNotNull(res.getError());
        assertEquals(-32001, res.getError().getCode());
        assertTrue(res.getError().getMessage().contains("không được phép gọi"));
    }

    @Test
    void testRejectMissingIdempotencyKeyOnFinancialWrite() {
        JsonRpcRequest req = new JsonRpcRequest(5, "tools/call", Map.of(
            "name", "core_transfer_execute",
            "arguments", Map.of("amount", 50_000_000L, "beneficiaryAccount", "123456"),
            "caller", "business_orchestrator",
            "workflow", "domestic_transfer"
            // Missing idempotencyKey!
        ));
        JsonRpcResponse res = mcpServer.handleRequest(req);

        assertNotNull(res);
        assertNotNull(res.getError());
        assertEquals(-32001, res.getError().getCode());
        assertTrue(res.getError().getMessage().contains("idempotency key"));
    }

    @Test
    void testResourcesListAndRead() {
        JsonRpcRequest listReq = new JsonRpcRequest(6, "resources/list", Map.of());
        JsonRpcResponse listRes = mcpServer.handleRequest(listReq);
        assertNotNull(listRes);
        assertNull(listRes.getError());

        JsonRpcRequest readReq = new JsonRpcRequest(7, "resources/read", Map.of(
            "uri", "policy://documents/QTTM-2026"
        ));
        JsonRpcResponse readRes = mcpServer.handleRequest(readReq);
        assertNotNull(readRes);
        assertNull(readRes.getError());
        Map<?, ?> result = (Map<?, ?>) readRes.getResult();
        List<?> contents = (List<?>) result.get("contents");
        assertFalse(contents.isEmpty());
    }
}
