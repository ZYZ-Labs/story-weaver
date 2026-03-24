package com.storyweaver.controller;

import com.storyweaver.domain.entity.Causality;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.service.CausalityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class CausalityController {

    private final CausalityService causalityService;

    public CausalityController(CausalityService causalityService) {
        this.causalityService = causalityService;
    }

    @GetMapping("/api/projects/{projectId}/causalities")
    public ResponseEntity<Map<String, Object>> getProjectCausalities(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(Map.of("code", 200, "message", "获取成功", "data", causalityService.getProjectCausalities(projectId, userId)));
    }

    @PostMapping("/api/projects/{projectId}/causalities")
    public ResponseEntity<Map<String, Object>> createCausality(
            @PathVariable Long projectId,
            @RequestBody Causality causality,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        Causality created = causalityService.createCausality(projectId, userId, causality);
        if (created == null) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "项目不存在或无权访问"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "创建成功", "data", created));
    }

    @GetMapping("/api/causalities/{id}")
    public ResponseEntity<Map<String, Object>> getCausality(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        Causality causality = causalityService.getCausality(id, userId);
        if (causality == null) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "因果关系不存在或无权访问"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "获取成功", "data", causality));
    }

    @PutMapping("/api/causalities/{id}")
    public ResponseEntity<Map<String, Object>> updateCausality(
            @PathVariable Long id,
            @RequestBody Causality causality,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        if (!causalityService.updateCausality(id, userId, causality)) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "因果关系不存在或无权访问"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功"));
    }

    @DeleteMapping("/api/causalities/{id}")
    public ResponseEntity<Map<String, Object>> deleteCausality(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        if (!causalityService.deleteCausality(id, userId)) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "因果关系不存在或无权访问"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }
}
