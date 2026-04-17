package com.storyweaver.controller;

import com.storyweaver.exception.GlobalExceptionHandler;
import com.storyweaver.story.generation.orchestration.SessionExecutionTrace;
import com.storyweaver.story.generation.orchestration.SessionExecutionTraceItem;
import com.storyweaver.story.generation.orchestration.SceneBindingContext;
import com.storyweaver.story.generation.orchestration.SceneBindingMode;
import com.storyweaver.story.generation.orchestration.SessionTraceStatus;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.story.generation.orchestration.StorySessionOrchestrator;
import com.storyweaver.story.generation.orchestration.StorySessionPreview;
import com.storyweaver.story.generation.orchestration.WriterSessionResult;
import com.storyweaver.storyunit.context.ChapterAnchorBundleView;
import com.storyweaver.storyunit.context.ProjectBriefView;
import com.storyweaver.storyunit.context.ReaderKnownStateView;
import com.storyweaver.storyunit.context.RecentStoryProgressView;
import com.storyweaver.storyunit.context.StoryUnitSummaryView;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.session.DirectorCandidate;
import com.storyweaver.storyunit.session.DirectorCandidateType;
import com.storyweaver.storyunit.session.ReviewDecision;
import com.storyweaver.storyunit.session.ReviewResult;
import com.storyweaver.storyunit.session.SelectionDecision;
import com.storyweaver.storyunit.session.SessionRole;
import com.storyweaver.storyunit.session.WriterExecutionBrief;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StorySessionOrchestrationControllerTest {

    private MockMvc mockMvc;
    private StorySessionOrchestrator storySessionOrchestrator;

    @BeforeEach
    void setUp() {
        storySessionOrchestrator = mock(StorySessionOrchestrator.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new StorySessionOrchestrationController(storySessionOrchestrator))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthorizationHeaderMissing() throws Exception {
        mockMvc.perform(get("/api/story-orchestration/projects/28/chapters/31/preview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturnPreviewWhenAvailable() throws Exception {
        StorySessionContextPacket contextPacket = new StorySessionContextPacket(
                28L,
                31L,
                "scene-1",
                new SceneBindingContext("scene-1", "", SceneBindingMode.CHAPTER_COLD_START, false, "当前章节暂无 scene 执行状态，按冷启动处理。", null),
                new ProjectBriefView(28L, "旧日王座", "logline", "summary"),
                new StoryUnitSummaryView(new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER), StoryUnitType.CHAPTER, "退役者的邀请函", "章节摘要"),
                new ChapterAnchorBundleView(28L, 31L, "退役者的邀请函", 9L, "第一卷", 15L, "林沉舟", List.of("林沉舟"), List.of("剧情"), List.of("剧情"), "章节摘要"),
                new ReaderKnownStateView(28L, 31L, List.of(), List.of("待揭晓")),
                new RecentStoryProgressView(28L, List.of()),
                List.of(),
                List.of()
        );
        StorySessionPreview preview = new StorySessionPreview(
                contextPacket,
                List.of(new DirectorCandidate("opening-31", DirectorCandidateType.OPENING, "先做开场定向", List.of("待揭晓"), List.of("pov=林沉舟"), List.of("不要跳过开场"), "完成触发点后停住。", 900, "首段优先开场")),
                new SelectionDecision("opening-31", "当前是第一段，优先开场。", List.of(), List.of()),
                new WriterExecutionBrief(28L, 31L, "scene-1", "opening-31", "先做开场定向", List.of("待揭晓"), List.of("pov=林沉舟"), List.of("不要跳过开场"), "完成触发点后停住。", 900, List.of(), ""),
                new WriterSessionResult("scene-1", "opening-31", "【目标】先做开场定向\n\n【收束点】完成触发点后停住。", "退役者的邀请函 / 先做开场定向"),
                new ReviewDecision("scene-1", ReviewResult.PASS, "规则审校通过。", List.of(), false, ""),
                new SessionExecutionTrace(28L, 31L, "scene-1", List.of(
                        new SessionExecutionTraceItem(SessionRole.ORCHESTRATOR, "context-scene-binding", SessionTraceStatus.COMPLETED, "当前章节暂无 scene 执行状态，按冷启动处理。", "sceneBindingContext", 1, false, java.util.Map.of()),
                        new SessionExecutionTraceItem(SessionRole.DIRECTOR, "director-candidates", SessionTraceStatus.COMPLETED, "已生成 1 个候选。", "directorCandidates", 1, false, java.util.Map.of("candidateCount", 1))
                ))
        );
        when(storySessionOrchestrator.preview(28L, 31L, "scene-1")).thenReturn(Optional.of(preview));

        mockMvc.perform(get("/api/story-orchestration/projects/28/chapters/31/preview")
                        .param("sceneId", "scene-1")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.selectionDecision.chosenCandidateId").value("opening-31"))
                .andExpect(jsonPath("$.data.reviewDecision.result").value("PASS"))
                .andExpect(jsonPath("$.data.trace.items[0].role").value("ORCHESTRATOR"))
                .andExpect(jsonPath("$.data.trace.items[0].attempt").value(1))
                .andExpect(jsonPath("$.data.trace.items[1].role").value("DIRECTOR"));
    }
}
