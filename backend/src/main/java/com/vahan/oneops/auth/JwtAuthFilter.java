package com.vahan.oneops.auth;

import com.vahan.oneops.common.AuthUser;
import com.vahan.oneops.user.User;
import com.vahan.oneops.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads "Authorization: Bearer <token>". The user is loaded from the database on every request,
 * so deactivating a user or changing a role takes effect immediately, not when the token expires.
 * Deliberately NOT a Spring bean: SecurityConfig adds it to the security chain exactly once.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UserService users;

    public JwtAuthFilter(JwtService jwt, UserService users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            jwt.parseUserId(header.substring(7)).ifPresent(userId -> {
                User user = users.findActive(userId);
                if (user != null) {
                    AuthUser principal = new AuthUser(user.getId(), user.getUsername(), user.getRole().name());
                    var auth = new UsernamePasswordAuthenticationToken(principal, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            });
        }
        chain.doFilter(request, response);
    }
}
