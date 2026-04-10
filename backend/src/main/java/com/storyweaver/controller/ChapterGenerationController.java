package com.storyweaver.controller;

import com.storyweaver.domain.dto.ChapterAnchorUpdateRequestDTO;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.service.ChapterService;
import com.storyweaver.story.generation.ChapterAnchorBundle;
import com.storyweaver.story.generation.ChapterAnchorResolver;
import com.storyweaver.story.generation.ChapterAnchorUpdateResult;
import com.storyweaver.story.generation.ChapterAnchorWriteService;
import com.storyweaver.story.generation.GenerationReadinessService;
import com.storyweaver.story.generation.GenerationReadinessVO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ChapterGenerationController {

    private final ChapterService chapterService;
    private final ChapterAnchorResolver chapterAnchorResolver;
    private final ChapterAnchorWriteService chapterAnchorWriteService;
    private final GenerationReadinessService generationReadinessService;

    public ChapterGenerationController(
            ChapterService chapterService,
            ChapterAnchorResolver chapterAnchorResolver,
            ChapterAnchorWriteService chapterAnchorWriteService,
            GenerationReadinessService generationReadinessService) {
        this.chapterService = chapterService;
        this.chapterAnchorResolver = chapterAnchorResolver;
        this.chapterAnchorWriteService = chapterAnchorWriteService;
        this.generationReadinessService = generationReadinessService;
    }

    @GetMapping("/api/projects/{projectId}/chapters/{chapterId}/generation-readiness")
    public ResponseEntity<Map<String, Object>> getGenerationReadiness(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        requireChapter(projectId, chapterId, userId);
        GenerationReadinessVO result = generationReadinessService.evaluate(userId, chapterId);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "获取成功",
                "data", result
        ));
    }

    @GetMapping("/api/projects/{projectId}/chapters/{chapterId}/anchors")
    public ResponseEntity<Map<String, Object>> getChapterAnchors(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        requireChapter(projectId, chapterId, userId);
        ChapterAnchorBundle result = chapterAnchorResolver.resolve(userId, chapterId);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "获取成功",
                "data", result
        ));
    }

    @PutMapping("/api/projects/{projectId}/chapters/{chapterId}/anchors")
    public ResponseEntity<Map<String, Object>> updateChapterAnchors(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestBody(required = false) ChapterAnchorUpdateRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        ChapterAnchorUpdateResult result = chapterAnchorWriteService.updateAnchors(userId, projectId, chapterId, requestDTO);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "章节锚点已更新",
                "data", result
        ));
    }

    private void requireChapter(Long projectId, Long chapterId, Long userId) {
        Chapter chapter = chapterService.getChapterWithAuth(chapterId, userId);
        if (chapter == null || !projectId.equals(chapter.getProjectId())) {
            throw new IllegalArgumentException("章节不存在或无权访问");
        }
    }
}
