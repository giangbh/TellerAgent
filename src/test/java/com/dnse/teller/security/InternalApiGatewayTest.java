package com.dnse.teller.security;

import com.dnse.teller.gateway.InternalApiGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class InternalApiGatewayTest {

    @Autowired
    private InternalApiGateway gateway;

    @Test
    void testGatewayRejectsNullAuthorization() {
        assertThrows(AuthorizationException.class, () -> {
            gateway.dispatchCorePosting(null, "core.transfer.execute", Map.of("amount", 50000000L));
        });
    }

    @Test
    void testGatewayRejectsTamperedAmount() {
        PostingAuthorization auth = new PostingAuthorization(
            "sess-1", "domestic_transfer", "core.transfer.execute",
            "IDEM-KEY-1", 50000000L, "GDV001", null, 1, Instant.now().toString()
        );

        AuthorizationException ex = assertThrows(AuthorizationException.class, () -> {
            gateway.dispatchCorePosting(auth, "core.transfer.execute", Map.of("amount", 99999999L)); // Tampered amount
        });
        assertEquals("AMOUNT_TAMPERED", ex.getCode());
    }

    @Test
    void testGatewayRejectsCapabilityMismatch() {
        PostingAuthorization auth = new PostingAuthorization(
            "sess-1", "domestic_transfer", "core.transfer.execute",
            "IDEM-KEY-1", 50000000L, "GDV001", null, 1, Instant.now().toString()
        );

        AuthorizationException ex = assertThrows(AuthorizationException.class, () -> {
            gateway.dispatchCorePosting(auth, "core.cash.execute", Map.of("amount", 50000000L)); // Mismatch
        });
        assertEquals("CAPABILITY_MISMATCH", ex.getCode());
    }
}
