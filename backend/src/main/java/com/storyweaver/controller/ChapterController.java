package com.storyweaver.controller;

import com.storyweaver.domain.dto.ChapterRequestDTO;
import com.storyweaver.domain.dto.ChapterNarrativeRuntimeModeRequestDTO;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.service.ChapterService;
import com.storyweaver.story.generation.orchestration.ChapterNarrativeRuntimeMode;
import com.storyweaver.story.generation.orchestration.ChapterNarrativeRuntimeModeService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/chapters")
public class ChapterController {

    private final ChapterService chapterService;
    private final ChapterNarrativeRuntimeModeService chapterNarrativeRuntimeModeService;

    public ChapterController(
            ChapterService chapterService,
            ChapterNarrativeRuntimeModeService chapterNarrativeRuntimeModeService) {
        this.chapterService = chapterService;
        this.chapterNarrativeRuntimeModeService = chapterNarrativeRuntimeModeService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProjectChapters(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);

        List<Chapter> chapters = chapterService.getProjectChapters(projectId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", chapters);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createChapter(
            @PathVariable Long projectId,
            @RequestBody ChapterRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);

        if (!StringUtils.hasText(requestDTO.getTitle())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "message", "章节标题不能为空"
            ));
        }

        Chapter chapter = chapterService.createChapter(projectId, userId, requestDTO);
        if (chapter == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "项目不存在或无权访问"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "创建成功",
                "data", chapter
        ));
    }

    @PutMapping("/{chapterId}")
    public ResponseEntity<Map<String, Object>> updateChapter(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestBody ChapterRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);

        boolean success = chapterService.updateChapter(projectId, chapterId, userId, requestDTO);
        if (!success) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "章节不存在、未关联当前项目或无权访问"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "更新成功"
        ));
    }

    @DeleteMapping("/{chapterId}")
    public ResponseEntity<Map<String, Object>> deleteChapter(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);

        boolean success = chapterService.deleteChapter(chapterId, userId);
        if (!success) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "章节不存在或无权访问"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "删除成功"
        ));
    }

    @GetMapping("/{chapterId}")
    public ResponseEntity<Map<String, Object>> getChapter(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);

        Chapter chapter = chapterService.getChapterWithAuth(chapterId, userId);
        if (chapter == null || !projectId.equals(chapter.getProjectId())) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "章节不存在或无权访问"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "获取成功",
                "data", chapter
        ));
    }

    @PutMapping("/{chapterId}/runtime-mode")
    public ResponseEntity<Map<String, Object>> updateRuntimeMode(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestBody ChapterNarrativeRuntimeModeRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        if (requestDTO == null || !StringUtils.hasText(requestDTO.getMode())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "message", "章节运行模式不能为空"
            ));
        }

        Chapter chapter = chapterService.getChapterWithAuth(chapterId, userId);
        if (chapter == null || !projectId.equals(chapter.getProjectId())) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "章节不存在或无权访问"
            ));
        }

        ChapterNarrativeRuntimeMode targetMode = ChapterNarrativeRuntimeMode.fromValue(requestDTO.getMode());
        ChapterNarrativeRuntimeMode updatedMode = chapterNarrativeRuntimeModeService.updateMode(chapter, targetMode);
        chapter.setNarrativeRuntimeMode(updatedMode.apiValue());

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "切换成功",
                "data", chapter
        ));
    }
}
