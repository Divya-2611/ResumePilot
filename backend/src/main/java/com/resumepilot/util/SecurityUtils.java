package com.resumepilot.util;

import com.resumepilot.entity.User;
import com.resumepilot.security.SecurityUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helpers to read the authenticated principal from the security context.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** Returns the authenticated user or null when anonymous. */
    public static User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser securityUser) {
            return securityUser.getUser();
        }
        return null;
    }

    /** Returns the authenticated user's id or throws when anonymous. */
    public static Long currentUserId() {
        User user = currentUser();
        if (user == null) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return user.getId();
    }
}
