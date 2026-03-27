package com.storyweaver.controller;

import com.storyweaver.domain.dto.AIWritingChatMessagePinRequestDTO;
import com.storyweaver.domain.dto.AIWritingChatMessageRequestDTO;
import com.storyweaver.domain.vo.AIWritingChatSessionVO;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.service.AIWritingChatService;
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
@RequestMapping("/api/ai-writing/chat")
public class AIWritingChatController {

    private final AIWritingChatService aiWritingChatService;

    public AIWritingChatController(AIWritingChatService aiWritingChatService) {
        this.aiWritingChatService = aiWritingChatService;
    }

    @GetMapping("/{chapterId}")
    public ResponseEntity<Map<String, Object>> getSession(
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        AIWritingChatSessionVO session = aiWritingChatService.getSession(userId, chapterId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "获取成功", "data", session));
    }

    @PostMapping("/{chapterId}/messages")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable Long chapterId,
            @Validated @RequestBody AIWritingChatMessageRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        AIWritingChatSessionVO session = aiWritingChatService.sendMessage(userId, chapterId, requestDTO);
        return ResponseEntity.ok(Map.of("code", 200, "message", "发送成功", "data", session));
    }

    @PostMapping("/messages/{messageId}/background")
    public ResponseEntity<Map<String, Object>> setBackgroundFlag(
            @PathVariable Long messageId,
            @RequestBody(required = false) AIWritingChatMessagePinRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        boolean pinned = requestDTO == null || requestDTO.getPinned() == null || requestDTO.getPinned();
        AIWritingChatSessionVO session = aiWritingChatService.setMessagePinnedToBackground(userId, messageId, pinned);
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功", "data", session));
    }
}
