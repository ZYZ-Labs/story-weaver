package com.storyweaver.controller;

import com.storyweaver.common.web.ApiResponse;
import com.storyweaver.storyunit.context.RecentStoryProgressView;
import com.storyweaver.storyunit.context.StoryContextQueryService;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StoryContextController {

    private final StoryContextQueryService storyContextQueryService;

    public StoryContextController(StoryContextQueryService storyContextQueryService) {
        this.storyContextQueryService = storyContextQueryService;
    }

    @GetMapping("/api/story-context/projects/{projectId}/brief")
    public ResponseEntity<?> getProjectBrief(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return storyContextQueryService.getProjectBrief(projectId)
                .<ResponseEntity<?>>map(view -> ResponseEntity.ok(ApiResponse.success("获取成功", view)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error(404, "项目不存在或已删除")));
    }

    @GetMapping("/api/story-context/story-units/summary")
    public ResponseEntity<?> getStoryUnitSummary(
            @RequestParam String unitId,
            @RequestParam String unitKey,
            @RequestParam StoryUnitType unitType,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        StoryUnitRef ref = new StoryUnitRef(unitId, unitKey, unitType);
        return storyContextQueryService.getStoryUnitSummary(ref)
                .<ResponseEntity<?>>map(view -> ResponseEntity.ok(ApiResponse.success("获取成功", view)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error(404, "StoryUnit 摘要不存在")));
    }

    @GetMapping("/api/story-context/projects/{projectId}/chapters/{chapterId}/anchors")
    public ResponseEntity<?> getChapterAnchorBundle(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return storyContextQueryService.getChapterAnchorBundle(projectId, chapterId)
                .<ResponseEntity<?>>map(view -> ResponseEntity.ok(ApiResponse.success("获取成功", view)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error(404, "章节锚点不存在")));
    }

    @GetMapping("/api/story-context/projects/{projectId}/chapters/{chapterId}/reader-known-state")
    public ResponseEntity<?> getReaderKnownState(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return storyContextQueryService.getReaderKnownState(projectId, chapterId)
                .<ResponseEntity<?>>map(view -> ResponseEntity.ok(ApiResponse.success("获取成功", view)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error(404, "读者已知状态不存在")));
    }

    @GetMapping("/api/story-context/projects/{projectId}/characters/{characterId}/runtime-state")
    public ResponseEntity<?> getCharacterRuntimeState(
            @PathVariable Long projectId,
            @PathVariable Long characterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return storyContextQueryService.getCharacterRuntimeState(projectId, characterId)
                .<ResponseEntity<?>>map(view -> ResponseEntity.ok(ApiResponse.success("获取成功", view)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error(404, "人物运行时状态不存在")));
    }

    @GetMapping("/api/story-context/projects/{projectId}/progress")
    public ResponseEntity<?> getRecentStoryProgress(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        RecentStoryProgressView view = storyContextQueryService.getRecentStoryProgress(projectId, limit);
        return ResponseEntity.ok(ApiResponse.success("获取成功", view));
    }
}
