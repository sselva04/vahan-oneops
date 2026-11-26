package com.vahan.oneops.common;

/**
 * The logged-in user as seen by controllers: take it with {@code @AuthenticationPrincipal AuthUser me}.
 * Lives in "common" so feature modules never depend on the auth module.
 */
public record AuthUser(Long id, String username, String role) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
