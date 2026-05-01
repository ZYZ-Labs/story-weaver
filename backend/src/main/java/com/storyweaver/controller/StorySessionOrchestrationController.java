package com.storyweaver.controller;

import com.storyweaver.common.web.ApiResponse;
import com.storyweaver.config.StoryCompatibilityProperties;
import com.storyweaver.domain.dto.ChapterSkeletonSceneRequestDTO;
import com.storyweaver.domain.dto.NodeActionRequestDTO;
import com.storyweaver.story.generation.orchestration.ChapterNodeRuntimeService;
import com.storyweaver.story.generation.orchestration.ChapterExecutionReviewService;
import com.storyweaver.story.generation.orchestration.ChapterNarrativeRuntimeModeService;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonGenerationService;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonMutationService;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonPlanner;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonStreamEvent;
import com.storyweaver.story.generation.orchestration.NodeActionRequest;
import com.storyweaver.story.generation.orchestration.SceneSkeletonMutationCommand;
import com.storyweaver.story.generation.orchestration.SceneExecutionRequest;
import com.storyweaver.story.generation.orchestration.StorySessionOrchestrator;
import com.storyweaver.story.generation.orchestration.impl.ChapterSceneWorkflowGuardService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
public class StorySessionOrchestrationController {

    private final StorySessionOrchestrator storySessionOrchestrator;
    private final ChapterSkeletonPlanner chapterSkeletonPlanner;
    private final ChapterSkeletonGenerationService chapterSkeletonGenerationService;
    private final ChapterExecutionReviewService chapterExecutionReviewService;
    private final ChapterSkeletonMutationService chapterSkeletonMutationService;
    private final ChapterNodeRuntimeService chapterNodeRuntimeService;
    private final ChapterSceneWorkflowGuardService chapterSceneWorkflowGuardService;
    private final StoryCompatibilityProperties storyCompatibilityProperties;
    private final ChapterNarrativeRuntimeModeService chapterNarrativeRuntimeModeService;

