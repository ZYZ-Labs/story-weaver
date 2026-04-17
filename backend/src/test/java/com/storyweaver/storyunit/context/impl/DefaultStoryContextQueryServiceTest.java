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
import com.storyweaver.storyunit.context.StoryUnitSummaryQueryService;
import com.storyweaver.storyunit.context.StoryUnitSummaryView;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultStoryContextQueryServiceTest {

    @Test
    void shouldDelegateToUnderlyingQueryServices() {
        ProjectBriefQueryService projectBriefQueryService = mock(ProjectBriefQueryService.class);
        StoryUnitSummaryQueryService storyUnitSummaryQueryService = mock(StoryUnitSummaryQueryService.class);
        ChapterAnchorBundleQueryService chapterAnchorBundleQueryService = mock(ChapterAnchorBundleQueryService.class);
        ReaderKnownStateQueryService readerKnownStateQueryService = mock(ReaderKnownStateQueryService.class);
        CharacterRuntimeStateQueryService characterRuntimeStateQueryService = mock(CharacterRuntimeStateQueryService.class);
        RecentStoryProgressQueryService recentStoryProgressQueryService = mock(RecentStoryProgressQueryService.class);

        ProjectBriefView projectBriefView = new ProjectBriefView(28L, "旧日王座", "logline", "summary");
        StoryUnitRef chapterRef = new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER);
        StoryUnitSummaryView summaryView = new StoryUnitSummaryView(chapterRef, StoryUnitType.CHAPTER, "第一章", "章节摘要");
        ChapterAnchorBundleView anchorBundleView = new ChapterAnchorBundleView(
                28L, 31L, "第一章", 11L, "卷一开篇", 9L, "林沉舟",
                List.of("林沉舟"), List.of("收到邀请"), List.of("收到邀请"), "章节摘要"
        );
        ReaderKnownStateView readerKnownStateView = new ReaderKnownStateView(28L, 31L, List.of("前章事实"), List.of("待揭晓事实"));
        CharacterRuntimeStateView runtimeStateView = new CharacterRuntimeStateView(
                28L, 9L, "林沉舟", "", "退役期", "重新面对旧战队邀请", List.of("旧手机"), List.of(), List.of("主角")
        );
        RecentStoryProgressView progressView = new RecentStoryProgressView(28L, List.of());

        when(projectBriefQueryService.getProjectBrief(28L)).thenReturn(Optional.of(projectBriefView));
        when(storyUnitSummaryQueryService.getStoryUnitSummary(chapterRef)).thenReturn(Optional.of(summaryView));
        when(chapterAnchorBundleQueryService.getChapterAnchorBundle(28L, 31L)).thenReturn(Optional.of(anchorBundleView));
        when(readerKnownStateQueryService.getReaderKnownState(28L, 31L)).thenReturn(Optional.of(readerKnownStateView));
        when(characterRuntimeStateQueryService.getCharacterRuntimeState(28L, 9L)).thenReturn(Optional.of(runtimeStateView));
        when(recentStoryProgressQueryService.getRecentStoryProgress(28L, 5)).thenReturn(progressView);

        DefaultStoryContextQueryService service = new DefaultStoryContextQueryService(
                projectBriefQueryService,
                storyUnitSummaryQueryService,
                chapterAnchorBundleQueryService,
                readerKnownStateQueryService,
                characterRuntimeStateQueryService,
                recentStoryProgressQueryService
        );

        assertEquals(projectBriefView, service.getProjectBrief(28L).orElseThrow());
        assertEquals(summaryView, service.getStoryUnitSummary(chapterRef).orElseThrow());
        assertEquals(anchorBundleView, service.getChapterAnchorBundle(28L, 31L).orElseThrow());
        assertEquals(readerKnownStateView, service.getReaderKnownState(28L, 31L).orElseThrow());
        assertEquals(runtimeStateView, service.getCharacterRuntimeState(28L, 9L).orElseThrow());
        assertEquals(progressView, service.getRecentStoryProgress(28L, 5));
    }
}
