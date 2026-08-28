package com.dnse.teller.workflow;

import com.dnse.teller.persistence.WorkflowRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkflowEngine {
    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    private final WorkflowRepository repository;
    private final ObjectMapper objectMapper;
    private final Map<String, WorkflowExecution> activeWorkflows = new ConcurrentHashMap<>();

    public WorkflowEngine(WorkflowRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public WorkflowExecution startWorkflow(String sessionId, String workflowName, Map<String, Object> initialInput) {
        String executionId = "WF-" + sessionId;
        String now = Instant.now().toString();

        WorkflowExecution execution = new WorkflowExecution(executionId, sessionId, workflowName, now);
        if (initialInput != null) {
            execution.getState().putAll(initialInput);
        }

        // Record Initial Event Sourcing entry
        recordEvent(executionId, "WorkflowExecutionStarted", "Workflow " + workflowName + " khởi chạy cho session " + sessionId);

        checkpoint(execution);
        activeWorkflows.put(executionId, execution);
        return execution;
    }

    public WorkflowExecution getOrRestoreWorkflow(String sessionId, String workflowName) {
        String executionId = "WF-" + sessionId;
        if (activeWorkflows.containsKey(executionId)) {
            return activeWorkflows.get(executionId);
        }

        Optional<Map<String, Object>> persisted = repository.findWorkflowExecution(executionId);
        if (persisted.isPresent()) {
            Map<String, Object> row = persisted.get();
            WorkflowExecution execution = new WorkflowExecution();
            execution.setExecutionId((String) row.get("execution_id"));
            execution.setSessionId((String) row.get("session_id"));
            execution.setWorkflowName((String) row.get("workflow_name"));
            execution.setStatus((String) row.get("status"));
            execution.setStartTime((String) row.get("start_time"));
            execution.setCloseTime((String) row.get("close_time"));

            try {
                String stateJson = (String) row.get("state_json");
                if (stateJson != null && !stateJson.isEmpty()) {
                    execution.setState(objectMapper.readValue(stateJson, new TypeReference<Map<String, Object>>() {}));
                }
            } catch (Exception e) {
                log.warn("Không thể deserialize state_json cho workflow {}: {}", executionId, e.getMessage());
            }

            activeWorkflows.put(executionId, execution);
            return execution;
        }

        return startWorkflow(sessionId, workflowName, Map.of());
    }

    public void recordActivityExecution(String executionId, String activityName, String status, Map<String, Object> output) {
        WorkflowExecution exec = activeWorkflows.get(executionId);
        if (exec != null) {
            exec.getState().put("activity_" + activityName, output);
            recordEvent(executionId, "ActivityTaskCompleted", activityName + " status=" + status);
            checkpoint(exec);
        }
    }

    public void signal(String executionId, String signalName, Object payload) {
        WorkflowExecution exec = activeWorkflows.get(executionId);
        if (exec != null) {
            exec.getState().put("signal_" + signalName, payload);
            recordEvent(executionId, "WorkflowExecutionSignaled", "Signal: " + signalName + " payload: " + payload);
            checkpoint(exec);
        }
    }

    public void completeWorkflow(String executionId, Map<String, Object> finalResult) {
        WorkflowExecution exec = activeWorkflows.get(executionId);
        if (exec != null) {
            exec.setStatus("COMPLETED");
            exec.setCloseTime(Instant.now().toString());
            if (finalResult != null) exec.getState().put("finalResult", finalResult);
            recordEvent(executionId, "WorkflowExecutionCompleted", "Workflow hoàn thành thành công.");
            checkpoint(exec);
        }
    }

    private void recordEvent(String executionId, String eventType, String detail) {
        try {
            repository.recordHistoryEvent(executionId, eventType, detail);
        } catch (Exception e) {
            log.error("Lỗi ghi nhận lịch sử event sourcing {} cho workflow {}: {}", eventType, executionId, e.getMessage(), e);
        }
    }

    private void checkpoint(WorkflowExecution execution) {
        try {
            String json = objectMapper.writeValueAsString(execution.getState());
            repository.saveWorkflowExecution(
                execution.getExecutionId(),
                execution.getSessionId(),
                execution.getWorkflowName(),
                execution.getStatus(),
                json
            );
        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi checkpoint workflow {} (Session {}): {}",
                execution.getExecutionId(), execution.getSessionId(), e.getMessage(), e);
            throw new WorkflowPersistenceException("Không thể checkpoint trạng thái workflow " + execution.getExecutionId() + ": " + e.getMessage(), e);
        }
    }

    public static class WorkflowPersistenceException extends RuntimeException {
        public WorkflowPersistenceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
