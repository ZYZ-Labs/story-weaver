package com.storyweaver.story.generation.orchestration.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.storyweaver.ai.application.support.StructuredJsonSupport;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.service.AIModelRoutingService;
import com.storyweaver.service.AIProviderService;
import com.storyweaver.storyunit.session.SceneContinuityState;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class AIContinuityStateService {

    private static final int EXTRACTION_MAX_TOKENS = 640;
    private static final int INSPECTION_MAX_TOKENS = 320;
    private static final int FACT_LIMIT = 4;
    private static final int NAME_LIMIT = 6;

    private final AIModelRoutingService aiModelRoutingService;
    private final AIProviderService aiProviderService;
    private final StructuredJsonSupport structuredJsonSupport;

    public AIContinuityStateService(
            AIModelRoutingService aiModelRoutingService,
            AIProviderService aiProviderService,
            StructuredJsonSupport structuredJsonSupport) {
        this.aiModelRoutingService = aiModelRoutingService;
        this.aiProviderService = aiProviderService;
        this.structuredJsonSupport = structuredJsonSupport;
    }

    public SceneContinuityState extractAcceptedContinuityState(
            String sceneId,
            String acceptedContent,
            String summary,
            String handoffLine,
            List<String> readerReveal,
            String nextSceneId,
            String nextSceneGoal,
            String stopCondition,
            SceneContinuityState previousState) {
        SceneContinuityState fallbackState = SceneContinuitySupport.buildAcceptedContinuityState(
                sceneId,
                acceptedContent,
                summary,
                handoffLine,
                readerReveal,
                nextSceneId,
                nextSceneGoal,
                stopCondition,
                previousState
        );
        try {
            AIModelRoutingService.ResolvedModelSelection selection = aiModelRoutingService.resolve(
                    null,
                    null,
                    "phase8.chapter-workspace.scene-draft.continuity"
            );
            String rawResponse = aiProviderService.generateText(
                    selection.provider(),
                    selection.model(),
                    buildExtractionSystemPrompt(),
                    buildExtractionUserPrompt(acceptedContent, summary, handoffLine, readerReveal, nextSceneGoal, stopCondition, previousState),
                    0.1,
                    EXTRACTION_MAX_TOKENS
            );
            JsonNode root = structuredJsonSupport.readRoot(
                    rawResponse,
                    "镜头连续性抽取没有返回内容",
                    "镜头连续性抽取结果不是有效 JSON"
            );
            return SceneContinuitySupport.mergeAcceptedContinuityState(
                    sceneId,
                    summary,
                    handoffLine,
                    limitDistinct(structuredJsonSupport.readList(root, "carryForwardFacts", "facts"), FACT_LIMIT),
                    limitDistinct(structuredJsonSupport.readList(root, "timeAnchors", "timeStates"), FACT_LIMIT),
                    limitDistinct(structuredJsonSupport.readList(root, "expectedNames", "allNames"), NAME_LIMIT),
                    limitDistinct(structuredJsonSupport.readList(root, "counterpartNames", "activeCounterparts"), NAME_LIMIT),
                    structuredJsonSupport.readBoolean(root, false, "requiresExplicitTimeTransition", "requiresTimeTransition", "needsTimeTransition"),
                    nextSceneId,
                    nextSceneGoal,
                    stopCondition,
                    previousState
            );
        } catch (Exception ignored) {
            return fallbackState;
        }
    }

    public List<String> inspectGeneratedScene(
            AIProvider provider,
            String modelName,
            SceneContinuityState continuityState,
            String content,
            String currentSceneGoal,
            String currentStopCondition,
            String nextSceneId,
            String nextSceneGoal) {
        List<String> fallbackIssues = SceneContinuitySupport.detectContinuityIssues(
                continuityState,
                content,
                currentSceneGoal,
                currentStopCondition,
                nextSceneId,
                nextSceneGoal
        );
        if (continuityState == null || continuityState.isEmpty() || !StringUtils.hasText(content)) {
            return fallbackIssues;
        }
        if (provider == null || !StringUtils.hasText(modelName)) {
            return fallbackIssues;
        }
        try {
            String rawResponse = aiProviderService.generateText(
                    provider,
                    modelName,
                    buildInspectionSystemPrompt(),
                    buildInspectionUserPrompt(continuityState, content, currentSceneGoal, currentStopCondition, nextSceneGoal),
                    0.0,
                    INSPECTION_MAX_TOKENS
            );
            JsonNode root = structuredJsonSupport.readRoot(
                    rawResponse,
                    "镜头连续性审查没有返回内容",
                    "镜头连续性审查结果不是有效 JSON"
            );
            List<String> issues = new ArrayList<>();
            if (structuredJsonSupport.readBoolean(root, false, "timeTransitionConflict", "timeConflict")) {
                issues.add("上一镜头仍停留在未来时间锚点，但当前正文已经写成事件已发生，缺少明确时间跳转。");
            }
            if (structuredJsonSupport.readBoolean(root, false, "counterpartDrift", "nameDrift")) {
                issues.add("当前正文把上一镜头已经确认的会话对象或称呼改写成了其他人。");
            }
            if (structuredJsonSupport.readBoolean(root, false, "advanceIntoNextScene", "nextSceneAdvanced")) {
                issues.add("当前正文已经提前展开下一镜头目标，越过了本镜头停点。");
            }
            if (!issues.isEmpty() && StringUtils.hasText(nextSceneId)) {
                issues.add("本轮只允许完成当前镜头，不能提前抢写 " + nextSceneId + "。");
            }
            return issues.isEmpty() ? fallbackIssues : List.copyOf(issues);
        } catch (Exception ignored) {
            return fallbackIssues;
        }
    }

    private String buildExtractionSystemPrompt() {
        return """
                你是一名小说镜头连续性抽取器。
                你的任务是根据一个已经正式接纳的镜头正文，提炼后续镜头必须继承的结构化状态。
                只返回严格 JSON，不要解释，不要 Markdown，不要虚构正文里没有出现的信息。
                JSON 结构固定为：
                {
                  "carryForwardFacts": ["..."],
                  "timeAnchors": ["..."],
                  "expectedNames": ["..."],
                  "counterpartNames": ["..."],
                  "requiresExplicitTimeTransition": true
                }
                规则：
                1. carryForwardFacts 只保留后续镜头必须承接的稳定事实，最多 4 条。
                2. timeAnchors 只保留后续镜头必须继续尊重的时间状态或时间说明，最多 4 条。
                3. expectedNames 只保留正文中真实出现、后续仍需沿用的角色称呼；不要把地点、时间、组织、系统词写进去。
                4. counterpartNames 只保留本镜头结尾仍在直接互动、或下一镜头必须继续承接的会话对象称呼。
                5. requiresExplicitTimeTransition 仅在本镜头停在“未来事件尚未发生”之前，下一镜头必须明确写出时间推进时才为 true。
                6. 如果无法确认某项，请返回空数组或 false，不要猜。
                """;
    }

    private String buildExtractionUserPrompt(
            String acceptedContent,
            String summary,
            String handoffLine,
            List<String> readerReveal,
            String nextSceneGoal,
            String stopCondition,
            SceneContinuityState previousState) {
        StringBuilder builder = new StringBuilder();
        builder.append("请从当前已接纳镜头中提炼结构化 continuity state。\n");
        if (previousState != null && !previousState.isEmpty()) {
            builder.append("上一份 continuity state 仅供去歧义参考，不能覆盖当前正文明示事实。\n");
            if (StringUtils.hasText(previousState.summary())) {
                builder.append("上一 continuity 摘要：").append(previousState.summary()).append('\n');
            }
            if (!previousState.counterpartNames().isEmpty()) {
                builder.append("上一 continuity 称呼：").append(String.join("、", previousState.counterpartNames())).append('\n');
            }
        }
        if (StringUtils.hasText(summary)) {
            builder.append("镜头摘要：").append(summary.trim()).append('\n');
        }
        if (StringUtils.hasText(handoffLine)) {
            builder.append("镜头交接：").append(handoffLine.trim()).append('\n');
        }
        if (readerReveal != null && !readerReveal.isEmpty()) {
            builder.append("镜头揭晓：").append(String.join("；", readerReveal)).append('\n');
        }
        if (StringUtils.hasText(stopCondition)) {
            builder.append("当前镜头停点：").append(stopCondition.trim()).append('\n');
        }
        if (StringUtils.hasText(nextSceneGoal)) {
            builder.append("下一镜头目标：").append(nextSceneGoal.trim()).append('\n');
        }
        builder.append("已接纳正文：\n").append(acceptedContent == null ? "" : acceptedContent.trim());
        return builder.toString();
    }

    private String buildInspectionSystemPrompt() {
        return """
                你是一名小说镜头连续性审查器。
                请根据已给出的 continuity state、当前镜头目标、当前镜头停点和下一镜头目标，
                判断当前正文是否存在以下三类硬问题：
                1. timeTransitionConflict：上一镜头明确停在未来事件发生前，但当前正文把事件直接写成已发生且没有清晰时间推进。
                2. counterpartDrift：当前正文把上一镜头已经确认的会话对象或称呼换成了其他人，且当前镜头目标/停点也没有授权这种切换。
                3. advanceIntoNextScene：当前正文已经开始完成下一镜头目标，而不是停在当前镜头停点前。
                只返回严格 JSON，不要解释，不要 Markdown。
                JSON 结构固定为：
                {
                  "timeTransitionConflict": false,
                  "counterpartDrift": false,
                  "advanceIntoNextScene": false
                }
                如果不确定，请优先返回 false，不要过度拦截。
                """;
    }

    private String buildInspectionUserPrompt(
            SceneContinuityState continuityState,
            String content,
            String currentSceneGoal,
            String currentStopCondition,
            String nextSceneGoal) {
        StringBuilder builder = new StringBuilder();
        builder.append("continuity 摘要：").append(continuityState.summary()).append('\n');
        if (StringUtils.hasText(continuityState.handoffLine())) {
            builder.append("continuity 交接：").append(continuityState.handoffLine()).append('\n');
        }
        if (!continuityState.carryForwardFacts().isEmpty()) {
            builder.append("必须继承事实：").append(String.join("；", continuityState.carryForwardFacts())).append('\n');
        }
        if (!continuityState.timeAnchors().isEmpty()) {
            builder.append("时间锚点：").append(String.join("；", continuityState.timeAnchors())).append('\n');
        }
        if (!continuityState.counterpartNames().isEmpty()) {
            builder.append("已确认会话对象：").append(String.join("、", continuityState.counterpartNames())).append('\n');
        } else if (!continuityState.expectedNames().isEmpty()) {
            builder.append("已确认称呼：").append(String.join("、", continuityState.expectedNames())).append('\n');
        }
        builder.append("是否必须显式时间推进：").append(continuityState.requiresExplicitTimeTransition()).append('\n');
        builder.append("当前镜头目标：").append(currentSceneGoal == null ? "" : currentSceneGoal.trim()).append('\n');
        builder.append("当前镜头停点：").append(currentStopCondition == null ? "" : currentStopCondition.trim()).append('\n');
        builder.append("下一镜头目标：").append(nextSceneGoal == null ? "" : nextSceneGoal.trim()).append('\n');
        builder.append("待检查正文：\n").append(content.trim());
        return builder.toString();
    }

    private List<String> limitDistinct(List<String> values, int limit) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            normalized.add(value.trim());
            if (normalized.size() >= Math.max(1, limit)) {
                break;
            }
        }
        return List.copyOf(normalized);
    }
}
