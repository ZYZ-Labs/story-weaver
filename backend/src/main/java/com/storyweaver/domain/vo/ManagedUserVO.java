package com.storyweaver.domain.vo;

import java.time.LocalDateTime;

public record ManagedUserVO(
        Long id,
        String username,
        String nickname,
        String email,
        String avatar,
        Integer status,
        String roleCode,
        Integer failedLoginAttempts,
        boolean locked,
        LocalDateTime lockedUntil,
        LocalDateTime lastLoginAt,
        LocalDateTime passwordChangedAt,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
