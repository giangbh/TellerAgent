package com.dnse.teller.controller;

import com.dnse.teller.model.BootstrapResponse;
import com.dnse.teller.model.Session;
import com.dnse.teller.orchestrator.TellerOrchestrator;
import com.dnse.teller.orchestrator.TellerOrchestrator.OrchestratorException;
import com.dnse.teller.security.ActorResolver;
import com.dnse.teller.security.AuthenticatedActor;
import com.dnse.teller.security.AuthorizationException;
import jakarta.servlet.http.HttpServletRequest;
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
    private final ActorResolver actorResolver;

    public SessionController(TellerOrchestrator orchestrator, ActorResolver actorResolver) {
        this.orchestrator = orchestrator;
        this.actorResolver = actorResolver;
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
    public ResponseEntity<Session> createSession(@RequestBody(required = false) Map<String, Object> body,
                                                 HttpServletRequest request) {
        AuthenticatedActor actor = actorResolver.resolve(request);
        actor.requireRole(AuthenticatedActor.Role.TELLER);
        return ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.createSession(body, actor));
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<Session> getSession(@PathVariable("id") String id, HttpServletRequest request) {
        actorResolver.resolve(request);
        return ResponseEntity.ok(orchestrator.getSession(id));
    }

    @PostMapping("/sessions/{id}/messages")
    public ResponseEntity<Session> postMessage(@PathVariable("id") String id,
                                               @RequestBody Map<String, Object> body,
                                               HttpServletRequest request) throws Exception {
        actorResolver.resolve(request);
        String text = body != null && body.get("text") != null ? String.valueOf(body.get("text")) : "";
        return ResponseEntity.ok(orchestrator.processMessage(id, text));
    }

    /**
     * P0-4: xác nhận của khách hàng là một endpoint riêng, bắt buộc kèm bằng
     * chứng, và được ghi nhận dưới danh tính của GDV thực hiện.
     */
    @PostMapping("/sessions/{id}/consent")
    public ResponseEntity<Session> recordConsent(@PathVariable("id") String id,
                                                 @RequestBody Map<String, Object> body,
                                                 HttpServletRequest request) {
        AuthenticatedActor actor = actorResolver.resolve(request);
        String evidenceType = body != null && body.get("evidenceType") != null ? String.valueOf(body.get("evidenceType")) : null;
        String evidenceRef = body != null && body.get("evidenceRef") != null ? String.valueOf(body.get("evidenceRef")) : null;
        return ResponseEntity.ok(orchestrator.recordCustomerConsent(id, actor, evidenceType, evidenceRef));
    }

    @PostMapping("/sessions/{id}/approvals")
    public ResponseEntity<Session> approve(@PathVariable("id") String id,
                                           @RequestBody Map<String, Object> body,
                                           HttpServletRequest request) {
        AuthenticatedActor actor = actorResolver.resolve(request);
        // "role" chỉ nói người dùng MUỐN ký với tư cách gì; quyền thực tế lấy từ actor.
        String requestedRole = body != null && body.get("role") != null ? String.valueOf(body.get("role")) : "";
        return ResponseEntity.ok(orchestrator.approve(id, requestedRole, actor));
    }

    @PostMapping("/sessions/{id}/execute")
    public ResponseEntity<Session> execute(@PathVariable("id") String id, HttpServletRequest request) throws Exception {
        AuthenticatedActor actor = actorResolver.resolve(request);
        return ResponseEntity.ok(orchestrator.execute(id, actor));
    }

    @PostMapping("/sessions/{id}/confirm-and-execute")
    public ResponseEntity<Session> confirmAndExecute(@PathVariable("id") String id, HttpServletRequest request) throws Exception {
        AuthenticatedActor actor = actorResolver.resolve(request);
        return ResponseEntity.ok(orchestrator.confirmAndExecuteCash(id, actor));
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthorizationException(AuthorizationException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
            "error", Map.of("code", ex.getCode(), "message", ex.getMessage())
        ));
    }

    @ExceptionHandler(OrchestratorException.class)
    public ResponseEntity<Map<String, Object>> handleOrchestratorException(OrchestratorException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
            "error", Map.of("code", ex.getCode(), "message", ex.getMessage())
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
