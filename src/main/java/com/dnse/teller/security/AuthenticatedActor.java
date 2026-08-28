package com.dnse.teller.security;

/**
 * Danh tính đã được xác thực của người đang thao tác.
 *
 * KHÁC BIỆT CỐT LÕI so với bản cũ: trước đây "actor" là một chuỗi tự do lấy từ
 * request body, nên bất kỳ ai cũng có thể tự xưng là supervisor. Từ nay mọi
 * quyết định phân quyền phải dựa trên đối tượng này, và đối tượng này chỉ được
 * sinh ra bởi {@link ActorResolver} từ nguồn xác thực.
 */
public record AuthenticatedActor(
        String userId,
        String displayName,
        Role role,
        String branchId
) {

    public enum Role {
        /** Giao dịch viên tại quầy. */
        TELLER,
        /** Kiểm soát viên — mắt thứ hai của maker-checker. */
        SUPERVISOR,
        /** Quản trị chính sách bảo mật MCP. Không được phép tác nghiệp giao dịch. */
        SECURITY_ADMIN;

        public static Role parse(String raw) {
            if (raw == null) {
                throw new AuthorizationException("Thiếu vai trò người dùng.", "ACTOR_ROLE_MISSING", 401);
            }
            try {
                return Role.valueOf(raw.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new AuthorizationException("Vai trò không hợp lệ: " + raw, "ACTOR_ROLE_INVALID", 403);
            }
        }
    }

    public AuthenticatedActor {
        if (userId == null || userId.isBlank()) {
            throw new AuthorizationException("Thiếu định danh người dùng.", "ACTOR_ID_MISSING", 401);
        }
        if (role == null) {
            throw new AuthorizationException("Thiếu vai trò người dùng.", "ACTOR_ROLE_MISSING", 401);
        }
    }

    public boolean hasRole(Role expected) {
        return role == expected;
    }

    public void requireRole(Role expected) {
        if (!hasRole(expected)) {
            throw new AuthorizationException(
                    "Vai trò " + role + " không được phép thực hiện thao tác yêu cầu vai trò " + expected + ".",
                    "ROLE_NOT_PERMITTED", 403);
        }
    }
}
