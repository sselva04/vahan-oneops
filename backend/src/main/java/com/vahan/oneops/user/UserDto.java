package com.vahan.oneops.user;

public record UserDto(Long id, String username, String fullName, Role role, boolean active) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getUsername(), u.getFullName(), u.getRole(), u.isActive());
    }
}
