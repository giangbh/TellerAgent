package com.dnse.teller.controller;

import com.dnse.teller.model.BootstrapResponse;
import com.dnse.teller.model.Session;
import com.dnse.teller.orchestrator.TellerOrchestrator;
import com.dnse.teller.orchestrator.TellerOrchestrator.OrchestratorException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:8799", "http://127.0.0.1:8799"})
public class SessionController {
    private final TellerOrchestrator orchestrator;

    public SessionController(TellerOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "mode", "SPRING_BOOT_MCP",
            "now", Instant.now().toString()
        ));
    }

    @GetMapping("/bootstrap")
    public ResponseEntity<BootstrapResponse> bootstrap() {
        return ResponseEntity.ok(orchestrator.bootstrap());
    }

    @PostMapping("/sessions")
    public ResponseEntity<Session> createSession(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.createSession(body));
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<Session> getSession(@PathVariable("id") String id) {
        return ResponseEntity.ok(orchestrator.getSession(id));
    }

    @PostMapping("/sessions/{id}/messages")
    public ResponseEntity<Session> postMessage(@PathVariable("id") String id, @RequestBody Map<String, Object> body) throws Exception {
        String text = body != null && body.get("text") != null ? String.valueOf(body.get("text")) : "";
        return ResponseEntity.ok(orchestrator.processMessage(id, text));
    }

    @PostMapping("/sessions/{id}/approvals")
    public ResponseEntity<Session> approve(@PathVariable("id") String id, @RequestBody Map<String, Object> body) {
        String actor = body != null && body.get("actor") != null ? String.valueOf(body.get("actor")) : "";
        return ResponseEntity.ok(orchestrator.approve(id, actor));
    }

    @PostMapping("/sessions/{id}/execute")
    public ResponseEntity<Session> execute(@PathVariable("id") String id) throws Exception {
        return ResponseEntity.ok(orchestrator.execute(id));
    }

    @PostMapping("/sessions/{id}/confirm-and-execute")
    public ResponseEntity<Session> confirmAndExecute(@PathVariable("id") String id) throws Exception {
        return ResponseEntity.ok(orchestrator.confirmAndExecuteCash(id));
    }

    @ExceptionHandler(OrchestratorException.class)
    public ResponseEntity<Map<String, Object>> handleOrchestratorException(OrchestratorException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
            "error", Map.of(
                "code", ex.getCode(),
                "message", ex.getMessage()
            )
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "error", Map.of(
                "code", "INTERNAL_ERROR",
                "message", ex.getMessage() != null ? ex.getMessage() : "Đã có lỗi xảy ra."
            )
        ));
    }
}
