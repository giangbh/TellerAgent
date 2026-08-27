package com.dnse.teller.controller;

import com.dnse.teller.mcp.JsonRpcRequest;
import com.dnse.teller.mcp.JsonRpcResponse;
import com.dnse.teller.mcp.McpServer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mcp")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:8799", "http://127.0.0.1:8799"})
public class McpController {
    private final McpServer mcpServer;

    public McpController(McpServer mcpServer) {
        this.mcpServer = mcpServer;
    }

    @PostMapping
    public ResponseEntity<JsonRpcResponse> handleMcpJsonRpc(@RequestBody JsonRpcRequest request) {
        JsonRpcResponse response = mcpServer.handleRequest(request);
        return ResponseEntity.ok(response);
    }
}
