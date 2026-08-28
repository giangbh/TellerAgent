package com.dnse.teller.security;

/** Lỗi phân quyền / kiểm soát. Luôn fail-closed: ném ra là từ chối, không có nhánh "cho qua". */
public class AuthorizationException extends RuntimeException {
    private final String code;
    private final int status;

    public AuthorizationException(String message, String code, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() { return code; }
    public int getStatus() { return status; }
}
