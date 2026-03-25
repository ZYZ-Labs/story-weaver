package com.storyweaver.controller;

import com.storyweaver.domain.entity.SystemConfig;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.service.SystemConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings/system-configs")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listConfigs(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        requireAdmin(authorizationHeader, authentication);
        return ResponseEntity.ok(Map.of("code", 200, "message", "获取成功", "data", systemConfigService.listMergedConfigs()));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> saveConfigs(
            @RequestBody List<SystemConfig> configs,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        requireAdmin(authorizationHeader, authentication);
        return ResponseEntity.ok(Map.of("code", 200, "message", "保存成功", "data", systemConfigService.saveConfigs(configs)));
    }

    private void requireAdmin(String authorizationHeader, Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("未认证或 token 无效");
        }
        SecurityUtils.requireAdmin(authentication);
    }
}
