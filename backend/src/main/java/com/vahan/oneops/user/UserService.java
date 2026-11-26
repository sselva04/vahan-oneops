package com.vahan.oneops.user;

import com.vahan.oneops.common.BadRequestException;
import com.vahan.oneops.common.ConflictException;
import com.vahan.oneops.common.NotFoundException;
import com.vahan.oneops.common.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UserService implements UserDirectory {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        return users.findAllByOrderByFullNameAsc().stream().map(UserDto::from).toList();
    }

    @Transactional(readOnly = true)
    public UserDto get(Long id) {
        return UserDto.from(find(id));
    }

    @Transactional
    public UserDto create(UserRequests.CreateUser r) {
        String username = normalize(r.username());
        if (users.existsByUsername(username)) {
            throw new ConflictException("Username '" + username + "' is already taken");
        }
        User user = new User(username, r.fullName().trim(), encoder.encode(r.password()), r.role());
        return UserDto.from(users.save(user));
    }

    @Transactional
    public UserDto update(Long actingAdminId, Long id, UserRequests.UpdateUser r) {
        User user = find(id);
        boolean isSelf = user.getId().equals(actingAdminId);
        if (isSelf && r.active() != null && !r.active()) {
            throw new BadRequestException("You cannot deactivate your own account");
        }
        if (isSelf && r.role() != null && r.role() != user.getRole()) {
            throw new BadRequestException("You cannot change your own role");
        }
        if (r.fullName() != null) {
            if (r.fullName().isBlank()) {
                throw new BadRequestException("Name cannot be empty");
            }
            user.setFullName(r.fullName().trim());
        }
        if (r.role() != null) {
            user.setRole(r.role());
        }
        if (r.active() != null) {
            user.setActive(r.active());
        }
        return UserDto.from(user);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        find(id).setPasswordHash(encoder.encode(newPassword));
    }

    @Transactional
    public void changePassword(Long id, String currentPassword, String newPassword) {
        User user = find(id);
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(encoder.encode(newPassword));
    }

    /** Used by login. Returns the user only when the password matches and the account is active. */
    @Transactional(readOnly = true)
    public User authenticate(String username, String password) {
        User user = users.findByUsername(normalize(username)).orElse(null);
        if (user == null || !user.isActive() || !encoder.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User findActive(Long id) {
        return users.findById(id).filter(User::isActive).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> displayNames(Collection<Long> userIds) {
        Map<Long, String> names = new HashMap<>();
        users.findAllById(userIds).forEach(u -> names.put(u.getId(), u.getFullName()));
        return names;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(Long userId) {
        return users.findById(userId).map(User::isActive).orElse(false);
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
