package com.storyweaver.storyunit.workflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.storyweaver.ai.application.support.StructuredJsonSupport;
import com.storyweaver.config.SummaryWorkflowProperties;
import com.storyweaver.service.AIModelRoutingService;
import com.storyweaver.service.AIProviderService;
import com.storyweaver.service.ProjectService;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.summary.workflow.StorySummaryConversationService;
import com.storyweaver.storyunit.summary.workflow.SummaryInputIntent;
import com.storyweaver.storyunit.summary.workflow.SummaryWorkflowChatMessage;
import com.storyweaver.storyunit.summary.workflow.SummaryWorkflowChatTurnRequest;
import com.storyweaver.storyunit.summary.workflow.SummaryWorkflowChatTurnResult;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class DefaultStorySummaryConversationService implements StorySummaryConversationService {

    private static final int MAX_HISTORY_MESSAGES = 10;
    private static final Logger log = LoggerFactory.getLogger(DefaultStorySummaryConversationService.class);

    private final ProjectService projectService;
    private final AIModelRoutingService aiModelRoutingService;
    private final AIProviderService aiProviderService;
    private final StructuredJsonSupport structuredJsonSupport;
    private final SummaryWorkflowProperties properties;
    private final ExecutorService conversationExecutor;

    public DefaultStorySummaryConversationService(
            ProjectService projectService,
            AIModelRoutingService aiModelRoutingService,
            AIProviderService aiProviderService,
            StructuredJsonSupport structuredJsonSupport,
            SummaryWorkflowProperties properties) {
        this.projectService = projectService;
        this.aiModelRoutingService = aiModelRoutingService;
        this.aiProviderService = aiProviderService;
        this.structuredJsonSupport = structuredJsonSupport;
        this.properties = properties;
        this.conversationExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @PreDestroy
    void closeExecutor() {
        conversationExecutor.close();
    }

    @Override
    public SummaryWorkflowChatTurnResult reply(SummaryWorkflowChatTurnRequest request) {
        return reply(null, request);
    }

    @Override
    public SummaryWorkflowChatTurnResult reply(Long userId, SummaryWorkflowChatTurnRequest request) {
        validateRequest(userId, request);

        List<SummaryWorkflowChatMessage> normalizedMessages = normalizeMessages(request.messages());
        if (normalizedMessages.isEmpty()) {
            return buildBootstrapResponse(request);
        }

        AIModelRoutingService.ResolvedModelSelection selection = aiModelRoutingService.resolve(
                request.selectedProviderId(),
                request.selectedModel(),
                "summary-workflow"
        );

        try {
            String rawResponse = requestConversationReply(selection, request, normalizedMessages);
            JsonNode root = structuredJsonSupport.readRoot(
                    rawResponse,
                    "摘要对话没有返回结果",
                    "摘要对话返回的结构不是有效 JSON"
            );
            return normalizeAiResult(request, selection, root, normalizedMessages);
        } catch (Exception exception) {
            log.warn(
                    "summary-workflow chat fallback triggered: projectId={}, targetType={}, intent={}, providerId={}, model={}, reason={}",
                    request.projectId(),
                    request.targetType(),
                    request.intent(),
                    selection.provider() == null ? null : selection.provider().getId(),
                    selection.model(),
                    exception.getMessage()
            );
            return buildFallbackResponse(request, selection, normalizedMessages);
        }
    }

    private void validateRequest(Long userId, SummaryWorkflowChatTurnRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (!projectService.hasProjectAccess(request.projectId(), userId)) {
            throw new IllegalArgumentException("项目不存在或无权访问");
        }
    }

    private List<SummaryWorkflowChatMessage> normalizeMessages(List<SummaryWorkflowChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        return messages.stream()
                .filter(message -> message != null && StringUtils.hasText(message.content()))
                .map(message -> new SummaryWorkflowChatMessage(normalizeRole(message.role()), message.content()))
                .skip(Math.max(messages.size() - MAX_HISTORY_MESSAGES, 0))
                .toList();
    }

    private SummaryWorkflowChatTurnResult buildBootstrapResponse(SummaryWorkflowChatTurnRequest request) {
        List<String> questions = defaultQuestions(request.targetType(), request.intent());
        String assistantMessage = switch (request.targetType()) {
            case CHARACTER -> "你先随便说这个人物的大概印象也行，比如他是谁、给人的感觉、和谁有关系。我会边问边帮你整理成摘要。";
            case WORLD_SETTING -> "你先说这个设定的大致印象，比如它是什么、会影响谁、为什么重要。我会帮你补成可用摘要。";
            case CHAPTER -> "你先说这一章你模糊想写什么就行，比如谁出场、要发生什么、想停在哪。我会边问边整理。";
            default -> "你先说个模糊印象，我会边问边整理成可用摘要。";
        };
        return new SummaryWorkflowChatTurnResult(
                assistantMessage + (questions.isEmpty() ? "" : "\n\n先回答这两个点会更快：\n- " + String.join("\n- ", questions.stream().limit(2).toList())),
                StringUtils.hasText(request.currentDraftSummary()) ? request.currentDraftSummary() : request.existingSummary(),
                questions,
                false,
                request.selectedProviderId(),
                request.selectedModel()
        );
    }

    private SummaryWorkflowChatTurnResult normalizeAiResult(
            SummaryWorkflowChatTurnRequest request,
            AIModelRoutingService.ResolvedModelSelection selection,
            JsonNode root,
            List<SummaryWorkflowChatMessage> normalizedMessages) {
        String assistantMessage = structuredJsonSupport.readText(root, "assistantMessage", "assistant_reply", "reply", "message");
        String draftSummary = structuredJsonSupport.readText(root, "draftSummary", "summary", "summaryDraft");
        List<String> pendingQuestions = structuredJsonSupport.readList(root, "pendingQuestions", "questions");
        boolean readyForPreview = structuredJsonSupport.readBoolean(root, false, "readyForPreview", "ready");

        if (!StringUtils.hasText(assistantMessage)) {
            assistantMessage = buildFallbackAssistantMessage(request, pendingQuestions);
        }
        if (!StringUtils.hasText(draftSummary)) {
            draftSummary = buildFallbackDraftSummary(request, normalizedMessages);
        }
        if (pendingQuestions.isEmpty()) {
            pendingQuestions = defaultQuestions(request.targetType(), request.intent());
        }
        if (!readyForPreview) {
            readyForPreview = draftSummary.length() >= 48 || pendingQuestions.isEmpty();
        }

        return new SummaryWorkflowChatTurnResult(
                assistantMessage,
                draftSummary,
                pendingQuestions,
                readyForPreview,
                selection.provider().getId(),
                selection.model()
        );
    }

    private SummaryWorkflowChatTurnResult buildFallbackResponse(
            SummaryWorkflowChatTurnRequest request,
            AIModelRoutingService.ResolvedModelSelection selection,
            List<SummaryWorkflowChatMessage> normalizedMessages) {
        List<String> pendingQuestions = defaultQuestions(request.targetType(), request.intent());
        String draftSummary = buildFallbackDraftSummary(request, normalizedMessages);
        boolean readyForPreview = StringUtils.hasText(draftSummary)
                && draftSummary.trim().length() >= 48;
        return new SummaryWorkflowChatTurnResult(
                buildFallbackAssistantMessage(request, pendingQuestions),
                draftSummary,
                pendingQuestions,
                readyForPreview,
                selection.provider().getId(),
                selection.model()
        );
    }

    private String requestConversationReply(
            AIModelRoutingService.ResolvedModelSelection selection,
            SummaryWorkflowChatTurnRequest request,
            List<SummaryWorkflowChatMessage> normalizedMessages) throws Exception {
        Future<String> future = conversationExecutor.submit(() -> aiProviderService.generateText(
                selection.provider(),
                selection.model(),
                buildSystemPrompt(request),
                buildUserPrompt(request, normalizedMessages),
                0.6,
                Math.max(256, properties.getConversationMaxTokens())
        ));

        int timeoutSeconds = Math.max(3, properties.getConversationTimeoutSeconds());
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new IllegalStateException("摘要对话超时，已回退到本地草稿整理", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception inner) {
                throw inner;
            }
            throw new IllegalStateException("摘要对话执行失败", cause);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("摘要对话被中断，已回退到本地草稿整理", exception);
        }
    }

    private String buildSystemPrompt(SummaryWorkflowChatTurnRequest request) {
        return """
                你是小说创作平台里的摘要采集助手。
                你的职责不是直接生成字段表单，而是通过短对话帮助用户把模糊印象整理成可用摘要。
                用户常常只知道模糊感觉、几个片段、人物关系或某个目标，你需要主动补全缺失维度，但不要替用户硬定太多细节。
                你必须遵守：
                1. 只输出 JSON，不要输出 Markdown，不要补充解释。
                2. assistantMessage 要像自然对话，控制在 2 到 5 句，最多追问两个最关键的问题。
                3. draftSummary 要写成面向作者的人类可读摘要，不要写成 JSON 字段。
                4. pendingQuestions 只保留最需要继续确认的点。
                5. 如果信息已经足够生成结构预览，readyForPreview=true；否则为 false。
                6. 绝对不能默认读者知道世界观、大纲、剧情里尚未揭晓的内容。
                7. 如果是创建对象，draftSummary 要能作为首轮创建摘要；如果是编辑对象，要尊重既有摘要并在此基础上调整。
                
                输出 JSON 结构：
                {
                  "assistantMessage": "给用户的回复",
                  "draftSummary": "整理后的摘要草稿",
                  "pendingQuestions": ["问题1", "问题2"],
                  "readyForPreview": false
                }
                
                当前对象类型：%s
                当前意图：%s
                当前模式：%s
                """.formatted(
                request.targetType().name(),
                request.intent().name(),
                request.operatorMode().name()
        );
    }

    private String buildUserPrompt(SummaryWorkflowChatTurnRequest request, List<SummaryWorkflowChatMessage> messages) {
        StringBuilder builder = new StringBuilder();
        builder.append("对象类型：").append(targetLabel(request.targetType())).append('\n');
        builder.append("意图：").append(request.intent().name()).append('\n');
        if (StringUtils.hasText(request.title())) {
            builder.append("对象标题：").append(request.title()).append('\n');
        }
        if (StringUtils.hasText(request.existingSummary())) {
            builder.append("现有摘要：\n").append(request.existingSummary()).append("\n\n");
        }
        if (StringUtils.hasText(request.currentDraftSummary())) {
            builder.append("当前草稿摘要：\n").append(request.currentDraftSummary()).append("\n\n");
        }
        builder.append("最近对话：\n");
        for (SummaryWorkflowChatMessage message : messages) {
            builder.append("- ")
                    .append("assistant".equals(message.role()) ? "助手" : "用户")
                    .append("：")
                    .append(message.content())
                    .append('\n');
        }
        builder.append('\n')
                .append("请根据这些信息：\n")
                .append("1. 给出一段自然的助手回复；\n")
                .append("2. 更新摘要草稿；\n")
                .append("3. 列出还缺的关键问题；\n")
                .append("4. 判断是否可以进入结构预览。\n");
        return builder.toString();
    }

    private String buildFallbackAssistantMessage(SummaryWorkflowChatTurnRequest request, List<String> pendingQuestions) {
        String prefix = switch (request.targetType()) {
            case CHARACTER -> "我先把这个人物的轮廓收住了。";
            case WORLD_SETTING -> "我先把这个设定的大致轮廓收住了。";
            case CHAPTER -> "我先把这一章的方向收住了。";
            default -> "我先把你的想法收住了。";
        };
        if (pendingQuestions.isEmpty()) {
            return prefix + " 现在已经能先生成结构预览了，你可以先看摘要草稿。";
        }
        return prefix + " 还差两个关键点，补完会更稳：\n- "
                + String.join("\n- ", pendingQuestions.stream().limit(2).toList());
    }

    private String buildFallbackDraftSummary(
            SummaryWorkflowChatTurnRequest request,
            List<SummaryWorkflowChatMessage> normalizedMessages) {
        LinkedHashSet<String> fragments = new LinkedHashSet<>();
        if (StringUtils.hasText(request.existingSummary())) {
            fragments.add(request.existingSummary().trim());
        }
        if (StringUtils.hasText(request.currentDraftSummary())) {
            fragments.add(request.currentDraftSummary().trim());
        }
        normalizedMessages.stream()
                .filter(message -> "user".equals(message.role()))
                .map(SummaryWorkflowChatMessage::content)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(fragments::add);

        String merged = String.join("\n", fragments);
        if (StringUtils.hasText(merged)) {
            return merged;
        }
        return switch (request.targetType()) {
            case CHARACTER -> "待补充人物摘要。";
            case WORLD_SETTING -> "待补充世界观摘要。";
            case CHAPTER -> "待补充章节摘要。";
            default -> "待补充摘要。";
        };
    }

    private List<String> defaultQuestions(StoryUnitType targetType, SummaryInputIntent intent) {
        return switch (targetType) {
            case CHARACTER -> List.of(
                    intent == SummaryInputIntent.CREATE ? "这个人物和主角是什么关系？" : "这次你最想改掉他身上的哪一部分？",
                    "他现在最想得到什么？",
                    "他给人的第一印象更接近什么？"
            );
            case WORLD_SETTING -> List.of(
                    "这个设定到底是什么？",
                    "它会直接影响谁或什么剧情？",
                    "它现在是否已经对读者揭晓？"
            );
            case CHAPTER -> List.of(
                    "这一章最少必须发生什么？",
                    "这一章读者会第一次知道什么？",
                    "这一章准备停在哪个点上？"
            );
            default -> List.of("你最确定的一点是什么？", "你还没想清的地方是什么？");
        };
    }

    private String targetLabel(StoryUnitType targetType) {
        return switch (targetType) {
            case CHARACTER -> "人物";
            case WORLD_SETTING -> "世界观";
            case CHAPTER -> "章节";
            default -> targetType.name().toLowerCase(Locale.ROOT);
        };
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "user";
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        if ("assistant".equals(normalized) || "system".equals(normalized)) {
            return normalized;
        }
        return "user";
    }
}
