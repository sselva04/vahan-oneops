package com.vahan.oneops.auth;

import com.vahan.oneops.common.AuthUser;
import com.vahan.oneops.user.User;
import com.vahan.oneops.user.UserDto;
import com.vahan.oneops.user.UserRequests;
import com.vahan.oneops.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(String token, UserDto user) {
    }

    private final UserService users;
    private final JwtService jwt;

    public AuthController(UserService users, JwtService jwt) {
        this.users = users;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = users.authenticate(request.username(), request.password());
        return new LoginResponse(jwt.issue(user.getId()), UserDto.from(user));
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal AuthUser me) {
        return users.get(me.id());
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthUser me,
                               @Valid @RequestBody UserRequests.ChangePassword request) {
        users.changePassword(me.id(), request.currentPassword(), request.newPassword());
    }
}
