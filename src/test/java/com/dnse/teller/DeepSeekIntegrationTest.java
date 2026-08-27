package com.dnse.teller;

import com.dnse.teller.orchestrator.DeepSeekClient;
import com.dnse.teller.orchestrator.ReasoningEngine;
import com.dnse.teller.model.Intent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DeepSeekIntegrationTest {

    @Autowired
    private DeepSeekClient deepSeekClient;

    @Autowired
    private ReasoningEngine reasoningEngine;

    @Test
    void testToolsManifestGenerationMatchesOpenAiSchema() {
        List<Map<String, Object>> tools = deepSeekClient.buildToolsManifest();
        assertNotNull(tools);
        assertEquals(17, tools.size());

        Map<String, Object> firstTool = tools.get(0);
        assertEquals("function", firstTool.get("type"));
        assertTrue(firstTool.containsKey("function"));

        @SuppressWarnings("unchecked")
        Map<String, Object> func = (Map<String, Object>) firstTool.get("function");
        assertNotNull(func.get("name"));
        assertNotNull(func.get("description"));
        assertNotNull(func.get("parameters"));
    }

    @Test
    void testDisabledOrUnconfiguredByDefault() {
        // Without supplying a real key in test, isConfiguredAndEnabled must be false
        assertFalse(deepSeekClient.isConfiguredAndEnabled());
        assertTrue(deepSeekClient.reasonAndPlan("chuyển 50tr cho An", Map.of()).isEmpty());
    }

    @Test
    void testDeterministicFallbackWorksSeamlessly() {
        Intent intent = reasoningEngine.detectIntent("chuyển 50 triệu cho Nguyễn Văn An tại Vietcombank STK 0123456789", null);
        assertNotNull(intent);
        assertEquals("DOMESTIC_TRANSFER", intent.getType());
        assertEquals("domestic_transfer", intent.getWorkflow());
        assertEquals(50_000_000L, intent.getEntities().get("amount"));
        assertEquals("VCB", intent.getEntities().get("bankCode"));
        assertEquals("0123456789", intent.getEntities().get("beneficiaryAccount"));
    }
}
