package com.dnse.teller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder asTeller(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder
            .header("X-Actor-Id", "GDV001")
            .header("X-Actor-Name", "Nguyễn Thị Hà")
            .header("X-Actor-Role", "TELLER")
            .header("X-Actor-Branch", "CN-SGD-01");
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"))
            .andExpect(jsonPath("$.mode").value("SPRING_BOOT_MCP"));
    }

    @Test
    void testBootstrapEndpoint() throws Exception {
        mockMvc.perform(get("/api/bootstrap"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.product").exists())
            .andExpect(jsonPath("$.capabilities").isArray())
            .andExpect(jsonPath("$.scenarios").isArray());
    }

    @Test
    void testSessionLifecycleViaRest() throws Exception {
        // 1. Create session
        String createRes = mockMvc.perform(asTeller(post("/api/sessions"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionId").exists())
            .andReturn().getResponse().getContentAsString();

        String sessionId = com.jayway.jsonpath.JsonPath.read(createRes, "$.sessionId");

        // 2. Post message
        mockMvc.perform(asTeller(post("/api/sessions/" + sessionId + "/messages"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"nộp tiền 50tr vào tài khoản 3456789\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DRAFT_READY"))
            .andExpect(jsonPath("$.transactionDraft.amount").value(50000000));

        // 3. Ghi nhận đồng ý của khách hàng kèm bằng chứng
        mockMvc.perform(asTeller(post("/api/sessions/" + sessionId + "/consent"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"evidenceType\":\"SIGNATURE\",\"evidenceRef\":\"PHIEU-REST-001\"}"))
            .andExpect(status().isOk());

        // 4. GDV xác nhận
        mockMvc.perform(asTeller(post("/api/sessions/" + sessionId + "/approvals"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"teller\"}"))
            .andExpect(status().isOk());

        // 5. Confirm and execute
        mockMvc.perform(asTeller(post("/api/sessions/" + sessionId + "/confirm-and-execute"))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("POSTED"))
            .andExpect(jsonPath("$.execution.coreReference").exists());
    }
}
