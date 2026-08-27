package com.dnse.teller.mcp;

import java.util.List;
import java.util.Map;

public interface McpTool {
    String getId();
    String getName();
    String getDescription();
    String getRisk();
    boolean isSideEffect();
    boolean isRequiresIdempotency();
    List<String> getAllowedCallers();
    List<String> getWorkflows();
    Map<String, Object> getInputSchema();

    Map<String, Object> execute(Map<String, Object> args, String idempotencyKey) throws Exception;
}
