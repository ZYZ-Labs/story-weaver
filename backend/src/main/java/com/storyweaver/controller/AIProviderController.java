package com.storyweaver.controller;

import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.domain.vo.ProviderDiscoveryVO;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.service.AIProviderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/providers")
public class AIProviderController {

    private final AIProviderService aiProviderService;

    public AIProviderController(AIProviderService aiProviderService) {
        this.aiProviderService = aiProviderService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listProviders(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        SecurityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(Map.of("code", 200, "message", "获取成功", "data", aiProviderService.listProviders()));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProvider(
            @RequestBody AIProvider provider,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        SecurityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(Map.of("code", 200, "message", "创建成功", "data", aiProviderService.createProvider(provider)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProvider(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        SecurityUtils.getCurrentUserId(authentication);
        AIProvider provider = aiProviderService.getById(id);
        if (provider == null) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "模型服务不存在"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "获取成功", "data", provider));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProvider(
            @PathVariable Long id,
            @RequestBody AIProvider provider,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        SecurityUtils.getCurrentUserId(authentication);
        AIProvider updated = aiProviderService.updateProvider(id, provider);
        if (updated == null) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "模型服务不存在"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功", "data", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProvider(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        SecurityUtils.getCurrentUserId(authentication);
        if (!aiProviderService.deleteProvider(id)) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "模型服务不存在"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testProvider(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        SecurityUtils.getCurrentUserId(authentication);
        boolean success = aiProviderService.testProvider(id);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", success ? "连接测试通过" : "连接测试失败",
                "data", Map.of("success", success)
        ));
    }

    @PostMapping("/discover-models")
    public ResponseEntity<Map<String, Object>> discoverModels(
            @RequestBody AIProvider provider,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        SecurityUtils.getCurrentUserId(authentication);
        ProviderDiscoveryVO discovery = aiProviderService.discoverModels(provider);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", discovery.message(),
                "data", discovery
        ));
    }
}
