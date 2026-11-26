package com.vahan.oneops.user;

import com.vahan.oneops.common.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin-only (enforced by SecurityConfig for everything under /api/admin). */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService users;

    public AdminUserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public List<UserDto> list() {
        return users.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody UserRequests.CreateUser request) {
        return users.create(request);
    }

    @PatchMapping("/{id}")
    public UserDto update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                          @Valid @RequestBody UserRequests.UpdateUser request) {
        return users.update(me.id(), id, request);
    }

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody UserRequests.ResetPassword request) {
        users.resetPassword(id, request.newPassword());
    }
}
