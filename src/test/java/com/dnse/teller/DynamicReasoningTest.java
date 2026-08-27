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

    @Test
    void testDynamicNboProductRecommendation() throws Exception {
        Session session = orchestrator.createSession(Map.of("customerRef", "CIF-0001842"));
        Session updated = orchestrator.processMessage(session.getSessionId(), "Tư vấn gợi ý gói sản phẩm ưu đãi và offer phù hợp cho khách hàng");

        assertNotNull(updated);
        assertEquals("DYNAMIC_NBO_LOOKUP", updated.getIntent().getType());
        assertEquals("ASSISTANCE_READY", updated.getStatus());

        String lastMsg = updated.getMessages().get(updated.getMessages().size() - 1).getText();
        assertTrue(lastMsg.contains("Offer") || lastMsg.contains("Signature") || lastMsg.contains("Tiết kiệm"));
    }

    @Test
    void testDynamicSavingsProductAdvisor() throws Exception {
        Session session = orchestrator.createSession(Map.of("counterId", "Q-07"));
        Session updated = orchestrator.processMessage(session.getSessionId(), "Khách muốn gửi 200 triệu kỳ hạn 6 tháng thì tính lãi bao nhiêu?");

        assertNotNull(updated);
        assertEquals("DYNAMIC_SAVINGS_ADVISE", updated.getIntent().getType());
        assertEquals("ASSISTANCE_READY", updated.getStatus());

        String lastMsg = updated.getMessages().get(updated.getMessages().size() - 1).getText();
        assertTrue(lastMsg.contains("lãi") || lastMsg.contains("200 tr") || lastMsg.contains("đáo hạn"));
    }

    @Test
    void testDynamicCustomerPersonaAnalytics() throws Exception {
        Session session = orchestrator.createSession(Map.of("customerRef", "CIF-0001842"));
        Session updated = orchestrator.processMessage(session.getSessionId(), "Phân tích chân dung khách hàng và hành vi chi tiêu");

        assertNotNull(updated);
        assertEquals("DYNAMIC_PERSONA_LOOKUP", updated.getIntent().getType());
        assertEquals("ASSISTANCE_READY", updated.getStatus());

        String lastMsg = updated.getMessages().get(updated.getMessages().size() - 1).getText();
        assertTrue(lastMsg.contains("Chân dung") || lastMsg.contains("khẩu vị") || lastMsg.contains("kênh"));
    }

    @Test
    void testDynamicCreditScoreCheck() throws Exception {
        Session session = orchestrator.createSession(Map.of("customerRef", "CIF-0001842"));
        Session updated = orchestrator.processMessage(session.getSessionId(), "Kiểm tra điểm tín dụng CIC và hạn mức vay khả dụng");

        assertNotNull(updated);
        assertEquals("DYNAMIC_CREDIT_SCORE_LOOKUP", updated.getIntent().getType());
        assertEquals("ASSISTANCE_READY", updated.getStatus());

        String lastMsg = updated.getMessages().get(updated.getMessages().size() - 1).getText();
        assertTrue(lastMsg.contains("Tín dụng") || lastMsg.contains("CIC") || lastMsg.contains("Hạn mức"));
    }

    @Test
    void testDynamicTransactionHistory() throws Exception {
        Session session = orchestrator.createSession(Map.of("customerRef", "CIF-0001842"));
        Session updated = orchestrator.processMessage(session.getSessionId(), "Trích lục lịch sử giao dịch sao kê của tài khoản 3456789");

        assertNotNull(updated);
        assertEquals("DYNAMIC_TRANSACTION_HISTORY", updated.getIntent().getType());
        assertEquals("ASSISTANCE_READY", updated.getStatus());

        String lastMsg = updated.getMessages().get(updated.getMessages().size() - 1).getText();
        assertTrue(lastMsg.contains("Sao kê") || lastMsg.contains("giao dịch") || lastMsg.contains("3456789"));
    }
}
