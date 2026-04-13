package com.storyweaver.storyunit.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.ai.application.support.StructuredJsonSupport;
import com.storyweaver.config.SummaryWorkflowProperties;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.service.AIModelRoutingService;
import com.storyweaver.service.AIProviderService;
import com.storyweaver.service.ProjectService;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.summary.workflow.SummaryInputIntent;
import com.storyweaver.storyunit.summary.workflow.SummaryOperatorMode;
import com.storyweaver.storyunit.summary.workflow.SummaryWorkflowChatMessage;
import com.storyweaver.storyunit.summary.workflow.SummaryWorkflowChatTurnRequest;
import com.storyweaver.storyunit.summary.workflow.SummaryWorkflowChatTurnResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultStorySummaryConversationServiceTest {

    @Test
    void shouldReturnBootstrapResponseWhenConversationIsEmpty() {
        ProjectService projectService = mock(ProjectService.class);
        when(projectService.hasProjectAccess(28L, 100L)).thenReturn(true);

        DefaultStorySummaryConversationService service = new DefaultStorySummaryConversationService(
                projectService,
                mock(AIModelRoutingService.class),
                mock(AIProviderService.class),
                new StructuredJsonSupport(new ObjectMapper()),
                properties()
        );

        SummaryWorkflowChatTurnResult result = service.reply(
                100L,
                new SummaryWorkflowChatTurnRequest(
                        StoryUnitType.CHAPTER,
                        null,
                        28L,
                        "第一章",
                        "",
                        "",
                        SummaryInputIntent.CREATE,
                        SummaryOperatorMode.DEFAULT,
                        List.of(),
                        null,
                        null
                )
        );

        assertTrue(result.assistantMessage().contains("这一章"));
        assertFalse(result.pendingQuestions().isEmpty());
        assertFalse(result.readyForPreview());
    }

    @Test
    void shouldParseAiJsonIntoChatTurnResult() {
        ProjectService projectService = mock(ProjectService.class);
        when(projectService.hasProjectAccess(28L, 100L)).thenReturn(true);

        AIProvider provider = new AIProvider();
        provider.setId(7L);
        provider.setModelName("deepseek-chat");

        AIModelRoutingService routingService = mock(AIModelRoutingService.class);
        when(routingService.resolve(null, "", "summary-workflow"))
                .thenReturn(new AIModelRoutingService.ResolvedModelSelection(provider, "deepseek-chat"));

        AIProviderService aiProviderService = mock(AIProviderService.class);
        when(aiProviderService.generateText(any(), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenReturn("""
                        {
                          "assistantMessage": "我先帮你收住了这个人物的轮廓，还差他和主角的关系。",
                          "draftSummary": "她是个表面冷静、控制欲很强的导师型角色，会对主角形成持续压力。",
                          "pendingQuestions": ["她和主角现在是什么关系？"],
                          "readyForPreview": true
                        }
                        """);

        DefaultStorySummaryConversationService service = new DefaultStorySummaryConversationService(
                projectService,
                routingService,
                aiProviderService,
                new StructuredJsonSupport(new ObjectMapper()),
                properties()
        );

        SummaryWorkflowChatTurnResult result = service.reply(
                100L,
                new SummaryWorkflowChatTurnRequest(
                        StoryUnitType.CHARACTER,
                        null,
                        28L,
                        "导师",
                        "",
                        "",
                        SummaryInputIntent.CREATE,
                        SummaryOperatorMode.DEFAULT,
                        List.of(new SummaryWorkflowChatMessage("user", "我想要一个表面冷静但控制欲很强的导师")),
                        null,
                        ""
                )
        );

        assertEquals("deepseek-chat", result.selectedModel());
        assertEquals(7L, result.selectedProviderId());
        assertTrue(result.assistantMessage().contains("人物"));
        assertTrue(result.draftSummary().contains("导师型角色"));
        assertTrue(result.readyForPreview());
    }

    @Test
    void shouldFallbackQuicklyWhenAiConversationTimesOut() {
        ProjectService projectService = mock(ProjectService.class);
        when(projectService.hasProjectAccess(28L, 100L)).thenReturn(true);

        AIProvider provider = new AIProvider();
        provider.setId(7L);
        provider.setModelName("deepseek-chat");

        AIModelRoutingService routingService = mock(AIModelRoutingService.class);
        when(routingService.resolve(null, "", "summary-workflow"))
                .thenReturn(new AIModelRoutingService.ResolvedModelSelection(provider, "deepseek-chat"));

        AIProviderService aiProviderService = mock(AIProviderService.class);
        when(aiProviderService.generateText(any(), anyString(), anyString(), anyString(), anyDouble(), anyInt()))
                .thenAnswer(invocation -> {
                    Thread.sleep(1500L);
                    return "{\"assistantMessage\":\"slow\",\"draftSummary\":\"slow\",\"pendingQuestions\":[],\"readyForPreview\":true}";
                });

        SummaryWorkflowProperties properties = properties();
        properties.setConversationTimeoutSeconds(1);

        DefaultStorySummaryConversationService service = new DefaultStorySummaryConversationService(
                projectService,
                routingService,
                aiProviderService,
                new StructuredJsonSupport(new ObjectMapper()),
                properties
        );

        SummaryWorkflowChatTurnResult result = assertTimeoutPreemptively(Duration.ofSeconds(3), () -> service.reply(
                100L,
                new SummaryWorkflowChatTurnRequest(
                        StoryUnitType.CHARACTER,
                        null,
                        28L,
                        "经纪人",
                        "",
                        "",
                        SummaryInputIntent.CREATE,
                        SummaryOperatorMode.DEFAULT,
                        List.of(new SummaryWorkflowChatMessage("user", "我想要一个很油滑的经纪人，表面圆滑，其实特别会算计，和林沉舟以前有合作。")),
                        null,
                        ""
                )
        ));

        assertFalse(result.assistantMessage().isBlank());
        assertFalse(result.draftSummary().isBlank());
        assertFalse(result.pendingQuestions().isEmpty());
    }

    private SummaryWorkflowProperties properties() {
        return new SummaryWorkflowProperties();
    }
}
