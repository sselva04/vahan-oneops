package com.vahan.oneops.user;

import java.util.Collection;
import java.util.Map;

/**
 * The only thing other modules may use to look at users. Feature modules (projects, billing, ...)
 * keep a plain user id and ask this interface for names, so they never touch the User table directly.
 */
public interface UserDirectory {

    Map<Long, String> displayNames(Collection<Long> userIds);

    boolean isActive(Long userId);
}
