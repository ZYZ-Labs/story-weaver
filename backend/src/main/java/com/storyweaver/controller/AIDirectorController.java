package com.storyweaver.controller;

import com.storyweaver.ai.director.application.AIDirectorApplicationService;
import com.storyweaver.domain.dto.AIDirectorDecisionRequestDTO;
import com.storyweaver.domain.vo.AIDirectorDecisionVO;
import com.storyweaver.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai-director")
public class AIDirectorController {

    private final AIDirectorApplicationService aiDirectorApplicationService;

    public AIDirectorController(AIDirectorApplicationService aiDirectorApplicationService) {
        this.aiDirectorApplicationService = aiDirectorApplicationService;
    }

    @PostMapping("/decide")
    public ResponseEntity<Map<String, Object>> decide(
            @Validated @RequestBody AIDirectorDecisionRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        AIDirectorDecisionVO decision = aiDirectorApplicationService.decide(userId, requestDTO);
        return ResponseEntity.ok(Map.of("code", 200, "message", "决策成功", "data", decision));
    }

    @GetMapping("/{decisionId}")
    public ResponseEntity<Map<String, Object>> getDecision(
            @PathVariable Long decisionId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        AIDirectorDecisionVO decision = aiDirectorApplicationService.getDecision(userId, decisionId);
        if (decision == null) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "总导决策不存在"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "获取成功", "data", decision));
    }

    @GetMapping("/chapter/{chapterId}/latest")
    public ResponseEntity<Map<String, Object>> getLatestDecision(
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        AIDirectorDecisionVO decision = aiDirectorApplicationService.getLatestDecision(userId, chapterId);
        if (decision == null) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "暂无总导决策记录"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "获取成功", "data", decision));
    }
}
