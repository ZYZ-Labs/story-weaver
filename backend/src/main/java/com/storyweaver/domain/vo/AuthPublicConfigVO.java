package com.storyweaver.domain.vo;

public record AuthPublicConfigVO(
        boolean registrationEnabled,
        int maxFailedAttempts,
        int lockMinutes
) {
}
