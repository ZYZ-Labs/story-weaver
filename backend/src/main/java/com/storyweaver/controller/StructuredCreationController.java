package com.storyweaver.controller;

import com.storyweaver.domain.dto.StructuredCreationApplyRequestDTO;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.story.generation.StructuredCreationApplyResult;
import com.storyweaver.story.generation.StructuredCreationApplyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class StructuredCreationController {

    private final StructuredCreationApplyService structuredCreationApplyService;

    public StructuredCreationController(StructuredCreationApplyService structuredCreationApplyService) {
        this.structuredCreationApplyService = structuredCreationApplyService;
    }

    @PostMapping("/api/projects/{projectId}/structured-creations/apply")
    public ResponseEntity<Map<String, Object>> applyStructuredCreation(
            @PathVariable Long projectId,
            @RequestBody StructuredCreationApplyRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        if (requestDTO != null) {
            requestDTO.setProjectId(projectId);
        }
        StructuredCreationApplyResult result = structuredCreationApplyService.apply(userId, requestDTO);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "待确认新增对象已创建",
                "data", result
        ));
    }
}
