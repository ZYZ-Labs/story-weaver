package com.storyweaver.controller;

import com.storyweaver.domain.dto.StoryProgressSuggestRequestDTO;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.story.generation.StoryProgressPredictor;
import com.storyweaver.story.generation.StoryProgressSuggestionVO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class StoryProgressController {

    private final StoryProgressPredictor storyProgressPredictor;

    public StoryProgressController(StoryProgressPredictor storyProgressPredictor) {
        this.storyProgressPredictor = storyProgressPredictor;
    }

    @PostMapping("/api/projects/{projectId}/outlines/progress-suggest")
    public ResponseEntity<Map<String, Object>> suggestOutlineProgress(
            @PathVariable Long projectId,
            @RequestBody(required = false) StoryProgressSuggestRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        StoryProgressSuggestionVO result = storyProgressPredictor.suggestOutlineProgress(
                userId,
                projectId,
                requestDTO == null ? null : requestDTO.getTargetOutlineId(),
                requestDTO == null ? null : requestDTO.getContextText()
        );
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "进度建议已生成",
                "data", result
        ));
    }

    @PostMapping("/api/projects/{projectId}/plotlines/progress-suggest")
    public ResponseEntity<Map<String, Object>> suggestPlotProgress(
            @PathVariable Long projectId,
            @RequestBody(required = false) StoryProgressSuggestRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        StoryProgressSuggestionVO result = storyProgressPredictor.suggestPlotProgress(
                userId,
                projectId,
                requestDTO == null ? null : requestDTO.getTargetOutlineId(),
                requestDTO == null ? null : requestDTO.getContextText()
        );
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "进度建议已生成",
                "data", result
        ));
    }

    @PostMapping("/api/projects/{projectId}/chapters/progress-suggest")
    public ResponseEntity<Map<String, Object>> suggestChapterProgress(
            @PathVariable Long projectId,
            @RequestBody(required = false) StoryProgressSuggestRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        StoryProgressSuggestionVO result = storyProgressPredictor.suggestChapterProgress(
                userId,
                projectId,
                requestDTO == null ? null : requestDTO.getTargetChapterId(),
                requestDTO == null ? null : requestDTO.getContextText()
        );
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "进度建议已生成",
                "data", result
        ));
    }
}
