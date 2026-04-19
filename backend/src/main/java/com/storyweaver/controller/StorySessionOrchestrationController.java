package com.storyweaver.controller;

import com.storyweaver.common.web.ApiResponse;
import com.storyweaver.domain.dto.ChapterSkeletonSceneRequestDTO;
import com.storyweaver.story.generation.orchestration.ChapterExecutionReviewService;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonMutationService;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonPlanner;
import com.storyweaver.story.generation.orchestration.SceneSkeletonMutationCommand;
import com.storyweaver.story.generation.orchestration.SceneExecutionRequest;
import com.storyweaver.story.generation.orchestration.StorySessionOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StorySessionOrchestrationController {

    private final StorySessionOrchestrator storySessionOrchestrator;
    private final ChapterSkeletonPlanner chapterSkeletonPlanner;
    private final ChapterExecutionReviewService chapterExecutionReviewService;
    private final ChapterSkeletonMutationService chapterSkeletonMutationService;

    public StorySessionOrchestrationController(
            StorySessionOrchestrator storySessionOrchestrator,
            ChapterSkeletonPlanner chapterSkeletonPlanner,
            ChapterExecutionReviewService chapterExecutionReviewService,
            ChapterSkeletonMutationService chapterSkeletonMutationService) {
        this.storySessionOrchestrator = storySessionOrchestrator;
        this.chapterSkeletonPlanner = chapterSkeletonPlanner;
        this.chapterExecutionReviewService = chapterExecutionReviewService;
        this.chapterSkeletonMutationService = chapterSkeletonMutationService;
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

    @PostMapping("/api/story-orchestration/projects/{projectId}/chapters/{chapterId}/execute")
    public ResponseEntity<?> execute(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestParam(defaultValue = "scene-1") String sceneId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return storySessionOrchestrator.execute(new SceneExecutionRequest(projectId, chapterId, sceneId))
                .<ResponseEntity<?>>map(execution -> ResponseEntity.ok(ApiResponse.success("执行成功", execution)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error(404, "镜头执行不存在")));
    }

    @GetMapping("/api/story-orchestration/projects/{projectId}/chapters/{chapterId}/skeleton-preview")
    public ResponseEntity<?> skeletonPreview(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return chapterSkeletonPlanner.plan(projectId, chapterId)
                .<ResponseEntity<?>>map(skeleton -> ResponseEntity.ok(ApiResponse.success("获取成功", skeleton)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error(404, "章节骨架预览不存在")));
    }

    @GetMapping("/api/story-orchestration/projects/{projectId}/chapters/{chapterId}/chapter-review")
    public ResponseEntity<?> chapterReview(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return chapterExecutionReviewService.review(projectId, chapterId)
                .<ResponseEntity<?>>map(review -> ResponseEntity.ok(ApiResponse.success("获取成功", review)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error(404, "章节级审校结果不存在")));
    }

    @PutMapping("/api/story-orchestration/projects/{projectId}/chapters/{chapterId}/skeleton-scenes/{sceneId}")
    public ResponseEntity<?> updateSkeletonScene(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @PathVariable String sceneId,
            @RequestBody ChapterSkeletonSceneRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        try {
            return chapterSkeletonMutationService.updateScene(projectId, chapterId, new SceneSkeletonMutationCommand(
                            sceneId,
                            requestDTO.getGoal(),
                            requestDTO.getReaderReveal(),
                            requestDTO.getMustUseAnchors(),
                            requestDTO.getStopCondition(),
                            requestDTO.getTargetWords()
                    ))
                    .<ResponseEntity<?>>map(skeleton -> ResponseEntity.ok(ApiResponse.success("镜头已更新", skeleton)))
                    .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error(404, "镜头不存在")));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(ApiResponse.error(409, ex.getMessage()));
        }
    }

    @DeleteMapping("/api/story-orchestration/projects/{projectId}/chapters/{chapterId}/skeleton-scenes/{sceneId}")
    public ResponseEntity<?> deleteSkeletonScene(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @PathVariable String sceneId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        try {
            return chapterSkeletonMutationService.deleteScene(projectId, chapterId, sceneId)
                    .<ResponseEntity<?>>map(skeleton -> ResponseEntity.ok(ApiResponse.success("镜头已删除", skeleton)))
                    .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error(404, "镜头不存在")));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(ApiResponse.error(409, ex.getMessage()));
        }
    }
}
