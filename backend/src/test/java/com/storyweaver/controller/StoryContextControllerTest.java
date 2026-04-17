package com.storyweaver.controller;

import com.storyweaver.exception.GlobalExceptionHandler;
import com.storyweaver.storyunit.context.ChapterAnchorBundleView;
import com.storyweaver.storyunit.context.CharacterRuntimeStateView;
import com.storyweaver.storyunit.context.ProjectBriefView;
import com.storyweaver.storyunit.context.ReaderKnownStateView;
import com.storyweaver.storyunit.context.RecentStoryProgressItemView;
import com.storyweaver.storyunit.context.RecentStoryProgressView;
import com.storyweaver.storyunit.context.StoryContextQueryService;
import com.storyweaver.storyunit.context.StoryUnitSummaryView;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StoryContextControllerTest {

    private MockMvc mockMvc;
    private StoryContextQueryService storyContextQueryService;

    @BeforeEach
    void setUp() {
        storyContextQueryService = mock(StoryContextQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new StoryContextController(storyContextQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthorizationHeaderMissing() throws Exception {
        mockMvc.perform(get("/api/story-context/projects/28/brief"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturnProjectBriefAndProgress() throws Exception {
        when(storyContextQueryService.getProjectBrief(28L)).thenReturn(Optional.of(
                new ProjectBriefView(28L, "旧日王座", "退役选手重返职业圈", "关联世界观：旧日王座职业圈")
        ));
        when(storyContextQueryService.getRecentStoryProgress(28L, 5)).thenReturn(new RecentStoryProgressView(
                28L,
                List.of(
                        new RecentStoryProgressItemView("chapter", 31L, "第一章 雨夜来信", "林沉舟收到邀请", "draft", LocalDateTime.now()),
                        new RecentStoryProgressItemView("character", 9L, "林沉舟", "退役两年的前职业选手", "主角", LocalDateTime.now())
                )
        ));

        mockMvc.perform(get("/api/story-context/projects/28/brief")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projectTitle").value("旧日王座"));

        mockMvc.perform(get("/api/story-context/projects/28/progress")
                        .param("limit", "5")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projectId").value(28))
                .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    void shouldReturnStoryUnitSummaryAndChapterContext() throws Exception {
        StoryUnitRef ref = new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER);
        when(storyContextQueryService.getStoryUnitSummary(ref)).thenReturn(Optional.of(
                new StoryUnitSummaryView(ref, StoryUnitType.CHAPTER, "第二章 赴约之前", "林沉舟准备赴约，但还没说出决定。")
        ));
        when(storyContextQueryService.getChapterAnchorBundle(28L, 31L)).thenReturn(Optional.of(
                new ChapterAnchorBundleView(
                        28L, 31L, "第二章 赴约之前", null, "",
                        9L, "林沉舟", List.of("林沉舟"), List.of("收到邀请"), List.of("收到邀请"),
                        "林沉舟准备赴约，但还没说出决定。"
                )
        ));
        when(storyContextQueryService.getReaderKnownState(28L, 31L)).thenReturn(Optional.of(
                new ReaderKnownStateView(28L, 31L, List.of("第一章 雨夜来信：林沉舟收到邀请"), List.of("本章待揭晓：他是否赴约"))
        ));

        mockMvc.perform(get("/api/story-context/story-units/summary")
                        .param("unitId", "31")
                        .param("unitKey", "chapter:31")
                        .param("unitType", "CHAPTER")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("第二章 赴约之前"));

        mockMvc.perform(get("/api/story-context/projects/28/chapters/31/anchors")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.mainPovCharacterName").value("林沉舟"));

        mockMvc.perform(get("/api/story-context/projects/28/chapters/31/reader-known-state")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.knownFacts[0]").value("第一章 雨夜来信：林沉舟收到邀请"));
    }

    @Test
    void shouldReturnCharacterRuntimeStateAndNotFoundWhenMissing() throws Exception {
        when(storyContextQueryService.getCharacterRuntimeState(28L, 9L)).thenReturn(Optional.of(
                new CharacterRuntimeStateView(
                        28L, 9L, "林沉舟", "", "退役期", "决定是否回应旧战队邀请",
                        List.of("旧手机"), List.of(), List.of("主角", "退役")
                )
        ));
        when(storyContextQueryService.getProjectBrief(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/story-context/projects/28/characters/9/runtime-state")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.characterName").value("林沉舟"))
                .andExpect(jsonPath("$.data.stateTags[0]").value("主角"));

        mockMvc.perform(get("/api/story-context/projects/404/brief")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
