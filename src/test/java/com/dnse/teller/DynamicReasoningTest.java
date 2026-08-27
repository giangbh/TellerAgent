package com.dnse.teller;

import com.dnse.teller.model.Session;
import com.dnse.teller.orchestrator.TellerOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DynamicReasoningTest {

    @Autowired
    private TellerOrchestrator orchestrator;

    @Test
    void testDynamicFxLookupAndConversion() throws Exception {
        Session session = orchestrator.createSession(Map.of("counterId", "Q-07"));
        Session updated = orchestrator.processMessage(session.getSessionId(), "Tỷ giá 2000 EUR hôm nay đổi ra bao nhiêu VND?");

        assertNotNull(updated);
        assertEquals("DYNAMIC_FX_LOOKUP", updated.getIntent().getType());
        assertEquals("dynamic_autonomous", updated.getWorkflow());
        assertEquals("ASSISTANCE_READY", updated.getStatus());

        // Verify Assistant message contains conversion calculation
        assertFalse(updated.getMessages().isEmpty());
        String lastMsg = updated.getMessages().get(updated.getMessages().size() - 1).getText();
        assertTrue(lastMsg.contains("EUR") || lastMsg.contains("VND"));
        assertTrue(lastMsg.contains("2000"));
    }

    @Test
    void testDynamicBranchDirectoryLookup() throws Exception {
        Session session = orchestrator.createSession(Map.of("counterId", "Q-07"));
        Session updated = orchestrator.processMessage(session.getSessionId(), "Địa chỉ và hotline chi nhánh tại Hà Nội ở đâu?");

        assertNotNull(updated);
        assertEquals("DYNAMIC_BRANCH_LOOKUP", updated.getIntent().getType());
        assertEquals("ASSISTANCE_READY", updated.getStatus());

        String lastMsg = updated.getMessages().get(updated.getMessages().size() - 1).getText();
        assertTrue(lastMsg.contains("Hà Nội") || lastMsg.contains("điểm giao dịch"));
    }

    @Test
    void testDynamicCustomerAccountsSummary() throws Exception {
        Session session = orchestrator.createSession(Map.of("customerRef", "CIF-0001842"));
        Session updated = orchestrator.processMessage(session.getSessionId(), "Tổng hợp toàn bộ tài khoản và sổ tiết kiệm của khách");

        assertNotNull(updated);
        assertEquals("DYNAMIC_ACCOUNT_SUMMARY", updated.getIntent().getType());
        assertEquals("ASSISTANCE_READY", updated.getStatus());

        String lastMsg = updated.getMessages().get(updated.getMessages().size() - 1).getText();
        assertTrue(lastMsg.contains("tiết kiệm") || lastMsg.contains("tài khoản"));
    }

    @Test
    void testDynamicCustomerProfileRead() throws Exception {
        Session session = orchestrator.createSession(Map.of("customerRef", "CIF-0001842"));
        Session updated = orchestrator.processMessage(session.getSessionId(), "Tra cứu thông tin khách hàng CIF CIF-0001842");

        assertNotNull(updated);
        assertEquals("ASSISTANCE_READY", updated.getStatus());

        String lastMsg = updated.getMessages().get(updated.getMessages().size() - 1).getText();
        assertTrue(lastMsg.contains("Nguyễn Minh Anh") || lastMsg.contains("PRIORITY"));
    }
}
