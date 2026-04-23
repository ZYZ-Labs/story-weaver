package com.storyweaver.controller;

import com.storyweaver.common.web.ApiResponse;
import com.storyweaver.storyunit.migration.LegacyBackfillDryRunService;
import com.storyweaver.storyunit.migration.LegacyBackfillAnalysisService;
import com.storyweaver.storyunit.migration.LegacyBackfillExecutionService;
import com.storyweaver.storyunit.migration.LegacyBackfillOverviewService;
import com.storyweaver.storyunit.migration.MigrationCompatibilitySnapshotService;
import com.storyweaver.storyunit.service.ChapterIncrementalStateStore;
import com.storyweaver.storyunit.service.ReaderRevealStateStore;
import com.storyweaver.storyunit.service.StoryEventStore;
import com.storyweaver.storyunit.service.StoryPatchStore;
import com.storyweaver.storyunit.service.StorySnapshotStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StoryStateController {

    private final StoryEventStore storyEventStore;
    private final StorySnapshotStore storySnapshotStore;
    private final StoryPatchStore storyPatchStore;
    private final ReaderRevealStateStore readerRevealStateStore;
    private final ChapterIncrementalStateStore chapterIncrementalStateStore;
    private final LegacyBackfillAnalysisService legacyBackfillAnalysisService;
    private final LegacyBackfillDryRunService legacyBackfillDryRunService;
    private final LegacyBackfillExecutionService legacyBackfillExecutionService;
    private final LegacyBackfillOverviewService legacyBackfillOverviewService;
    private final MigrationCompatibilitySnapshotService migrationCompatibilitySnapshotService;

    public StoryStateController(
            StoryEventStore storyEventStore,
            StorySnapshotStore storySnapshotStore,
            StoryPatchStore storyPatchStore,
            ReaderRevealStateStore readerRevealStateStore,
            ChapterIncrementalStateStore chapterIncrementalStateStore,
            LegacyBackfillAnalysisService legacyBackfillAnalysisService,
            LegacyBackfillDryRunService legacyBackfillDryRunService,
            LegacyBackfillExecutionService legacyBackfillExecutionService,
            LegacyBackfillOverviewService legacyBackfillOverviewService,
            MigrationCompatibilitySnapshotService migrationCompatibilitySnapshotService) {
        this.storyEventStore = storyEventStore;
        this.storySnapshotStore = storySnapshotStore;
        this.storyPatchStore = storyPatchStore;
        this.readerRevealStateStore = readerRevealStateStore;
        this.chapterIncrementalStateStore = chapterIncrementalStateStore;
        this.legacyBackfillAnalysisService = legacyBackfillAnalysisService;
        this.legacyBackfillDryRunService = legacyBackfillDryRunService;
        this.legacyBackfillExecutionService = legacyBackfillExecutionService;
        this.legacyBackfillOverviewService = legacyBackfillOverviewService;
        this.migrationCompatibilitySnapshotService = migrationCompatibilitySnapshotService;
    }

    @GetMapping("/api/story-state/projects/{projectId}/chapters/{chapterId}/events")
    public ResponseEntity<?> chapterEvents(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return ResponseEntity.ok(ApiResponse.success("获取成功", storyEventStore.listChapterEvents(projectId, chapterId)));
    }

    @GetMapping("/api/story-state/projects/{projectId}/chapters/{chapterId}/snapshots")
    public ResponseEntity<?> chapterSnapshots(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return ResponseEntity.ok(ApiResponse.success("获取成功", storySnapshotStore.listChapterSnapshots(projectId, chapterId)));
    }

    @GetMapping("/api/story-state/projects/{projectId}/chapters/{chapterId}/patches")
    public ResponseEntity<?> chapterPatches(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return ResponseEntity.ok(ApiResponse.success("获取成功", storyPatchStore.listChapterPatches(projectId, chapterId)));
    }

    @GetMapping("/api/story-state/projects/{projectId}/chapters/{chapterId}/reader-reveal-state")
    public ResponseEntity<?> chapterReaderRevealState(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return ResponseEntity.ok(ApiResponse.success(
                "获取成功",
                readerRevealStateStore.findChapterRevealState(projectId, chapterId).orElse(null)
        ));
    }

    @GetMapping("/api/story-state/projects/{projectId}/chapters/{chapterId}/chapter-state")
    public ResponseEntity<?> chapterState(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return ResponseEntity.ok(ApiResponse.success(
                "获取成功",
                chapterIncrementalStateStore.findChapterState(projectId, chapterId).orElse(null)
        ));
    }

    @GetMapping("/api/story-state/projects/{projectId}/chapters/{chapterId}/backfill-analysis")
    public ResponseEntity<?> chapterBackfillAnalysis(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return ResponseEntity.ok(ApiResponse.success(
                "获取成功",
                legacyBackfillAnalysisService.analyzeChapter(projectId, chapterId).orElse(null)
        ));
    }

    @GetMapping("/api/story-state/projects/{projectId}/chapters/{chapterId}/backfill-dry-run")
    public ResponseEntity<?> chapterBackfillDryRun(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return ResponseEntity.ok(ApiResponse.success(
                "获取成功",
                legacyBackfillDryRunService.planChapterBackfill(projectId, chapterId).orElse(null)
        ));
    }

    @org.springframework.web.bind.annotation.PostMapping("/api/story-state/projects/{projectId}/chapters/{chapterId}/backfill-execute")
    public ResponseEntity<?> chapterBackfillExecute(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return ResponseEntity.ok(ApiResponse.success(
                "执行成功",
                legacyBackfillExecutionService.executeChapterBackfill(projectId, chapterId).orElse(null)
        ));
    }

    @GetMapping("/api/story-state/projects/{projectId}/chapters/{chapterId}/compatibility-snapshot")
    public ResponseEntity<?> chapterCompatibilitySnapshot(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return ResponseEntity.ok(ApiResponse.success(
                "获取成功",
                migrationCompatibilitySnapshotService.getChapterSnapshot(projectId, chapterId).orElse(null)
        ));
    }

    @GetMapping("/api/story-state/projects/{projectId}/backfill-overview")
    public ResponseEntity<?> projectBackfillOverview(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        return ResponseEntity.ok(ApiResponse.success(
                "获取成功",
                legacyBackfillOverviewService.buildProjectOverview(projectId).orElse(null)
        ));
    }
}
