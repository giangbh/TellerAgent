package com.dnse.teller.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Điểm nối (seam) duy nhất giữa hạ tầng xác thực và logic nghiệp vụ.
 *
 * POC: đọc danh tính từ header do reverse proxy / BFF đã xác thực đặt vào.
 * PRODUCTION: thay thân hàm {@link #resolve} bằng việc đọc từ
 * SecurityContextHolder sau khi cấu hình Spring Security + OIDC. Toàn bộ phần
 * còn lại của hệ thống KHÔNG cần sửa, vì không nơi nào khác được phép tự dựng
 * {@link AuthenticatedActor}.
 *
 * Chế độ trust-header chỉ được bật khi teller.security.trust-headers=true và
 * mặc định là false, để không ai vô tình chạy production với cơ chế này.
 */
@Service
public class ActorResolver {

    static final String HEADER_ID = "X-Actor-Id";
    static final String HEADER_NAME = "X-Actor-Name";
    static final String HEADER_ROLE = "X-Actor-Role";
    static final String HEADER_BRANCH = "X-Actor-Branch";

    private final boolean trustHeaders;

    public ActorResolver(@Value("${teller.security.trust-headers:false}") boolean trustHeaders) {
        this.trustHeaders = trustHeaders;
    }

    public AuthenticatedActor resolve(HttpServletRequest request) {
        if (!trustHeaders) {
            throw new AuthorizationException(
                    "Chưa cấu hình nguồn xác thực. Bật teller.security.trust-headers cho môi trường POC "
                            + "hoặc cấu hình OIDC cho môi trường thật.",
                    "AUTHENTICATION_NOT_CONFIGURED", 401);
        }
        if (request == null) {
            throw new AuthorizationException("Không có ngữ cảnh request để xác thực.", "NO_REQUEST_CONTEXT", 401);
        }

        String userId = trimToNull(request.getHeader(HEADER_ID));
        String role = trimToNull(request.getHeader(HEADER_ROLE));
        String name = trimToNull(request.getHeader(HEADER_NAME));
        String branch = trimToNull(request.getHeader(HEADER_BRANCH));

        if (userId == null) {
            throw new AuthorizationException("Request thiếu " + HEADER_ID + ".", "ACTOR_ID_MISSING", 401);
        }

        return new AuthenticatedActor(
                userId,
                name != null ? name : userId,
                AuthenticatedActor.Role.parse(role),
                branch
        );
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
