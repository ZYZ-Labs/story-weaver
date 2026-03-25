package com.storyweaver.domain.vo;

import java.time.LocalDateTime;

public record LoginFailureStateVO(
        int failedAttempts,
        int remainingAttempts,
        boolean locked,
        LocalDateTime lockedUntil
) {
}
