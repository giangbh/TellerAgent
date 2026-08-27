package com.dnse.teller.mcp;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class McpToolRegistry {
    private final Map<String, McpTool> toolsById = new LinkedHashMap<>();
    private final Map<String, McpTool> toolsByName = new LinkedHashMap<>();

    public McpToolRegistry(List<McpTool> tools) {
        for (McpTool tool : tools) {
            toolsById.put(tool.getId(), tool);
            toolsByName.put(tool.getName(), tool);
        }
    }

    public Optional<McpTool> findById(String id) {
        return Optional.ofNullable(toolsById.get(id));
    }

    public Optional<McpTool> findByName(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }

    public Optional<McpTool> resolve(String identifier) {
        if (toolsById.containsKey(identifier)) return Optional.of(toolsById.get(identifier));
        if (toolsByName.containsKey(identifier)) return Optional.of(toolsByName.get(identifier));
        return Optional.empty();
    }

    public List<McpTool> getAllTools() {
        return new ArrayList<>(toolsById.values());
    }

    public List<Map<String, Object>> getCapabilitiesList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (McpTool tool : toolsById.values()) {
            Map<String, Object> cap = new LinkedHashMap<>();
            cap.put("id", tool.getId());
            cap.put("name", tool.getName());
            cap.put("type", "TOOL");
            cap.put("description", tool.getDescription());
            cap.put("risk", tool.getRisk());
            cap.put("sideEffect", tool.isSideEffect());
            cap.put("allowedCallers", tool.getAllowedCallers());
            cap.put("workflows", tool.getWorkflows());
            cap.put("requiresIdempotency", tool.isRequiresIdempotency());
            cap.put("inputSchema", tool.getInputSchema());
            list.add(cap);
        }
        return list;
    }
}
