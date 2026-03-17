package com.storyweaver.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public final class AuthHeaderSupport {

    private AuthHeaderSupport() {
    }

    public static boolean hasValidBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return false;
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authorizationHeader.substring(7);
        return !token.isBlank();
    }

    public static ResponseEntity<Map<String, Object>> unauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "code", 401,
                "message", "未认证或 token 无效"
        ));
    }
}
