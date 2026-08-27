package com.dnse.teller.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcResponse {
    private String jsonrpc = "2.0";
    private Object id;
    private Object result;
    private McpError error;

    public JsonRpcResponse() {}

    public static JsonRpcResponse success(Object id, Object result) {
        JsonRpcResponse resp = new JsonRpcResponse();
        resp.setId(id);
        resp.setResult(result);
        return resp;
    }

    public static JsonRpcResponse error(Object id, int code, String message, Object data) {
        JsonRpcResponse resp = new JsonRpcResponse();
        resp.setId(id);
        resp.setError(new McpError(code, message, data));
        return resp;
    }

    public String getJsonrpc() { return jsonrpc; }
    public void setJsonrpc(String jsonrpc) { this.jsonrpc = jsonrpc; }

    public Object getId() { return id; }
    public void setId(Object id) { this.id = id; }

    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }

    public McpError getError() { return error; }
    public void setError(McpError error) { this.error = error; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class McpError {
        private int code;
        private String message;
        private Object data;

        public McpError() {}
        public McpError(int code, String message, Object data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }
}
