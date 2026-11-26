package com.vahan.oneops.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserRequests {

    private UserRequests() {
    }

    public record CreateUser(
            @NotBlank @Size(min = 3, max = 50)
            @Pattern(regexp = "[A-Za-z0-9._-]+", message = "Use letters, numbers, dot, dash or underscore only")
            String username,
            @NotBlank @Size(max = 120) String fullName,
            @NotNull Role role,
            @NotBlank @Size(min = 8, max = 100, message = "Password must be at least 8 characters") String password) {
    }

    /** Every field is optional; only the ones sent are changed. */
    public record UpdateUser(@Size(max = 120) String fullName, Role role, Boolean active) {
    }

    public record ResetPassword(
            @NotBlank @Size(min = 8, max = 100, message = "Password must be at least 8 characters") String newPassword) {
    }

    public record ChangePassword(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 100, message = "Password must be at least 8 characters") String newPassword) {
    }
}
