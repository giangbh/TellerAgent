package com.dnse.teller;

import com.dnse.teller.mcp.McpSecurityPolicyProvider;
import com.dnse.teller.mcp.McpServer;
import com.dnse.teller.model.Session;
import com.dnse.teller.orchestrator.TellerOrchestrator;
import com.dnse.teller.orchestrator.TellerOrchestrator.OrchestratorException;
import com.dnse.teller.security.AuthorizationException;
import com.dnse.teller.security.PostingAuthorizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Bộ test đối kháng: mỗi test tương ứng một lỗ hổng P0 đã được vá.
 *
 * Đây là nhóm test có giá trị nhất cho hệ thống này — test "đường hạnh phúc"
 * không phát hiện được việc một lớp kiểm soát tồn tại nhưng có đường vòng.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AdversarialControlTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private McpServer mcpServer;
    @Autowired private TellerOrchestrator orchestrator;
    @Autowired private McpSecurityPolicyProvider policyProvider;
    @Autowired private PostingAuthorizer postingAuthorizer;

    // ---------- P0-1 ----------

    @Test
    @DisplayName("P0-1: không có tool hạch toán core trong endpoint MCP công khai")
    void financialWriteIsNotReachableOverHttp() throws Exception {
        String payload = """
            {"jsonrpc":"2.0","id":1,"method":"tools/call",
             "params":{"name":"core.transfer.execute","idempotencyKey":"attack-1",
             "arguments":{"amount":5000000000,"beneficiaryAccount":"999999999"}}}
            """;

        mockMvc.perform(post("/api/mcp").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.error.data.code").value("UNKNOWN_TOOL"));
    }

    @Test
    @DisplayName("P0-1: lời gọi tool không khai báo caller/workflow bị từ chối")
    void missingCallerIsRejectedInsteadOfDefaultingToPrivilege() throws Exception {
        String payload = """
            {"jsonrpc":"2.0","id":2,"method":"tools/call",
             "params":{"name":"fx.rate.lookup","arguments":{"currency":"USD"}}}
            """;

        mockMvc.perform(post("/api/mcp").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.error.data.code").value("TOOL_CONTEXT_MISSING"));
    }

    // ---------- P0-2 ----------

    @Test
    @DisplayName("P0-2: agent không thể gọi công cụ ghi sổ tài chính qua MCP Server vì không tồn tại trong MCP")
    void agentCannotPostCoreWithoutAuthorization() {
        McpServer.McpSecurityException ex = assertThrows(McpServer.McpSecurityException.class, () ->
            mcpServer.executeDirect(
                "business_orchestrator", "domestic_transfer", "core.transfer.execute",
                Map.of("amount", 1_000_000L, "beneficiaryAccount", "123"), "forged-key"));

        assertEquals("UNKNOWN_CAPABILITY", ex.getCode());
    }

    // ---------- P0-3 ----------

    @Test
    @DisplayName("P0-3: GDV không thể tự ký mắt thứ hai với vai trò kiểm soát viên")
    void tellerCannotActAsSupervisor() throws Exception {
        Session opened = orchestrator.createSession(Map.of(), TestActors.TELLER);
        orchestrator.processMessage(opened.getSessionId(),
            "Chuyển 50 triệu cho Nguyễn Văn An tại VCB, số tài khoản 012345678901");

        AuthorizationException ex = assertThrows(AuthorizationException.class, () ->
            orchestrator.approve(opened.getSessionId(), "supervisor", TestActors.TELLER));

        assertEquals("ROLE_NOT_PERMITTED", ex.getCode());
    }

    @Test
    @DisplayName("P0-3: không xác thực thì không tạo được phiên")
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("ACTOR_ID_MISSING"));
    }

    // ---------- P0-4 ----------

    @Test
    @DisplayName("P0-4: không tự sinh ra sự đồng ý của khách hàng")
    void cashSubmitRequiresRealCustomerConsent() throws Exception {
        Session opened = orchestrator.createSession(Map.of(), TestActors.TELLER);
        orchestrator.processMessage(opened.getSessionId(), "nộp tiền 50tr vào tài khoản 3456789");

        OrchestratorException ex = assertThrows(OrchestratorException.class, () ->
            orchestrator.confirmAndExecuteCash(opened.getSessionId(), TestActors.TELLER));

        assertEquals("CUSTOMER_CONSENT_MISSING", ex.getCode());
    }

    @Test
    @DisplayName("P0-4: xác nhận khách hàng phải kèm bằng chứng hợp lệ")
    void consentWithoutEvidenceIsRejected() throws Exception {
        Session opened = orchestrator.createSession(Map.of(), TestActors.TELLER);
        orchestrator.processMessage(opened.getSessionId(), "nộp tiền 50tr vào tài khoản 3456789");

        assertThrows(OrchestratorException.class, () ->
            orchestrator.recordCustomerConsent(opened.getSessionId(), TestActors.TELLER, "OTP", "  "));

        assertThrows(OrchestratorException.class, () ->
            orchestrator.recordCustomerConsent(opened.getSessionId(), TestActors.TELLER, "TIN_TUONG", "REF-1"));
    }

    @Test
    @DisplayName("P0-4: GDV khác không thể gửi hạch toán giao dịch của người kia")
    void anotherTellerCannotSubmitSomeoneElsesTransaction() throws Exception {
        Session opened = orchestrator.createSession(Map.of(), TestActors.TELLER);
        orchestrator.processMessage(opened.getSessionId(), "nộp tiền 50tr vào tài khoản 3456789");
        orchestrator.recordCustomerConsent(opened.getSessionId(), TestActors.TELLER, "SIGNATURE", "PHIEU-001");
        orchestrator.approve(opened.getSessionId(), "teller", TestActors.TELLER);

        OrchestratorException ex = assertThrows(OrchestratorException.class, () ->
            orchestrator.execute(opened.getSessionId(), TestActors.OTHER_TELLER));

        assertEquals("TELLER_MISMATCH", ex.getCode());
    }

    // ---------- P0-5 ----------

    @Test
    @DisplayName("P0-5: capability không có policy tường minh thì bị từ chối, không phải cho qua")
    void unknownCapabilityIsDeniedByDefault() {
        assertNull(policyProvider.getPolicy("capability.khong.ton.tai"));
    }

    @Test
    @DisplayName("P0-5: không nới được quyền của capability ghi sổ tài chính lúc runtime")
    void financialWritePolicyCannotBeWidenedAtRuntime() {
        assertThrows(IllegalArgumentException.class, () ->
            policyProvider.updatePolicy("core.transfer.execute",
                java.util.List.of("business_orchestrator", "dynamic_tool_agent"),
                java.util.List.of("domestic_transfer", "dynamic_autonomous")));
    }

    @Test
    @DisplayName("P0-5: sửa policy trong DB không nới được quyền vượt khai báo trong code")
    void policyEditCannotExceedToolDeclaration() throws Exception {
        // fx.rate.lookup khai báo cứng danh sách caller trong code; DB chỉ có thể thu hẹp.
        var existing = policyProvider.getPolicy("fx.rate.lookup");
        var original = java.util.List.copyOf(existing.getAllowedCallers());
        var widened = new java.util.ArrayList<>(original);
        widened.add("ke_tan_cong_agent");
        policyProvider.updatePolicy("fx.rate.lookup", widened, existing.getAllowedWorkflows());
        try {
            assertThrows(McpServer.McpSecurityException.class, () ->
                mcpServer.executeDirect("ke_tan_cong_agent", "dynamic_autonomous",
                    "fx.rate.lookup", Map.of("currency", "USD"), null));
        } finally {
            policyProvider.updatePolicy("fx.rate.lookup", original, existing.getAllowedWorkflows());
        }
    }

    // ---------- P1-5 ----------

    @Test
    @DisplayName("P1-5: idempotency key là hàm xác định của nội dung nghiệp vụ")
    void idempotencyKeyIsDeterministic() throws Exception {
        Session opened = orchestrator.createSession(Map.of(), TestActors.TELLER);
        Session draft = orchestrator.processMessage(opened.getSessionId(), "nộp tiền 50tr vào tài khoản 3456789");

        String first = PostingAuthorizer.deriveIdempotencyKey(draft);
        String second = PostingAuthorizer.deriveIdempotencyKey(draft);

        assertEquals(first, second);
        assertTrue(first.startsWith("IDK-"));
    }

    // ---------- Đường hạnh phúc vẫn phải chạy được ----------

    @Test
    @DisplayName("Luồng đúng: consent có bằng chứng → GDV duyệt → hạch toán thành công")
    void happyPathStillWorks() throws Exception {
        Session opened = orchestrator.createSession(Map.of(), TestActors.TELLER);
        orchestrator.processMessage(opened.getSessionId(), "nộp tiền 50tr vào tài khoản 3456789");
        orchestrator.recordCustomerConsent(opened.getSessionId(), TestActors.TELLER, "SIGNATURE", "PHIEU-002");
        orchestrator.approve(opened.getSessionId(), "teller", TestActors.TELLER);

        Session posted = orchestrator.execute(opened.getSessionId(), TestActors.TELLER);

        assertEquals("POSTED", posted.getStatus());
        assertTrue(posted.getExecution().getCoreReference().startsWith("CD"));
        assertEquals("GDV001", posted.getApprovals().getTellerRecord().getActorId());
        assertEquals("SIGNATURE", posted.getApprovals().getCustomerRecord().getEvidenceType());

        // Idempotency: gọi lại trả về đúng bút toán cũ
        Session repeated = orchestrator.execute(opened.getSessionId(), TestActors.TELLER);
        assertEquals(posted.getExecution().getCoreReference(), repeated.getExecution().getCoreReference());
    }
}
