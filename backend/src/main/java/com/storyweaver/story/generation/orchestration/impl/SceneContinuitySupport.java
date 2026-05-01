package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.storyunit.session.SceneContinuityState;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneHandoffSnapshot;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SceneContinuitySupport {

    private static final int FACT_LIMIT = 4;
    private static final int NAME_LIMIT = 6;
    private static final Pattern NAME_BEFORE_ACTION_PATTERN = Pattern.compile(
            "([\\p{IsHan}]{2,4}?)(?=决定|答应|回复|登录|进入|走向|站在|停在|准备|说道|说|问|出场|出现|发来|等他|正朝|朝他走来)"
    );
    private static final Pattern NAME_AFTER_INTERACTION_PATTERN = Pattern.compile(
            "(?:看见|看到|见到|联系|邀请|回复|会见|碰见|遇见|听见|等到|等待|和|与|对|给|向)([\\p{IsHan}]{2,4})(?=正|朝|站|走|挥|开|说|问|发|回|会|交|对|并|，|。|、)"
    );
    private static final Pattern NAME_WITH_CONTEXT_PATTERN = Pattern.compile(
            "([\\p{IsHan}]{2,4}?)(?=发来的消息|来电|站在|走来|挥手|开口|对上视线)"
    );
    private static final Pattern NAME_WITH_POSSESSIVE_PATTERN = Pattern.compile(
            "([\\p{IsHan}]{2,4}?)(?=的)"
    );
    private static final Pattern STANDALONE_NAME_PATTERN = Pattern.compile(
            "(?:(?<=^)|(?<=[，。！？、“”『』（）()\\s]))([\\p{IsHan}]{2,4})(?=[，。！？、“”『』（）()\\s])"
    );
    private static final Pattern ACTION_PHRASE_PATTERN = Pattern.compile(
            "(?:进入|抵达|前往|来到|登录|会见|碰面|见到|交换|交谈|对话|说明|汇报|讨论|确认)[\\p{IsHan}0-9]{0,8}|与[\\p{IsHan}]{1,4}(?:会面|交谈|对话)"
    );
    private static final Pattern TIME_ANCHOR_PATTERN = Pattern.compile(
            "(?:第?[一二三四五六七八九十百0-9]+天|次日|翌日|明天|今晚|今夜|凌晨|清晨|早上|上午|中午|下午|傍晚|晚上|夜里|次晨|午后|[0-9一二三四五六七八九十]+点(?:半|[一二三四五六七八九十0-9]分)?)"
    );
    private static final Set<String> GENERIC_NAME_STOPWORDS = Set.of(
            "项目", "角色", "镜头", "章节", "故事", "场景", "正文", "内容", "系统", "页面", "世界", "地图", "数据", "玩家"
    );
    private static final List<String> GENERIC_NAME_FRAGMENT_STOPWORDS = List.of(
            "大厅", "入口", "边缘", "光门", "人群", "中午", "上午", "晚上", "凌晨", "今晚", "今夜", "明天", "第二天", "次日", "翌日"
    );

    private SceneContinuitySupport() {
    }

    public static SceneContinuityState resolveContinuityState(
            SceneHandoffSnapshot previousSceneHandoff,
            SceneExecutionState resolvedSceneState,
            List<SceneExecutionState> existingSceneStates,
            String nextSceneId,
            String nextSceneGoal,
            String stopCondition) {
        SceneContinuityState continuityState = readContinuityState(previousSceneHandoff, resolvedSceneState, existingSceneStates);
        if (continuityState.isEmpty()) {
            continuityState = buildFallbackState(previousSceneHandoff, resolvedSceneState, existingSceneStates);
        }
        return withForwardTargets(continuityState, nextSceneId, nextSceneGoal, stopCondition);
    }

    public static SceneContinuityState buildAcceptedContinuityState(
            String sceneId,
            String acceptedContent,
            String summary,
            String handoffLine,
            List<String> readerReveal,
            String nextSceneId,
            String nextSceneGoal,
            String stopCondition,
            SceneContinuityState previousState) {
        List<String> extractedFacts = extractCriticalFacts(acceptedContent, summary, handoffLine, readerReveal);
        List<String> extractedTimeAnchors = extractTimeAnchors(acceptedContent, summary, handoffLine, extractedFacts);
        List<String> currentExpectedNames = mergeLimited(NAME_LIMIT,
                extractNamesFromText(acceptedContent),
                extractNamesFromText(summary),
                extractNamesFromText(handoffLine),
                extractNamesFromSentences(extractedFacts));
        List<String> currentCounterpartNames = mergeLimited(NAME_LIMIT,
                extractNamesFromText(handoffLine),
                extractNamesFromText(firstSentence(acceptedContent)),
                extractNamesFromText(lastSentence(acceptedContent)),
                extractNamesFromText(summary));
        return mergeAcceptedContinuityState(
                sceneId,
                summary,
                handoffLine,
                extractedFacts,
                extractedTimeAnchors,
                currentExpectedNames,
                currentCounterpartNames,
                false,
                nextSceneId,
                nextSceneGoal,
                stopCondition,
                previousState
        );
    }

    public static SceneContinuityState mergeAcceptedContinuityState(
            String sceneId,
            String summary,
            String handoffLine,
            List<String> carryForwardFacts,
            List<String> timeAnchors,
            List<String> expectedNames,
            List<String> counterpartNames,
            boolean requiresExplicitTimeTransition,
            String nextSceneId,
            String nextSceneGoal,
            String stopCondition,
            SceneContinuityState previousState) {
        SceneContinuityState baseState = previousState == null ? SceneContinuityState.empty() : previousState;
        return new SceneContinuityState(
                normalizeText(sceneId),
                normalizeText(summary),
                normalizeText(handoffLine),
                preferRecentLimited(FACT_LIMIT, carryForwardFacts, baseState.carryForwardFacts()),
                preferCurrentListOrFallback(timeAnchors, baseState.timeAnchors()),
                preferRecentLimited(NAME_LIMIT, expectedNames, baseState.expectedNames()),
                preferCurrentListOrFallback(counterpartNames, baseState.counterpartNames()),
                requiresExplicitTimeTransition || baseState.requiresExplicitTimeTransition() && (timeAnchors == null || timeAnchors.isEmpty()),
                normalizeText(nextSceneId),
                normalizeText(nextSceneGoal),
                normalizeText(stopCondition)
        );
    }

    public static List<String> buildConstraintLines(SceneContinuityState continuityState) {
        if (continuityState == null || continuityState.isEmpty()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        if (StringUtils.hasText(continuityState.summary())) {
            lines.add("上一镜头真实摘要：" + continuityState.summary());
        }
        if (StringUtils.hasText(continuityState.handoffLine())) {
            lines.add("上一镜头真实交接：" + continuityState.handoffLine());
        }
        if (!continuityState.carryForwardFacts().isEmpty()) {
            lines.add("必须继承事实：" + String.join("；", continuityState.carryForwardFacts()));
        }
        if (!continuityState.timeAnchors().isEmpty()) {
            lines.add("时间锚点：" + String.join("；", continuityState.timeAnchors()));
        }
        if (!continuityState.counterpartNames().isEmpty()) {
            lines.add("当前涉及人物称呼：" + String.join("、", continuityState.counterpartNames()));
        } else if (!continuityState.expectedNames().isEmpty()) {
            lines.add("当前已确认称呼：" + String.join("、", continuityState.expectedNames()));
        }
        if (continuityState.requiresExplicitTimeTransition()) {
            lines.add("下一镜头必须显式写出时间推进，不能把未发生事件直接写成已发生。");
        }
        return List.copyOf(lines);
    }

    public static List<String> detectContinuityIssues(
            SceneContinuityState continuityState,
            String content,
            String currentSceneGoal,
            String currentStopCondition,
            String nextSceneId,
            String nextSceneGoal) {
        if (continuityState == null || continuityState.isEmpty() || !StringUtils.hasText(content)) {
            return List.of();
        }
        List<String> issues = new ArrayList<>();
        if (hasCounterpartDrift(continuityState, content, currentSceneGoal, currentStopCondition)) {
            issues.add("当前正文把上一镜头已经确认的会话对象或称呼改写成了其他人。");
        }
        if (advancesIntoNextScene(content, currentSceneGoal, currentStopCondition, nextSceneGoal)) {
            issues.add("当前正文已经提前展开下一镜头目标，越过了本镜头停点。");
        }
        if (!issues.isEmpty() && StringUtils.hasText(nextSceneId)) {
            issues.add("本轮只允许完成当前镜头，不能提前抢写 " + nextSceneId + "。");
        }
        return List.copyOf(issues);
    }

    private static SceneContinuityState readContinuityState(
            SceneHandoffSnapshot previousSceneHandoff,
            SceneExecutionState resolvedSceneState,
            List<SceneExecutionState> existingSceneStates) {
        SceneContinuityState fromHandoff = previousSceneHandoff == null
                ? SceneContinuityState.empty()
                : readContinuityState(previousSceneHandoff.stateDelta());
        if (!fromHandoff.isEmpty()) {
            return fromHandoff;
        }
        SceneContinuityState fromResolvedState = resolvedSceneState == null
                ? SceneContinuityState.empty()
                : readContinuityState(resolvedSceneState.stateDelta());
        if (!fromResolvedState.isEmpty()) {
            return fromResolvedState;
        }
        if (existingSceneStates == null || existingSceneStates.isEmpty()) {
            return SceneContinuityState.empty();
        }
        for (int index = existingSceneStates.size() - 1; index >= 0; index--) {
            SceneContinuityState continuityState = readContinuityState(existingSceneStates.get(index).stateDelta());
            if (!continuityState.isEmpty()) {
                return continuityState;
            }
        }
        return SceneContinuityState.empty();
    }

    private static SceneContinuityState readContinuityState(Map<String, Object> stateDelta) {
        if (stateDelta == null || stateDelta.isEmpty()) {
            return SceneContinuityState.empty();
        }
        return SceneContinuityState.fromStateDeltaValue(stateDelta.get("continuity"));
    }

    private static SceneContinuityState buildFallbackState(
            SceneHandoffSnapshot previousSceneHandoff,
            SceneExecutionState resolvedSceneState,
            List<SceneExecutionState> existingSceneStates) {
        String sourceSceneId = previousSceneHandoff == null ? "" : previousSceneHandoff.fromSceneId();
        String summary = firstNonBlank(
                previousSceneHandoff == null ? "" : previousSceneHandoff.outcomeSummary(),
                resolvedSceneState == null ? "" : resolvedSceneState.outcomeSummary(),
                latest(existingSceneStates, SceneExecutionState::outcomeSummary)
        );
        String handoffLine = firstNonBlank(
                previousSceneHandoff == null ? "" : previousSceneHandoff.handoffLine(),
                resolvedSceneState == null ? "" : resolvedSceneState.handoffLine(),
                latest(existingSceneStates, SceneExecutionState::handoffLine)
        );
        List<String> facts = mergeLimited(FACT_LIMIT, extractCriticalFacts("", summary, handoffLine, List.of()));
        return new SceneContinuityState(
                normalizeText(sourceSceneId),
                normalizeText(summary),
                normalizeText(handoffLine),
                facts,
                extractTimeAnchors("", summary, handoffLine, facts),
                mergeLimited(NAME_LIMIT, extractNamesFromText(summary), extractNamesFromText(handoffLine)),
                mergeLimited(NAME_LIMIT, extractNamesFromText(handoffLine), extractNamesFromText(summary)),
                false,
                "",
                "",
                ""
        );
    }

    private static SceneContinuityState withForwardTargets(
            SceneContinuityState continuityState,
            String nextSceneId,
            String nextSceneGoal,
            String stopCondition) {
        SceneContinuityState baseState = continuityState == null ? SceneContinuityState.empty() : continuityState;
        return new SceneContinuityState(
                baseState.sourceSceneId(),
                baseState.summary(),
                baseState.handoffLine(),
                baseState.carryForwardFacts(),
                baseState.timeAnchors(),
                baseState.expectedNames(),
                baseState.counterpartNames(),
                baseState.requiresExplicitTimeTransition(),
                normalizeText(nextSceneId),
                normalizeText(nextSceneGoal),
                normalizeText(stopCondition)
        );
    }

    private static List<String> extractCriticalFacts(
            String content,
            String summary,
            String handoffLine,
            List<String> readerReveal) {
        List<String> facts = new ArrayList<>();
        addIfText(facts, firstSentence(content));
        addIfText(facts, normalizeText(summary));
        addIfText(facts, lastSentence(content));
        addIfText(facts, normalizeText(handoffLine));
        if (readerReveal != null) {
            for (String reveal : readerReveal) {
                addIfText(facts, "已向读者揭晓：" + normalizeText(reveal));
            }
        }
        return mergeLimited(FACT_LIMIT, facts);
    }

    private static List<String> extractTimeAnchors(
            String acceptedContent,
            String summary,
            String handoffLine,
            List<String> fallbackFacts) {
        List<String> candidates = new ArrayList<>();
        addTimeAnchorsFromText(candidates, acceptedContent);
        addTimeAnchorsFromText(candidates, summary);
        addTimeAnchorsFromText(candidates, handoffLine);
        if (fallbackFacts != null) {
            for (String fact : fallbackFacts) {
                addTimeAnchorsFromText(candidates, fact);
            }
        }
        return mergeLimited(FACT_LIMIT, candidates);
    }

    private static void addTimeAnchorsFromText(List<String> target, String text) {
        if (target == null || !StringUtils.hasText(text)) {
            return;
        }
        List<String> sentences = splitSentences(text);
        for (String sentence : sentences) {
            if (TIME_ANCHOR_PATTERN.matcher(sentence).find()) {
                addIfText(target, sentence);
            }
        }
    }

    private static List<String> splitSentences(String content) {
        String normalized = normalizeText(content).replaceAll("\\s+", " ");
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }
        List<String> sentences = new ArrayList<>();
        for (String segment : normalized.split("(?<=[。！？!?])")) {
            if (StringUtils.hasText(segment)) {
                sentences.add(segment.trim());
            }
        }
        if (sentences.isEmpty()) {
            sentences.add(normalized);
        }
        return List.copyOf(sentences);
    }

    private static String firstSentence(String content) {
        List<String> sentences = splitSentences(content);
        return sentences.isEmpty() ? "" : sentences.getFirst();
    }

    private static String lastSentence(String content) {
        List<String> sentences = splitSentences(content);
        return sentences.isEmpty() ? "" : sentences.getLast();
    }

    private static List<String> extractNamesFromText(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        return extractNames(List.of(text));
    }

    private static List<String> extractNamesFromSentences(List<String> sentences) {
        return extractNames(sentences);
    }

    private static List<String> extractNames(List<String> parts) {
        if (parts == null || parts.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            addNamesByPattern(names, part, NAME_BEFORE_ACTION_PATTERN);
            addNamesByPattern(names, part, NAME_AFTER_INTERACTION_PATTERN);
            addNamesByPattern(names, part, NAME_WITH_CONTEXT_PATTERN);
            addNamesByPattern(names, part, NAME_WITH_POSSESSIVE_PATTERN);
            addNamesByPattern(names, part, STANDALONE_NAME_PATTERN);
        }
        return List.copyOf(names);
    }

    private static void addNamesByPattern(Set<String> names, String text, Pattern pattern) {
        if (names == null || !StringUtils.hasText(text) || pattern == null || names.size() >= NAME_LIMIT) {
            return;
        }
        Matcher matcher = pattern.matcher(text);
        while (matcher.find() && names.size() < NAME_LIMIT) {
            addCandidateName(names, matcher.group(1));
        }
    }

    private static void addCandidateName(Set<String> names, String rawCandidate) {
        if (names == null) {
            return;
        }
        String candidate = normalizeText(rawCandidate);
        if (!StringUtils.hasText(candidate)) {
            return;
        }
        if (candidate.length() == 1 || candidate.length() > 4) {
            return;
        }
        if (GENERIC_NAME_STOPWORDS.contains(candidate)) {
            return;
        }
        if (containsAny(candidate, GENERIC_NAME_FRAGMENT_STOPWORDS)) {
            return;
        }
        if (candidate.length() == 2 && candidate.endsWith("们")) {
            return;
        }
        if (containsAny(candidate, List.of("当前", "正文", "镜头", "章节", "故事", "场景"))) {
            return;
        }
        names.add(candidate);
    }

    private static boolean hasCounterpartDrift(
            SceneContinuityState continuityState,
            String content,
            String currentSceneGoal,
            String currentStopCondition) {
        if (continuityState.counterpartNames().isEmpty()) {
            return false;
        }
        List<String> mentionedNames = extractNamesFromText(content);
        if (mentionedNames.isEmpty()) {
            return false;
        }
        List<String> currentSceneAllowedNames = mergeLimited(NAME_LIMIT,
                extractNamesFromText(currentSceneGoal),
                extractNamesFromText(currentStopCondition));
        if (mentionedNames.stream().anyMatch(currentSceneAllowedNames::contains)) {
            return false;
        }
        return mentionedNames.stream().noneMatch(continuityState.counterpartNames()::contains);
    }

    private static boolean advancesIntoNextScene(
            String content,
            String currentSceneGoal,
            String currentStopCondition,
            String nextSceneGoal) {
        if (!StringUtils.hasText(content) || !StringUtils.hasText(nextSceneGoal)) {
            return false;
        }
        List<String> nextActionPhrases = extractActionPhrases(nextSceneGoal);
        if (nextActionPhrases.isEmpty()) {
            return false;
        }
        String normalizedContent = normalizeText(content);
        String normalizedCurrentGoal = normalizeText(currentSceneGoal);
        String normalizedStopCondition = normalizeText(currentStopCondition);
        for (String phrase : nextActionPhrases) {
            if (!StringUtils.hasText(phrase) || !normalizedContent.contains(phrase)) {
                continue;
            }
            if (normalizedCurrentGoal.contains(phrase) || normalizedStopCondition.contains(phrase)) {
                continue;
            }
            return true;
        }
        for (String phrase : nextActionPhrases) {
            for (String targetTerm : extractTargetTerms(phrase)) {
                if (!normalizedContent.contains(targetTerm)) {
                    continue;
                }
                if (normalizedCurrentGoal.contains(targetTerm) || normalizedStopCondition.contains(targetTerm)) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    private static List<String> extractActionPhrases(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        LinkedHashSet<String> phrases = new LinkedHashSet<>();
        Matcher matcher = ACTION_PHRASE_PATTERN.matcher(text);
        while (matcher.find()) {
            String phrase = normalizeText(matcher.group());
            if (phrase.length() >= 4) {
                phrases.add(phrase);
            }
        }
        return List.copyOf(phrases);
    }

    private static List<String> extractTargetTerms(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        LinkedHashSet<String> targets = new LinkedHashSet<>();
        String normalizedText = normalizeText(text)
                .replaceFirst("^(?:进入|抵达|前往|来到|登录|会见|碰面|见到|交换|交谈|对话|说明|汇报|讨论|确认)", "");
        Matcher matcher = Pattern.compile("[\\p{IsHan}]{2,8}").matcher(normalizedText);
        while (matcher.find()) {
            String candidate = normalizeText(matcher.group());
            if (candidate.length() < 3) {
                continue;
            }
            if (containsAny(candidate, List.of("主角"))) {
                continue;
            }
            targets.add(candidate);
        }
        return List.copyOf(targets);
    }

    @SafeVarargs
    private static List<String> preferRecentLimited(int limit, List<String> current, List<String>... fallbacks) {
        List<String> preferred = mergeLimited(limit, current);
        if (!preferred.isEmpty()) {
            return preferred;
        }
        return mergeLimited(limit, fallbacks);
    }

    private static List<String> preferCurrentListOrFallback(List<String> current, List<String> fallback) {
        if (current != null && !current.isEmpty()) {
            return List.copyOf(current);
        }
        if (fallback == null || fallback.isEmpty()) {
            return List.of();
        }
        return List.copyOf(fallback);
    }

    @SafeVarargs
    private static List<String> mergeLimited(int limit, List<String>... groups) {
        if (groups == null || groups.length == 0) {
            return List.of();
        }
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (List<String> group : groups) {
            if (group == null || group.isEmpty()) {
                continue;
            }
            for (String item : group) {
                String normalized = normalizeText(item);
                if (!StringUtils.hasText(normalized)) {
                    continue;
                }
                merged.add(normalized);
                if (merged.size() >= Math.max(1, limit)) {
                    return List.copyOf(merged);
                }
            }
        }
        return List.copyOf(merged);
    }

    private static void addIfText(List<String> target, String value) {
        if (target == null || !StringUtils.hasText(value)) {
            return;
        }
        target.add(value.trim());
    }

    private static boolean containsAny(String value, List<String> keywords) {
        if (!StringUtils.hasText(value) || keywords == null || keywords.isEmpty()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (!StringUtils.hasText(keyword)) {
                continue;
            }
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String latest(List<SceneExecutionState> states, Function<SceneExecutionState, String> extractor) {
        if (states == null || states.isEmpty() || extractor == null) {
            return "";
        }
        for (int index = states.size() - 1; index >= 0; index--) {
            String value = normalizeText(extractor.apply(states.get(index)));
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            String normalized = normalizeText(value);
            if (StringUtils.hasText(normalized)) {
                return normalized;
            }
        }
        return "";
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }
}
