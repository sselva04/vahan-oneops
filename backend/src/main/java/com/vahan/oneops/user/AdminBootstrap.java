package com.vahan.oneops.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * On the very first start (empty user table) creates the first admin so someone can log in.
 * Set VAHAN_ADMIN_PASSWORD to choose the password; otherwise a random one is printed once in the log.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String username;
    private final String configuredPassword;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder,
                          @Value("${vahan.bootstrap.admin-username:admin}") String username,
                          @Value("${vahan.bootstrap.admin-password:}") String configuredPassword) {
        this.users = users;
        this.encoder = encoder;
        this.username = username;
        this.configuredPassword = configuredPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        String password = configuredPassword;
        boolean generated = password == null || password.isBlank();
        if (generated) {
            byte[] bytes = new byte[12];
            new SecureRandom().nextBytes(bytes);
            password = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }
        users.save(new User(username.trim().toLowerCase(), "Administrator", encoder.encode(password), Role.ADMIN));
        if (generated) {
            log.warn("First start: created admin user '{}' with generated password '{}'. Log in and change it now.",
                    username, password);
        } else {
            log.info("First start: created admin user '{}' with the configured password.", username);
        }
    }
}
