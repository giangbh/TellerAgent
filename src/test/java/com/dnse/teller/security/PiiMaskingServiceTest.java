package com.dnse.teller.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PiiMaskingServiceTest {

    private PiiMaskingService piiMaskingService;

    @BeforeEach
    void setUp() {
        piiMaskingService = new PiiMaskingService(new ObjectMapper());
    }

    @Test
    void testMaskPromptAndRehydrate() {
        String prompt = "Chuyển 50 triệu cho Nguyễn Văn An tại Vietcombank, số tài khoản 012345678901";
        Map<String, Object> entities = Map.of(
            "beneficiaryAccount", "012345678901",
            "beneficiaryName", "Nguyễn Văn An",
            "bankCode", "VCB"
        );

        PiiMaskingService.PiiContext ctx = piiMaskingService.maskPromptAndEntities(prompt, entities);

        String masked = ctx.getMaskedText();
        assertFalse(masked.contains("012345678901"), "Số tài khoản thực tế không được xuất hiện trong prompt gửi sang LLM");
        assertFalse(masked.contains("Nguyễn Văn An"), "Tên khách hàng thực tế không được xuất hiện trong prompt gửi sang LLM");
        assertTrue(masked.contains("<ACCOUNT_"), "Phải chứa token ẩn danh <ACCOUNT_X>");
        assertTrue(masked.contains("<PERSON_NAME_"), "Phải chứa token ẩn danh <PERSON_NAME_X>");

        // Test Unmasking / Re-hydration
        String llmResponse = "Đã tìm thấy tài khoản " + ctx.getTokenToReal().keySet().stream().filter(k -> k.startsWith("<ACCOUNT_")).findFirst().get()
                + " của khách hàng " + ctx.getTokenToReal().keySet().stream().filter(k -> k.startsWith("<PERSON_NAME_")).findFirst().get();

        String unmasked = ctx.unmask(llmResponse);
        assertTrue(unmasked.contains("012345678901"));
        assertTrue(unmasked.contains("Nguyễn Văn An"));
    }

    @Test
    void testMaskCashDepositAndCif() {
        String prompt = "Nộp tiền 50tr vào tài khoản 3456789 của khách hàng CIF-0001842 số điện thoại 0987654321";

        PiiMaskingService.PiiContext ctx = piiMaskingService.maskPromptAndEntities(prompt, Map.of());
        String masked = ctx.getMaskedText();

        assertFalse(masked.contains("3456789"));
        assertFalse(masked.contains("0001842"));
        assertFalse(masked.contains("0987654321"));
        assertTrue(masked.contains("<ACCOUNT_"));
        assertTrue(masked.contains("<CUSTOMER_REF_"));
        assertTrue(masked.contains("<PHONE_"));

        // Unmask
        String restored = ctx.unmask(masked);
        assertTrue(restored.contains("3456789"));
        assertTrue(restored.contains("0987654321"));
    }

    @Test
    void testMaskStructuredToolOutputsJson() {
        Map<String, Object> toolOutputs = Map.of(
            "accountNumber", "3456789",
            "accountHolder", "Nguyễn Minh Anh",
            "customerRef", "CIF-0001842",
            "balance", 150_000_000L
        );

        PiiMaskingService.PiiContext ctx = piiMaskingService.maskToolOutputs("Tra cứu tài khoản 3456789", toolOutputs);

        String json = ctx.getMaskedJson();
        assertFalse(json.contains("3456789"));
        assertFalse(json.contains("Nguyễn Minh Anh"));
        assertFalse(json.contains("CIF-0001842"));
        assertTrue(json.contains("150000000"), "Số dư và các chỉ số thống kê số học được giữ nguyên để LLM phân tích");

        // Unmask Map
        Map<String, Object> mockLlmExtractedArgs = Map.of(
            "accountNumber", ctx.getOrCreateToken("3456789", "ACCOUNT"),
            "amount", 50_000_000L
        );

        Map<String, Object> restoredArgs = ctx.unmaskMap(mockLlmExtractedArgs);
        assertEquals("3456789", restoredArgs.get("accountNumber"));
    }
}
