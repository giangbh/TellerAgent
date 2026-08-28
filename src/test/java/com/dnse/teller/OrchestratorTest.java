package com.dnse.teller;

import com.dnse.teller.model.Session;
import com.dnse.teller.orchestrator.TellerOrchestrator;
import com.dnse.teller.orchestrator.TellerOrchestrator.OrchestratorException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OrchestratorTest {

    @Autowired
    private TellerOrchestrator orchestrator;

    @Test
    void testDomesticTransferCompleteFlow() throws Exception {
        Session opened = orchestrator.createSession(Map.of(), TestActors.TELLER);
        Session state = orchestrator.processMessage(
            opened.getSessionId(),
            "Chuyển 50 triệu cho Nguyễn Văn An tại Vietcombank, số tài khoản 012345678901"
        );

        assertEquals("domestic_transfer", state.getWorkflow());
        assertEquals("DRAFT_READY", state.getStatus());
        assertEquals(50_000_000L, state.getTransactionDraft().getAmount());
        assertEquals("012345678901", state.getTransactionDraft().getBeneficiaryAccount());
        assertEquals("VCB", state.getTransactionDraft().getBankCode());
        assertEquals("Ngân hàng TMCP Ngoại thương Việt Nam", state.getTransactionDraft().getBankName());
        assertTrue(state.getTransactionDraft().getValidation().getValid());
        assertEquals("PASS", state.getRisk().getDecision());
        assertEquals(4, state.getAgentRuntime().getCompletedSteps().size());
        assertFalse(state.getControl().isPostingAllowed());
    }

    @Test
    void testMissingFieldsInterruptAndResume() throws Exception {
        Session opened = orchestrator.createSession(Map.of(), TestActors.TELLER);
        Session waiting = orchestrator.processMessage(opened.getSessionId(), "Tôi muốn chuyển 25 triệu sang Vietcombank");

        assertEquals("WAITING_HUMAN_INPUT", waiting.getStatus());
        assertTrue(waiting.getTransactionDraft().getValidation().getMissingFields().contains("beneficiaryAccount"));
        assertTrue(waiting.getTransactionDraft().getValidation().getMissingFields().contains("beneficiaryName"));

        Session resumed = orchestrator.processMessage(opened.getSessionId(), "Chuyển cho Lê Minh Quân số tài khoản 123456789012");
        assertEquals("DRAFT_READY", resumed.getStatus());
        assertEquals("Lê Minh Quân", resumed.getTransactionDraft().getBeneficiaryName());
        assertEquals("123456789012", resumed.getTransactionDraft().getBeneficiaryAccount());
        assertEquals(25_000_000L, resumed.getTransactionDraft().getAmount());
    }

    @Test
    void testMakerCheckerApprovalAndCoreExecution() throws Exception {
        Session opened = orchestrator.createSession(Map.of(), TestActors.TELLER);
        orchestrator.processMessage(opened.getSessionId(), "Chuyển 50 triệu cho Nguyễn Văn An tại VCB, số tài khoản 012345678901");

        // Teller cannot approve before customer consent is recorded
        assertThrows(OrchestratorException.class,
            () -> orchestrator.approve(opened.getSessionId(), "teller", TestActors.TELLER));

        Session cApproved = orchestrator.recordCustomerConsent(
            opened.getSessionId(), TestActors.TELLER, "OTP", "OTP-884211");
        assertFalse(cApproved.getControl().isPostingAllowed());

        Session tApproved = orchestrator.approve(opened.getSessionId(), "teller", TestActors.TELLER);
        assertTrue(tApproved.getControl().isPostingAllowed());
        assertEquals("READY_TO_POST", tApproved.getStatus());

        Session posted = orchestrator.execute(opened.getSessionId(), TestActors.TELLER);
        assertEquals("POSTED", posted.getStatus());
        assertNotNull(posted.getExecution());
        assertTrue(posted.getExecution().getCoreReference().startsWith("FT"));

        // Idempotency: execute again returns identical reference
        Session repeated = orchestrator.execute(opened.getSessionId(), TestActors.TELLER);
        assertEquals(posted.getExecution().getCoreReference(), repeated.getExecution().getCoreReference());
    }

    @Test
    void testRiskReviewBlocksPosting() throws Exception {
        Session opened = orchestrator.createSession(Map.of(), TestActors.TELLER);
        Session state = orchestrator.processMessage(
            opened.getSessionId(),
            "Chuyển 220 triệu cho Trần Thị Lan tại BIDV, số tài khoản 123456789999"
        );

        assertEquals("RISK_REVIEW", state.getStatus());
        assertEquals("REVIEW", state.getRisk().getDecision());
        assertFalse(state.getControl().isPostingAllowed());
        assertThrows(OrchestratorException.class,
            () -> orchestrator.recordCustomerConsent(opened.getSessionId(), TestActors.TELLER, "OTP", "OTP-1"));
    }

    @Test
    void testPolicyAssistance() throws Exception {
        Session opened = orchestrator.createSession(Map.of(), TestActors.TELLER);
        Session state = orchestrator.processMessage(opened.getSessionId(), "Quy trình và biểu phí chuyển khoản tại quầy như thế nào?");

        assertEquals("ASSISTANCE_READY", state.getStatus());
        assertEquals(1, state.getAgentRuntime().getDelegations().size());
        assertEquals("policy_agent", state.getAgentRuntime().getDelegations().get(0).getAgent());
        assertFalse(state.getPolicyFindings().getCitations().isEmpty());
    }

    @Test
    void testCashDepositAutoFillAndConfirm() throws Exception {
        Session opened = orchestrator.createSession(Map.of(), TestActors.TELLER);
        Session draft = orchestrator.processMessage(opened.getSessionId(), "nộp tiền 50tr vào tài khoản 3456789");

        assertEquals("CASH_DEPOSIT", draft.getIntent().getType());
        assertEquals("cash_deposit", draft.getWorkflow());
        assertEquals("DRAFT_READY", draft.getStatus());
        assertEquals("CASH_DEPOSIT_SCREEN", draft.getTransactionDraft().getScreenCode());
        assertEquals(50_000_000L, draft.getTransactionDraft().getAmount());
        assertEquals("3456789", draft.getTransactionDraft().getAccountNumber());
        assertEquals("Nguyễn Minh Anh", draft.getTransactionDraft().getAccountHolder());
        assertTrue(draft.getTransactionDraft().getValidation().getValid());

        orchestrator.recordCustomerConsent(opened.getSessionId(), TestActors.TELLER, "SIGNATURE", "PHIEU-100");
        orchestrator.approve(opened.getSessionId(), "teller", TestActors.TELLER);

        Session posted = orchestrator.confirmAndExecuteCash(opened.getSessionId(), TestActors.TELLER);
        assertEquals("POSTED", posted.getStatus());
        assertEquals("GDV001", posted.getApprovals().getTellerRecord().getActorId());
        assertEquals("GDV001", posted.getApprovals().getCustomerRecord().getRecordedByActorId());
        assertTrue(posted.getExecution().getCoreReference().startsWith("CD"));
    }

    @Test
    void testCashWithdrawalAutoFillAndLimitCheck() throws Exception {
        Session opened = orchestrator.createSession(Map.of(), TestActors.TELLER);
        Session draft = orchestrator.processMessage(opened.getSessionId(), "rút tiền 50tr từ tài khoản 3456789");

        assertEquals("CASH_WITHDRAWAL", draft.getIntent().getType());
        assertEquals("cash_withdrawal", draft.getWorkflow());
        assertEquals("CASH_WITHDRAWAL_SCREEN", draft.getTransactionDraft().getScreenCode());
        assertEquals(50_000_000L, draft.getTransactionDraft().getAmount());
        assertEquals("3456789", draft.getTransactionDraft().getAccountNumber());
        assertEquals(Boolean.TRUE, draft.getTransactionDraft().getLimit().get("sufficientFunds"));

        orchestrator.recordCustomerConsent(opened.getSessionId(), TestActors.TELLER, "SIGNATURE", "PHIEU-101");
        orchestrator.approve(opened.getSessionId(), "teller", TestActors.TELLER);

        Session posted = orchestrator.confirmAndExecuteCash(opened.getSessionId(), TestActors.TELLER);
        assertEquals("POSTED", posted.getStatus());
        assertTrue(posted.getExecution().getCoreReference().startsWith("CW"));
    }
}