    public StorySessionOrchestrationController(
            StorySessionOrchestrator storySessionOrchestrator,
            ChapterSkeletonPlanner chapterSkeletonPlanner,
            ChapterSkeletonGenerationService chapterSkeletonGenerationService,
            ChapterExecutionReviewService chapterExecutionReviewService,
            ChapterSkeletonMutationService chapterSkeletonMutationService,
            ChapterNodeRuntimeService chapterNodeRuntimeService,
            ChapterSceneWorkflowGuardService chapterSceneWorkflowGuardService,
            StoryCompatibilityProperties storyCompatibilityProperties,
            ChapterNarrativeRuntimeModeService chapterNarrativeRuntimeModeService) {
        this.storySessionOrchestrator = storySessionOrchestrator;
        this.chapterSkeletonPlanner = chapterSkeletonPlanner;
        this.chapterSkeletonGenerationService = chapterSkeletonGenerationService;
        this.chapterExecutionReviewService = chapterExecutionReviewService;
        this.chapterSkeletonMutationService = chapterSkeletonMutationService;
        this.chapterNodeRuntimeService = chapterNodeRuntimeService;
        this.chapterSceneWorkflowGuardService = chapterSceneWorkflowGuardService;
        this.storyCompatibilityProperties = storyCompatibilityProperties;
        this.chapterNarrativeRuntimeModeService = chapterNarrativeRuntimeModeService;
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
        chapterNarrativeRuntimeModeService.assertSceneMode(projectId, chapterId, "在章节工作区预览 scene 链");
        chapterSceneWorkflowGuardService.assertSceneSelectable(projectId, chapterId, sceneId);
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
        chapterNarrativeRuntimeModeService.assertSceneMode(projectId, chapterId, "继续推进 scene 链");
        chapterSceneWorkflowGuardService.assertCurrentUnlockedScene(projectId, chapterId, sceneId, "推进当前镜头");
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

    @PostMapping("/api/story-orchestration/projects/{projectId}/chapters/{chapterId}/skeleton-generate")
    public ResponseEntity<?> generateSkeleton(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestParam(defaultValue = "false") boolean forceRefresh,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        try {
            return chapterSkeletonGenerationService.generate(projectId, chapterId, forceRefresh)
                    .<ResponseEntity<?>>map(skeleton -> ResponseEntity.ok(ApiResponse.success("镜头骨架已生成", skeleton)))
                    .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error(404, "章节不存在或无法生成镜头骨架")));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(ApiResponse.error(409, ex.getMessage()));
        }
    }

    @PostMapping(value = "/api/story-orchestration/projects/{projectId}/chapters/{chapterId}/skeleton-generate-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> generateSkeletonStream(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestParam(defaultValue = "false") boolean forceRefresh,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }

        SseEmitter emitter = new SseEmitter(0L);
        Thread.startVirtualThread(() -> {
            try {
                chapterSkeletonGenerationService.generateStream(
                        projectId,
                        chapterId,
                        forceRefresh,
                        event -> sendSkeletonStreamEvent(emitter, event)
                );
            } catch (Exception exception) {
                finishSkeletonStreamWithError(emitter, ChapterSkeletonStreamEvent.error(resolveMessage(exception)));
            } finally {
                completeEmitterQuietly(emitter);
            }
        });

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
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

    @GetMapping("/api/story-orchestration/projects/{projectId}/chapters/{chapterId}/node-preview")
    public ResponseEntity<?> nodePreview(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        if (!storyCompatibilityProperties.isChapterWorkspaceNodePreviewEnabled()) {
            return ResponseEntity.status(409).body(ApiResponse.error(409, "node runtime 预览当前未开放。"));
        }
        return chapterNodeRuntimeService.preview(projectId, chapterId)
                .<ResponseEntity<?>>map(view -> ResponseEntity.ok(ApiResponse.success("获取成功", view)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error(404, "节点运行态预览不存在")));
    }

    @PostMapping("/api/story-orchestration/projects/{projectId}/chapters/{chapterId}/node-actions/resolve")
    public ResponseEntity<?> resolveNodeAction(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestBody NodeActionRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        if (!storyCompatibilityProperties.isChapterWorkspaceNodeResolveEnabled()) {
            return ResponseEntity.status(409).body(ApiResponse.error(409, "node runtime 推进当前默认关闭，避免与 scene mode 混写。"));
        }
        chapterNarrativeRuntimeModeService.assertNodeMode(projectId, chapterId, "继续推进 node runtime");
        try {
            return chapterNodeRuntimeService.resolve(new NodeActionRequest(
                            projectId,
                            chapterId,
                            requestDTO.getNodeId(),
                            requestDTO.getCheckpointId(),
                            requestDTO.getSelectedOptionId(),
                            requestDTO.getCustomAction()
                    ))
                    .<ResponseEntity<?>>map(result -> ResponseEntity.ok(ApiResponse.success("节点推进成功", result)))
                    .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error(404, "节点运行态不存在")));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(ApiResponse.error(409, ex.getMessage()));
        }
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

    private void sendSkeletonStreamEvent(SseEmitter emitter, ChapterSkeletonStreamEvent event) {
        try {
            emitter.send(SseEmitter.event().name(event.getType()).data(event));
        } catch (IOException | IllegalStateException exception) {
            throw new IllegalStateException("流式连接已中断", exception);
        }
    }

    private void finishSkeletonStreamWithError(SseEmitter emitter, ChapterSkeletonStreamEvent event) {
        try {
            emitter.send(SseEmitter.event().name(event.getType()).data(event));
        } catch (IOException | IllegalStateException ignored) {
        }
    }

    private void completeEmitterQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
        }
    }

    private String resolveMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "镜头骨架生成失败，请稍后重试" : message;
    }
}
