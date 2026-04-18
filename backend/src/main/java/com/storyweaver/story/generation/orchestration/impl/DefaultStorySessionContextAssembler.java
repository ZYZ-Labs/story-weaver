package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.SceneBindingContext;
import com.storyweaver.story.generation.orchestration.SceneBindingMode;
import com.storyweaver.story.generation.orchestration.StorySessionContextAssembler;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.storyunit.context.CharacterRuntimeStateView;
import com.storyweaver.storyunit.context.StoryContextQueryService;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.service.SceneExecutionStateQueryService;
import com.storyweaver.storyunit.service.SceneRuntimeStateStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DefaultStorySessionContextAssembler implements StorySessionContextAssembler {

    private final StoryContextQueryService storyContextQueryService;
    private final ObjectProvider<SceneExecutionStateQueryService> sceneExecutionStateQueryServiceProvider;
    private final ObjectProvider<SceneRuntimeStateStore> sceneRuntimeStateStoreProvider;

    public DefaultStorySessionContextAssembler(
            StoryContextQueryService storyContextQueryService,
            ObjectProvider<SceneExecutionStateQueryService> sceneExecutionStateQueryServiceProvider,
            ObjectProvider<SceneRuntimeStateStore> sceneRuntimeStateStoreProvider) {
        this.storyContextQueryService = storyContextQueryService;
        this.sceneExecutionStateQueryServiceProvider = sceneExecutionStateQueryServiceProvider;
        this.sceneRuntimeStateStoreProvider = sceneRuntimeStateStoreProvider;
    }

    @Override
    public Optional<StorySessionContextPacket> assemble(Long projectId, Long chapterId, String sceneId) {
        if (projectId == null || chapterId == null) {
            return Optional.empty();
        }

        Optional<com.storyweaver.storyunit.context.ProjectBriefView> projectBrief = storyContextQueryService.getProjectBrief(projectId);
        Optional<com.storyweaver.storyunit.context.StoryUnitSummaryView> chapterSummary = storyContextQueryService.getStoryUnitSummary(
                new StoryUnitRef(String.valueOf(chapterId), "chapter:" + chapterId, StoryUnitType.CHAPTER)
        );
        Optional<com.storyweaver.storyunit.context.ChapterAnchorBundleView> chapterAnchorBundle =
                storyContextQueryService.getChapterAnchorBundle(projectId, chapterId);
        Optional<com.storyweaver.storyunit.context.ReaderKnownStateView> readerKnownState =
                storyContextQueryService.getReaderKnownState(projectId, chapterId);

        if (projectBrief.isEmpty() || chapterSummary.isEmpty() || chapterAnchorBundle.isEmpty() || readerKnownState.isEmpty()) {
            return Optional.empty();
        }

        List<CharacterRuntimeStateView> characterRuntimeStates = new ArrayList<>();
        Long mainPovCharacterId = chapterAnchorBundle.get().mainPovCharacterId();
        if (mainPovCharacterId != null) {
            storyContextQueryService.getCharacterRuntimeState(projectId, mainPovCharacterId)
                    .ifPresent(characterRuntimeStates::add);
        }

        List<com.storyweaver.storyunit.session.SceneExecutionState> existingSceneStates = List.of();
        SceneBindingContext sceneBindingContext;
        SceneExecutionStateQueryService sceneExecutionStateQueryService = sceneExecutionStateQueryServiceProvider.getIfAvailable();
        if (sceneExecutionStateQueryService != null) {
            existingSceneStates = sceneExecutionStateQueryService.listChapterScenes(projectId, chapterId);
            Optional<com.storyweaver.storyunit.session.SceneExecutionState> requestedSceneState =
                    sceneExecutionStateQueryService.getSceneState(projectId, chapterId, sceneId);
            Optional<com.storyweaver.storyunit.session.SceneExecutionState> latestSceneState =
                    sceneExecutionStateQueryService.findLatestChapterScene(projectId, chapterId);
            sceneBindingContext = buildSceneBindingContext(sceneId, existingSceneStates, requestedSceneState, latestSceneState);
        } else {
            sceneBindingContext = new SceneBindingContext(
                    sceneId,
                    "",
                    SceneBindingMode.SCENE_QUERY_UNAVAILABLE,
                    false,
                    "当前未接入 scene 执行状态查询，sceneId 仅做参数透传。",
                    null
            );
        }

        com.storyweaver.storyunit.session.SceneHandoffSnapshot previousSceneHandoff = resolvePreviousSceneHandoff(
                projectId,
                chapterId,
                sceneId,
                sceneBindingContext
        );

        return Optional.of(new StorySessionContextPacket(
                projectId,
                chapterId,
                sceneId,
                sceneBindingContext,
                projectBrief.get(),
                chapterSummary.get(),
                chapterAnchorBundle.get(),
                readerKnownState.get(),
                storyContextQueryService.getRecentStoryProgress(projectId, 5),
                characterRuntimeStates,
                previousSceneHandoff,
                existingSceneStates
        ));
    }

    private SceneBindingContext buildSceneBindingContext(
            String requestedSceneId,
            List<com.storyweaver.storyunit.session.SceneExecutionState> existingSceneStates,
            Optional<com.storyweaver.storyunit.session.SceneExecutionState> requestedSceneState,
            Optional<com.storyweaver.storyunit.session.SceneExecutionState> latestSceneState) {
        if (requestedSceneState.isPresent()) {
            com.storyweaver.storyunit.session.SceneExecutionState resolvedSceneState = requestedSceneState.get();
            return new SceneBindingContext(
                    requestedSceneId,
                    resolvedSceneState.sceneId(),
                    SceneBindingMode.SCENE_BOUND,
                    false,
                    "已绑定请求的 scene 执行状态。",
                    resolvedSceneState
            );
        }
        if (existingSceneStates.isEmpty()) {
            return new SceneBindingContext(
                    requestedSceneId,
                    "",
                    SceneBindingMode.CHAPTER_COLD_START,
                    false,
                    "当前章节暂无 scene 执行状态，按冷启动处理。",
                    null
            );
        }
        if (latestSceneState.isPresent()) {
            com.storyweaver.storyunit.session.SceneExecutionState resolvedSceneState = latestSceneState.get();
            return new SceneBindingContext(
                    requestedSceneId,
                    resolvedSceneState.sceneId(),
                    SceneBindingMode.SCENE_FALLBACK_TO_LATEST,
                    true,
                    "未找到请求的 sceneId，当前回退到最近 scene 上下文。",
                    resolvedSceneState
            );
        }
        return new SceneBindingContext(
                requestedSceneId,
                "",
                SceneBindingMode.SCENE_QUERY_UNAVAILABLE,
                false,
                "scene 查询可用，但当前没有可绑定的 scene 执行状态。",
                null
        );
    }

    private com.storyweaver.storyunit.session.SceneHandoffSnapshot resolvePreviousSceneHandoff(
            Long projectId,
            Long chapterId,
            String requestedSceneId,
            SceneBindingContext sceneBindingContext) {
        SceneRuntimeStateStore runtimeStateStore = sceneRuntimeStateStoreProvider.getIfAvailable();
        if (runtimeStateStore == null) {
            return null;
        }
        String targetSceneId = StringUtils.hasText(requestedSceneId) ? requestedSceneId.trim() : sceneBindingContext.resolvedSceneId();
        if (!StringUtils.hasText(targetSceneId)) {
            return null;
        }
        return runtimeStateStore.findHandoffToScene(projectId, chapterId, targetSceneId).orElse(null);
    }
}
