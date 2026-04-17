package com.storyweaver.controller;

import com.storyweaver.common.web.ApiResponse;
import com.storyweaver.story.generation.orchestration.StorySessionOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StorySessionOrchestrationController {

    private final StorySessionOrchestrator storySessionOrchestrator;

    public StorySessionOrchestrationController(StorySessionOrchestrator storySessionOrchestrator) {
        this.storySessionOrchestrator = storySessionOrchestrator;
    }

    @GetMapping("/api/story-orchestration/projects/{projectId}/chapters/{chapterId}/preview")
    public ResponseEntity<?> preview(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestParam(defaultValue = "scene-1") String sceneId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return storySessionOrchestrator.preview(projectId, chapterId, sceneId)
                .<ResponseEntity<?>>map(preview -> ResponseEntity.ok(ApiResponse.success("获取成功", preview)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error(404, "编排预览不存在")));
    }
}
