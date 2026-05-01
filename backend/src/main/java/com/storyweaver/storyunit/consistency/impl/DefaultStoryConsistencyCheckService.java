package com.storyweaver.storyunit.consistency.impl;

import com.storyweaver.story.generation.orchestration.ChapterExecutionReview;
import com.storyweaver.story.generation.orchestration.ChapterExecutionReviewService;
import com.storyweaver.storyunit.consistency.ConsistencySeverity;
import com.storyweaver.storyunit.consistency.StoryConsistencyCheck;
import com.storyweaver.storyunit.consistency.StoryConsistencyCheckService;
import com.storyweaver.storyunit.consistency.StoryConsistencyIssue;
import com.storyweaver.storyunit.service.ChapterIncrementalStateStore;
import com.storyweaver.storyunit.service.ReaderRevealStateStore;
import com.storyweaver.storyunit.service.SceneRuntimeStateStore;
import com.storyweaver.storyunit.service.StoryEventStore;
import com.storyweaver.storyunit.service.StoryPatchStore;
import com.storyweaver.storyunit.service.StorySnapshotStore;
import com.storyweaver.storyunit.session.ReviewSeverity;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DefaultStoryConsistencyCheckService implements StoryConsistencyCheckService {

    private final SceneRuntimeStateStore sceneRuntimeStateStore;
    private final StoryEventStore storyEventStore;
    private final StorySnapshotStore storySnapshotStore;
    private final StoryPatchStore storyPatchStore;
    private final ReaderRevealStateStore readerRevealStateStore;
    private final ChapterIncrementalStateStore chapterIncrementalStateStore;
    private final ChapterExecutionReviewService chapterExecutionReviewService;

    public DefaultStoryConsistencyCheckService(
            SceneRuntimeStateStore sceneRuntimeStateStore,
            StoryEventStore storyEventStore,
            StorySnapshotStore storySnapshotStore,
            StoryPatchStore storyPatchStore,
            ReaderRevealStateStore readerRevealStateStore,
            ChapterIncrementalStateStore chapterIncrementalStateStore,
            ChapterExecutionReviewService chapterExecutionReviewService) {
        this.sceneRuntimeStateStore = sceneRuntimeStateStore;
        this.storyEventStore = storyEventStore;
        this.storySnapshotStore = storySnapshotStore;
        this.storyPatchStore = storyPatchStore;
        this.readerRevealStateStore = readerRevealStateStore;
        this.chapterIncrementalStateStore = chapterIncrementalStateStore;
        this.chapterExecutionReviewService = chapterExecutionReviewService;
    }

    @Override
    public Optional<StoryConsistencyCheck> checkChapter(Long projectId, Long chapterId) {
        List<com.storyweaver.storyunit.session.SceneExecutionState> scenes =
                sceneRuntimeStateStore.listChapterScenes(projectId, chapterId);
        List<com.storyweaver.storyunit.session.SceneHandoffSnapshot> handoffs =
                sceneRuntimeStateStore.listChapterHandoffs(projectId, chapterId);
        List<com.storyweaver.storyunit.event.StoryEvent> events =
                storyEventStore.listChapterEvents(projectId, chapterId);
        List<com.storyweaver.storyunit.snapshot.StorySnapshot> snapshots =
                storySnapshotStore.listChapterSnapshots(projectId, chapterId);
        List<com.storyweaver.storyunit.patch.StoryPatch> patches =
                storyPatchStore.listChapterPatches(projectId, chapterId);

        boolean hasReaderRevealState = readerRevealStateStore.findChapterRevealState(projectId, chapterId).isPresent();
        boolean hasChapterState = chapterIncrementalStateStore.findChapterState(projectId, chapterId).isPresent();
        Optional<ChapterExecutionReview> review = chapterExecutionReviewService.review(projectId, chapterId);

        int completedSceneCount = (int) scenes.stream()
                .filter(scene -> scene.status() == SceneExecutionStatus.COMPLETED)
                .count();

        List<StoryConsistencyIssue> issues = new ArrayList<>();
        if (scenes.isEmpty()) {
            issues.add(new StoryConsistencyIssue("no_scene_state", ConsistencySeverity.INFO, "当前章节还没有 scene runtime state。"));
        }
        if (!scenes.isEmpty() && events.isEmpty()) {
            issues.add(new StoryConsistencyIssue("missing_events", ConsistencySeverity.WARNING, "当前章节已有 scene runtime state，但还没有 StoryEvent 基线。"));
        }
        if (!scenes.isEmpty() && snapshots.isEmpty()) {
            issues.add(new StoryConsistencyIssue("missing_snapshots", ConsistencySeverity.WARNING, "当前章节已有 scene runtime state，但还没有 StorySnapshot 基线。"));
        }
        if (completedSceneCount > 0 && patches.isEmpty()) {
            issues.add(new StoryConsistencyIssue("missing_patches", ConsistencySeverity.INFO, "当前章节已有已完成镜头，但还没有 StoryPatch 基线。"));
        }
        if (!hasReaderRevealState) {
            issues.add(new StoryConsistencyIssue("missing_reader_reveal_state", ConsistencySeverity.WARNING, "当前章节还没有 ReaderRevealState。"));
        }
        if (!hasChapterState) {
            issues.add(new StoryConsistencyIssue("missing_chapter_state", ConsistencySeverity.WARNING, "当前章节还没有 ChapterIncrementalState。"));
        }
        review.ifPresent(chapterReview -> appendReviewIssues(issues, chapterReview));

        return Optional.of(new StoryConsistencyCheck(
                projectId,
                chapterId,
                scenes.size(),
                completedSceneCount,
                handoffs.size(),
                events.size(),
                snapshots.size(),
                patches.size(),
                hasReaderRevealState,
                hasChapterState,
                review.isPresent(),
                review.map(value -> value.result().name()).orElse(""),
                review.map(ChapterExecutionReview::summary).orElse(""),
                issues
        ));
    }

    private static void appendReviewIssues(List<StoryConsistencyIssue> issues, ChapterExecutionReview chapterReview) {
        if (!chapterReview.chapterExecutionComplete()) {
            issues.add(new StoryConsistencyIssue(
                    "chapter_review_incomplete",
                    ConsistencySeverity.WARNING,
                    chapterReview.summary().isBlank() ? "章节执行尚未收口。" : chapterReview.summary()
            ));
        }
        chapterReview.issues().forEach(issue -> issues.add(new StoryConsistencyIssue(
                "chapter_review:" + issue.code(),
                mapSeverity(issue.severity()),
                issue.message()
        )));
    }

    private static ConsistencySeverity mapSeverity(ReviewSeverity severity) {
        return switch (severity) {
            case INFO -> ConsistencySeverity.INFO;
            case WARNING -> ConsistencySeverity.WARNING;
            case ERROR, BLOCKER -> ConsistencySeverity.ERROR;
        };
    }
}
