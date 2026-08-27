package com.dnse.teller.persistence;

import com.dnse.teller.model.Session;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;

@Repository
public class WorkflowRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public WorkflowRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    // 1. Session Persistence
    public void saveSession(Session session) {
        try {
            String json = objectMapper.writeValueAsString(session);
            String sql = """
                INSERT INTO sessions (session_id, branch_id, counter_id, teller_id, customer_ref, workflow, status, revision, data_json, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(session_id) DO UPDATE SET
                    workflow = excluded.workflow,
                    status = excluded.status,
                    revision = excluded.revision,
                    data_json = excluded.data_json,
                    updated_at = excluded.updated_at;
            """;
            jdbcTemplate.update(sql,
                session.getSessionId(),
                session.getBranchId(),
                session.getCounterId(),
                session.getTellerId(),
                session.getCustomerRef(),
                session.getWorkflow(),
                session.getStatus(),
                session.getRevision(),
                json,
                session.getCreatedAt(),
                session.getUpdatedAt()
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi lưu session vào SQLite: " + e.getMessage(), e);
        }
    }

    public Optional<Session> findSessionById(String sessionId) {
        String sql = "SELECT data_json FROM sessions WHERE session_id = ?";
        List<Session> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            try {
                return objectMapper.readValue(rs.getString("data_json"), Session.class);
            } catch (Exception e) {
                return null;
            }
        }, sessionId);

        return list.stream().filter(Objects::nonNull).findFirst();
    }

    public List<Session> listAllSessions() {
        String sql = "SELECT data_json FROM sessions ORDER BY updated_at DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            try {
                return objectMapper.readValue(rs.getString("data_json"), Session.class);
            } catch (Exception e) {
                return null;
            }
        }).stream().filter(Objects::nonNull).toList();
    }

    // 2. Temporal Workflow Execution Persistence
    public void saveWorkflowExecution(String executionId, String sessionId, String workflowName, String status, String stateJson) {
        String now = Instant.now().toString();
        String sql = """
            INSERT INTO workflow_executions (execution_id, session_id, workflow_name, status, start_time, close_time, state_json)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(execution_id) DO UPDATE SET
                status = excluded.status,
                close_time = excluded.close_time,
                state_json = excluded.state_json;
        """;
        jdbcTemplate.update(sql,
            executionId,
            sessionId,
            workflowName,
            status,
            now,
            "COMPLETED".equals(status) || "FAILED".equals(status) ? now : null,
            stateJson
        );
    }

    public Optional<Map<String, Object>> findWorkflowExecution(String executionId) {
        String sql = "SELECT * FROM workflow_executions WHERE execution_id = ?";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, executionId);
        return list.stream().findFirst();
    }

    // 3. Event Sourcing History Replay
    public void recordHistoryEvent(String executionId, String eventType, String eventData) {
        String sql = "INSERT INTO workflow_history_events (execution_id, event_type, event_data, created_at) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, executionId, eventType, eventData, Instant.now().toString());
    }

    public List<Map<String, Object>> getHistoryEvents(String executionId) {
        String sql = "SELECT * FROM workflow_history_events WHERE execution_id = ? ORDER BY id ASC";
        return jdbcTemplate.queryForList(sql, executionId);
    }

    // 4. Immutable Audit Log & Idempotency Ledger
    public void recordAuditLog(String traceId, String caller, String capabilityId, String idempotencyKey, String resultStatus, String payloadJson) {
        String sql = """
            INSERT INTO audit_logs (trace_id, caller, capability_id, idempotency_key, result_status, payload_json, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.update(sql, traceId, caller, capabilityId, idempotencyKey, resultStatus, payloadJson, Instant.now().toString());
    }

    public Optional<Map<String, Object>> findAuditByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) return Optional.empty();
        String sql = "SELECT * FROM audit_logs WHERE idempotency_key = ? ORDER BY id ASC LIMIT 1";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, idempotencyKey);
        return list.stream().findFirst();
    }
}
