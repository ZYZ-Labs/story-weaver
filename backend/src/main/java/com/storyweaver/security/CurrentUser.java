package com.storyweaver.security;

public record CurrentUser(Long userId, String username, String roleCode) {
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(roleCode);
    }
}
