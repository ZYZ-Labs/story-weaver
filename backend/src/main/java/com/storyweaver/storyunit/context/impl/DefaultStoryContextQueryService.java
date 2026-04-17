package com.storyweaver.storyunit.context.impl;

import com.storyweaver.storyunit.context.ChapterAnchorBundleQueryService;
import com.storyweaver.storyunit.context.ChapterAnchorBundleView;
import com.storyweaver.storyunit.context.CharacterRuntimeStateQueryService;
import com.storyweaver.storyunit.context.CharacterRuntimeStateView;
import com.storyweaver.storyunit.context.ProjectBriefQueryService;
import com.storyweaver.storyunit.context.ProjectBriefView;
import com.storyweaver.storyunit.context.ReaderKnownStateQueryService;
import com.storyweaver.storyunit.context.ReaderKnownStateView;
import com.storyweaver.storyunit.context.RecentStoryProgressQueryService;
import com.storyweaver.storyunit.context.RecentStoryProgressView;
import com.storyweaver.storyunit.context.StoryContextQueryService;
import com.storyweaver.storyunit.context.StoryUnitSummaryQueryService;
import com.storyweaver.storyunit.context.StoryUnitSummaryView;
import com.storyweaver.storyunit.model.StoryUnitRef;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DefaultStoryContextQueryService implements StoryContextQueryService {

    private final ProjectBriefQueryService projectBriefQueryService;
    private final StoryUnitSummaryQueryService storyUnitSummaryQueryService;
    private final ChapterAnchorBundleQueryService chapterAnchorBundleQueryService;
    private final ReaderKnownStateQueryService readerKnownStateQueryService;
    private final CharacterRuntimeStateQueryService characterRuntimeStateQueryService;
    private final RecentStoryProgressQueryService recentStoryProgressQueryService;

    public DefaultStoryContextQueryService(
            ProjectBriefQueryService projectBriefQueryService,
            StoryUnitSummaryQueryService storyUnitSummaryQueryService,
            ChapterAnchorBundleQueryService chapterAnchorBundleQueryService,
            ReaderKnownStateQueryService readerKnownStateQueryService,
            CharacterRuntimeStateQueryService characterRuntimeStateQueryService,
            RecentStoryProgressQueryService recentStoryProgressQueryService) {
        this.projectBriefQueryService = projectBriefQueryService;
        this.storyUnitSummaryQueryService = storyUnitSummaryQueryService;
        this.chapterAnchorBundleQueryService = chapterAnchorBundleQueryService;
        this.readerKnownStateQueryService = readerKnownStateQueryService;
        this.characterRuntimeStateQueryService = characterRuntimeStateQueryService;
        this.recentStoryProgressQueryService = recentStoryProgressQueryService;
    }

    @Override
    public Optional<ProjectBriefView> getProjectBrief(Long projectId) {
        return projectBriefQueryService.getProjectBrief(projectId);
    }

    @Override
    public Optional<StoryUnitSummaryView> getStoryUnitSummary(StoryUnitRef ref) {
        return storyUnitSummaryQueryService.getStoryUnitSummary(ref);
    }

    @Override
    public Optional<ChapterAnchorBundleView> getChapterAnchorBundle(Long projectId, Long chapterId) {
        return chapterAnchorBundleQueryService.getChapterAnchorBundle(projectId, chapterId);
    }

    @Override
    public Optional<ReaderKnownStateView> getReaderKnownState(Long projectId, Long chapterId) {
        return readerKnownStateQueryService.getReaderKnownState(projectId, chapterId);
    }

    @Override
    public Optional<CharacterRuntimeStateView> getCharacterRuntimeState(Long projectId, Long characterId) {
        return characterRuntimeStateQueryService.getCharacterRuntimeState(projectId, characterId);
    }

    @Override
    public RecentStoryProgressView getRecentStoryProgress(Long projectId, int limit) {
        return recentStoryProgressQueryService.getRecentStoryProgress(projectId, limit);
    }
}
