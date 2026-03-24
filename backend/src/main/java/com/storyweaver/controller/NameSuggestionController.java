package com.storyweaver.controller;

import com.storyweaver.domain.dto.NameSuggestionRequestDTO;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.service.NameSuggestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/name-suggestions")
public class NameSuggestionController {

    private final NameSuggestionService nameSuggestionService;

    public NameSuggestionController(NameSuggestionService nameSuggestionService) {
        this.nameSuggestionService = nameSuggestionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> generateSuggestions(
            @PathVariable Long projectId,
            @Validated @RequestBody NameSuggestionRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "获取成功",
                "data", nameSuggestionService.generateSuggestions(projectId, userId, requestDTO)
        ));
    }
}
