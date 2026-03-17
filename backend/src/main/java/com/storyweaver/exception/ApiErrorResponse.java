package com.storyweaver.exception;

import java.time.Instant;

public record ApiErrorResponse(int code, String message, String path, Instant timestamp) {
}
