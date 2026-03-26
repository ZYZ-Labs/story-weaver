package com.storyweaver.controller;

import com.storyweaver.domain.dto.OutlineRequestDTO;
import com.storyweaver.domain.entity.Outline;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.service.OutlineService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/outlines")
public class OutlineController {

    private final OutlineService outlineService;

    public OutlineController(OutlineService outlineService) {
        this.outlineService = outlineService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProjectOutlines(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "获取成功",
                "data", outlineService.getProjectOutlines(projectId, userId)
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOutline(
            @PathVariable Long projectId,
            @RequestBody OutlineRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        if (!StringUtils.hasText(requestDTO.getTitle()) && !StringUtils.hasText(requestDTO.getSummary())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "message", "大纲至少需要标题或摘要"
            ));
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        Outline outline = outlineService.createOutline(projectId, userId, requestDTO);
        if (outline == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "项目不存在或无权访问"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "创建成功",
                "data", outline
        ));
    }

    @GetMapping("/{outlineId}")
    public ResponseEntity<Map<String, Object>> getOutline(
            @PathVariable Long projectId,
            @PathVariable Long outlineId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        Outline outline = outlineService.getOutlineWithAuth(outlineId, userId);
        if (outline == null || !projectId.equals(outline.getProjectId())) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "大纲不存在或无权访问"
            ));
        }
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "获取成功",
                "data", outline
        ));
    }

    @PutMapping("/{outlineId}")
    public ResponseEntity<Map<String, Object>> updateOutline(
            @PathVariable Long projectId,
            @PathVariable Long outlineId,
            @RequestBody OutlineRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        boolean success = outlineService.updateOutline(projectId, outlineId, userId, requestDTO);
        if (!success) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "大纲不存在、未关联当前项目或无权访问"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "更新成功"
        ));
    }

    @DeleteMapping("/{outlineId}")
    public ResponseEntity<Map<String, Object>> deleteOutline(
            @PathVariable Long projectId,
            @PathVariable Long outlineId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        boolean success = outlineService.deleteOutline(outlineId, userId);
        if (!success) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "大纲不存在或无权访问"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "删除成功"
        ));
    }
}
