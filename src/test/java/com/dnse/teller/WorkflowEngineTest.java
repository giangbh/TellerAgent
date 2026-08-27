package com.dnse.teller;

import com.dnse.teller.model.Session;
import com.dnse.teller.orchestrator.TellerOrchestrator;
import com.dnse.teller.persistence.WorkflowRepository;
import com.dnse.teller.workflow.WorkflowEngine;
import com.dnse.teller.workflow.WorkflowExecution;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class WorkflowEngineTest {

    @Autowired
    private WorkflowEngine workflowEngine;

    @Autowired
    private WorkflowRepository repository;

    @Autowired
    private TellerOrchestrator orchestrator;

    @Test
    void testWorkflowLifecycleAndCheckpointing() {
        String sessionId = UUID.randomUUID().toString();
        WorkflowExecution exec = workflowEngine.startWorkflow(sessionId, "transfer_lifecycle", Map.of("amount", 50000000));

        assertNotNull(exec);
        assertEquals("RUNNING", exec.getStatus());
        assertEquals("WF-" + sessionId, exec.getExecutionId());

        // Record Activity
        workflowEngine.recordActivityExecution(exec.getExecutionId(), "CustomerContextActivity", "COMPLETED", Map.of("cif", "CIF-0001842"));

        // Send Signal
        workflowEngine.signal(exec.getExecutionId(), "CustomerApproval", true);

        // Complete
        workflowEngine.completeWorkflow(exec.getExecutionId(), Map.of("coreRef", "FT00000001"));
        assertEquals("COMPLETED", exec.getStatus());

        // Verify SQLite Persistence
        Optional<Map<String, Object>> persisted = repository.findWorkflowExecution(exec.getExecutionId());
        assertTrue(persisted.isPresent());
        assertEquals("COMPLETED", persisted.get().get("status"));

        List<Map<String, Object>> events = repository.getHistoryEvents(exec.getExecutionId());
        assertFalse(events.isEmpty());
        assertTrue(events.stream().anyMatch(e -> "WorkflowExecutionStarted".equals(e.get("event_type"))));
        assertTrue(events.stream().anyMatch(e -> "ActivityTaskCompleted".equals(e.get("event_type"))));
        assertTrue(events.stream().anyMatch(e -> "WorkflowExecutionSignaled".equals(e.get("event_type"))));
        assertTrue(events.stream().anyMatch(e -> "WorkflowExecutionCompleted".equals(e.get("event_type"))));
    }

    @Test
    void testSessionCrashRecoveryFromSqlite() throws Exception {
        // 1. Create a session and process a message
        Session opened = orchestrator.createSession(Map.of("customerRef", "CIF-TEST-PERSIST"));
        orchestrator.processMessage(opened.getSessionId(), "nộp tiền 50tr vào tài khoản 3456789");

        // 2. Query direct from SQLite database
        Optional<Session> fromDb = repository.findSessionById(opened.getSessionId());
        assertTrue(fromDb.isPresent());
        assertEquals("CIF-TEST-PERSIST", fromDb.get().getCustomerRef());
        assertEquals("DRAFT_READY", fromDb.get().getStatus());
        assertEquals(50000000L, fromDb.get().getTransactionDraft().getAmount());

        // 3. Retrieve through Orchestrator (simulating memory reload)
        Session retrieved = orchestrator.getSession(opened.getSessionId());
        assertNotNull(retrieved);
        assertEquals(opened.getSessionId(), retrieved.getSessionId());
        assertEquals("DRAFT_READY", retrieved.getStatus());
    }
}
