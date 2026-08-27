package com.dnse.teller;

import com.dnse.teller.gateway.InternalApiGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class InternalApiGatewayTest {

    @Autowired
    private InternalApiGateway gateway;

    @Test
    void testGatewayDispatchSuccessAndTraceId() throws Exception {
        Map<String, Object> result = gateway.dispatchCorePosting(
            "business_orchestrator",
            "domestic_transfer",
            "core.transfer.execute",
            Map.of("amount", 50000000L),
            "IDEMPOTENT-GTW-1"
        );

        assertNotNull(result);
        assertEquals("POSTED", result.get("status"));
        assertNotNull(result.get("coreReference"));
        assertTrue(String.valueOf(result.get("coreReference")).startsWith("FT"));
        assertNotNull(result.get("traceId"));
        assertTrue(String.valueOf(result.get("traceId")).startsWith("TRC-"));

        // Repeated request with same idempotency key returns identical core reference
        Map<String, Object> repeated = gateway.dispatchCorePosting(
            "business_orchestrator",
            "domestic_transfer",
            "core.transfer.execute",
            Map.of("amount", 50000000L),
            "IDEMPOTENT-GTW-1"
        );
        assertEquals(result.get("coreReference"), repeated.get("coreReference"));
    }

    @Test
    void testGatewayRejectsUnauthorizedCaller() {
        assertThrows(SecurityException.class, () -> {
            gateway.dispatchCorePosting(
                "customer_context_agent", // Unauthorized!
                "domestic_transfer",
                "core.transfer.execute",
                Map.of("amount", 50000000L),
                "IDEMPOTENT-GTW-2"
            );
        });
    }
}
