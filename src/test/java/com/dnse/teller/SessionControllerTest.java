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
        String createRes = mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionId").exists())
            .andReturn().getResponse().getContentAsString();

        String sessionId = com.jayway.jsonpath.JsonPath.read(createRes, "$.sessionId");

        // 2. Post message
        mockMvc.perform(post("/api/sessions/" + sessionId + "/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"nộp tiền 50tr vào tài khoản 3456789\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DRAFT_READY"))
            .andExpect(jsonPath("$.transactionDraft.amount").value(50000000));

        // 3. Confirm and execute
        mockMvc.perform(post("/api/sessions/" + sessionId + "/confirm-and-execute")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("POSTED"))
            .andExpect(jsonPath("$.execution.coreReference").exists());
    }
}
