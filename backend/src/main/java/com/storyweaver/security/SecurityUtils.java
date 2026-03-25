package com.storyweaver.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static CurrentUser getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new InsufficientAuthenticationException("未认证用户");
        }
        return currentUser;
    }

    public static Long getCurrentUserId(Authentication authentication) {
        return getCurrentUser(authentication).userId();
    }

    public static boolean isAdmin(Authentication authentication) {
        return getCurrentUser(authentication).isAdmin();
    }

    public static void requireAdmin(Authentication authentication) {
        if (!isAdmin(authentication)) {
            throw new AccessDeniedException("需要管理员权限");
        }
    }
}
