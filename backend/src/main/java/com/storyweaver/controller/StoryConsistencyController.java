package com.storyweaver.controller;

import com.storyweaver.security.SecurityUtils;
import com.storyweaver.story.generation.StoryConsistencyInspector;
import com.storyweaver.story.generation.StoryConsistencyReportVO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class StoryConsistencyController {

    private final StoryConsistencyInspector storyConsistencyInspector;

    public StoryConsistencyController(StoryConsistencyInspector storyConsistencyInspector) {
        this.storyConsistencyInspector = storyConsistencyInspector;
    }

    @GetMapping("/api/projects/{projectId}/story-consistency")
    public ResponseEntity<Map<String, Object>> inspectProjectConsistency(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        StoryConsistencyReportVO report = storyConsistencyInspector.inspectProject(userId, projectId);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "获取成功",
                "data", report
        ));
    }
}
