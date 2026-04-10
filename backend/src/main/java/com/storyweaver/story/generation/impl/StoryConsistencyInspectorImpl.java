package com.storyweaver.story.generation.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.storyweaver.domain.entity.AIDirectorDecision;
import com.storyweaver.domain.entity.AIWritingRecord;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.repository.AIDirectorDecisionMapper;
import com.storyweaver.repository.AIWritingRecordMapper;
import com.storyweaver.service.ChapterService;
import com.storyweaver.service.CharacterService;
import com.storyweaver.service.ProjectService;
import com.storyweaver.story.generation.ChapterAnchorBundle;
import com.storyweaver.story.generation.ChapterAnchorResolver;
import com.storyweaver.story.generation.GenerationReadinessService;
import com.storyweaver.story.generation.GenerationReadinessVO;
import com.storyweaver.story.generation.StoryConsistencyInspector;
import com.storyweaver.story.generation.StoryConsistencyIssueVO;
import com.storyweaver.story.generation.StoryConsistencyReportVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StoryConsistencyInspectorImpl implements StoryConsistencyInspector {

    private static final Pattern NAME_ACTION_PATTERN = Pattern.compile(
            "([\\u4e00-\\u9fa5]{2,4})(?:说|道|问|答|想|看|笑|喊|低声|抬头|皱眉|点头|转身|看向|望向|走向|走到|盯着|开口|沉声)"
    );

    private static final Set<String> NAME_STOPWORDS = Set.of(
            "自己", "时候", "地方", "声音", "目光", "身体", "空气", "周围", "这里", "那里",
            "现在", "今天", "终于", "只是", "已经", "如果", "于是", "然后", "没有", "不能",
            "不是", "仿佛", "因为", "所以", "眼前", "心里", "门口", "身后", "面前", "此刻"
    );

    private final ProjectService projectService;
    private final ChapterService chapterService;
    private final CharacterService characterService;
    private final ChapterAnchorResolver chapterAnchorResolver;
    private final GenerationReadinessService generationReadinessService;
    private final AIDirectorDecisionMapper aiDirectorDecisionMapper;
    private final AIWritingRecordMapper aiWritingRecordMapper;
    private final ObjectMapper objectMapper;

    public StoryConsistencyInspectorImpl(
            ProjectService projectService,
            ChapterService chapterService,
            CharacterService characterService,
            ChapterAnchorResolver chapterAnchorResolver,
            GenerationReadinessService generationReadinessService,
            AIDirectorDecisionMapper aiDirectorDecisionMapper,
            AIWritingRecordMapper aiWritingRecordMapper,
            ObjectMapper objectMapper) {
        this.projectService = projectService;
        this.chapterService = chapterService;
        this.characterService = characterService;
        this.chapterAnchorResolver = chapterAnchorResolver;
        this.generationReadinessService = generationReadinessService;
        this.aiDirectorDecisionMapper = aiDirectorDecisionMapper;
        this.aiWritingRecordMapper = aiWritingRecordMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public StoryConsistencyReportVO inspectProject(Long userId, Long projectId) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            throw new IllegalArgumentException("项目不存在或无权访问");
        }

        Project project = projectService.getById(projectId);
        List<Chapter> chapters = new ArrayList<>(chapterService.getProjectChapters(projectId, userId));
        chapters.sort(Comparator
                .comparing((Chapter chapter) -> chapter.getOrderNum() == null ? Integer.MAX_VALUE : chapter.getOrderNum())
                .thenComparing(Chapter::getId));

        List<Character> projectCharacters = characterService.getProjectCharacters(projectId, userId);
        Set<String> canonNames = resolveCanonNames(projectCharacters);
        Set<String> leadNames = resolveLeadNames(projectCharacters);
        Map<Long, AIWritingRecord> latestRecords = resolveLatestRecords(projectId);

        StoryConsistencyReportVO report = new StoryConsistencyReportVO();
        report.setProjectId(projectId);
        report.setProjectName(project == null ? "" : safe(project.getName()));
        report.setChapterCount(chapters.size());

        int traceCompleteCount = 0;
        int directorInspectedCount = 0;

        for (Chapter chapter : chapters) {
            GenerationReadinessVO readiness = generationReadinessService.evaluate(userId, chapter.getId());
            ChapterAnchorBundle anchors = readiness.getResolvedAnchors() == null
                    ? chapterAnchorResolver.resolve(userId, chapter.getId())
                    : readiness.getResolvedAnchors();
            AIWritingRecord latestRecord = latestRecords.get(chapter.getId());
            if (latestRecord != null) {
                report.setGeneratedChapterCount(report.getGeneratedChapterCount() + 1);
            }

            if (readiness.isBlocked()) {
                report.setBlockedReadinessCount(report.getBlockedReadinessCount() + 1);
                registerIssue(
                        report,
                        "warning",
                        "anchor_gap",
                        chapter,
                        "本章生成前置锚点不足，当前仍存在阻塞项。",
                        readiness.getBlockingIssues()
                );
                report.addRecommendation("优先补齐章节 brief、POV、必出人物和剧情锚点，再重新生成。");
            }

            if (anchors.getMainPovCharacterId() == null) {
                report.setPovIssueCount(report.getPovIssueCount() + 1);
                registerIssue(
                        report,
                        "critical",
                        "pov_anchor_missing",
                        chapter,
                        "本章缺少稳定 POV 锚点。",
                        List.of("当前 mainPov 为空")
                );
                report.addRecommendation("为章节显式绑定主 POV 人物，避免正文视角漂移。");
            }

            if (anchors.getStoryBeatIds() == null || anchors.getStoryBeatIds().isEmpty()) {
                report.setStoryBeatIssueCount(report.getStoryBeatIssueCount() + 1);
                registerIssue(
                        report,
                        "critical",
                        "story_beats_missing",
                        chapter,
                        "本章缺少剧情推进锚点，难以稳定控制章节目标。",
                        List.of("当前 story beats 为空")
                );
                report.addRecommendation("为章节绑定至少一个剧情节点或推进目标。");
            }

            AIDirectorDecision latestDecision = aiDirectorDecisionMapper.findLatestByChapterIdAndUserId(chapter.getId(), userId);
            if (latestDecision != null) {
                directorInspectedCount++;
                if ("fallback".equalsIgnoreCase(safe(latestDecision.getStatus()))) {
                    report.setDirectorFallbackCount(report.getDirectorFallbackCount() + 1);
                    registerIssue(
                            report,
                            "warning",
                            "director_fallback",
                            chapter,
                            "该章最近一次总导决策发生 fallback。",
                            List.of(safe(latestDecision.getErrorMessage()))
                    );
                    report.addRecommendation("优先复验总导真实 Provider 返回，避免章节继续长期依赖启发式 fallback。");
                }
            }

            if (latestRecord != null) {
                JsonNode trace = readJson(latestRecord.getGenerationTraceJson());
                if (hasCompleteTrace(trace)) {
                    traceCompleteCount++;
                } else {
                    report.setTraceMissingCount(report.getTraceMissingCount() + 1);
                    registerIssue(
                            report,
                            "warning",
                            "trace_incomplete",
                            chapter,
                            "该章最近一次生成记录缺少完整 generation trace。",
                            buildTraceEvidence(trace)
                    );
                    report.addRecommendation("重新生成一次，让系统写入完整的 readiness、anchor snapshot 和 summary trace。");
                }
            }

            inspectNamingRisk(report, chapter, anchors, latestRecord, canonNames, leadNames);
        }

        report.setDirectorFallbackRate(rate(report.getDirectorFallbackCount(), directorInspectedCount));
        report.setTraceCoverageRate(rate(traceCompleteCount, report.getGeneratedChapterCount()));
        report.setRecommendations(deduplicate(report.getRecommendations()));
        report.setIssues(sortIssues(report.getIssues()));
        finalizeScore(report);
        return report;
    }

    private void inspectNamingRisk(
            StoryConsistencyReportVO report,
            Chapter chapter,
            ChapterAnchorBundle anchors,
            AIWritingRecord latestRecord,
            Set<String> canonNames,
            Set<String> leadNames) {
        String content = resolveInspectionContent(chapter, latestRecord);
        if (!StringUtils.hasText(content)) {
            return;
        }

        Set<String> anchorNames = new LinkedHashSet<>();
        if (StringUtils.hasText(anchors.getMainPovCharacterName())) {
            anchorNames.add(anchors.getMainPovCharacterName().trim());
        }
        if (anchors.getRequiredCharacterNames() != null) {
            anchors.getRequiredCharacterNames().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(anchorNames::add);
        }

        boolean anchorMentioned = containsAny(content, anchorNames);
        boolean leadMentioned = leadNames.isEmpty() || containsAny(content, leadNames);

        List<String> canonMentions = canonNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(name -> content.contains(name))
                .toList();
        List<String> unknownCandidates = extractUnknownNameCandidates(content, canonNames);

        if (!anchorNames.isEmpty() && !anchorMentioned && (!canonMentions.isEmpty() || !unknownCandidates.isEmpty())) {
            report.setNamingRiskCount(report.getNamingRiskCount() + 1);
            registerIssue(
                    report,
                    "warning",
                    "naming_drift_risk",
                    chapter,
                    "正文中的人物称呼与本章锚点不一致，存在命名漂移风险。",
                    buildNamingEvidence(anchorNames, canonMentions, unknownCandidates)
            );
            report.addRecommendation("先确认本章 anchor pack，再针对漂移章节重新生成或重写。");
            return;
        }

        if (!leadMentioned && !unknownCandidates.isEmpty()) {
            report.setNamingRiskCount(report.getNamingRiskCount() + 1);
            registerIssue(
                    report,
                    "info",
                    "naming_drift_risk",
                    chapter,
                    "正文未稳定出现项目主角名，但出现了未知高频人名候选。",
                    buildNamingEvidence(leadNames, canonMentions, unknownCandidates)
            );
            report.addRecommendation("补一轮主角 canon 和本章人物锚点检查，再观察是否仍有主角命名漂移。");
        }
    }

    private List<String> buildNamingEvidence(
            Set<String> expectedNames,
            List<String> canonMentions,
            List<String> unknownCandidates) {
        List<String> evidence = new ArrayList<>();
        if (expectedNames != null && !expectedNames.isEmpty()) {
            evidence.add("期望人物：" + String.join("、", expectedNames));
        }
        if (canonMentions != null && !canonMentions.isEmpty()) {
            evidence.add("正文已出现项目人物：" + String.join("、", deduplicate(canonMentions)));
        }
        if (unknownCandidates != null && !unknownCandidates.isEmpty()) {
            evidence.add("未知高频人名候选：" + String.join("、", unknownCandidates));
        }
        return evidence;
    }

    private List<String> buildTraceEvidence(JsonNode trace) {
        List<String> evidence = new ArrayList<>();
        if (trace == null || trace.isNull() || trace.isMissingNode()) {
            evidence.add("generationTrace 为空");
            return evidence;
        }
        if (!isPopulatedNode(trace.path("readiness"))) {
            evidence.add("缺少 readiness");
        }
        if (!isPopulatedNode(trace.path("anchors"))) {
            evidence.add("缺少 anchors");
        } else if (!StringUtils.hasText(trace.path("anchors").path("anchorSummary").asText(""))) {
            evidence.add("anchors 中缺少 anchorSummary");
        }
        if (!isPopulatedNode(trace.path("director"))) {
            evidence.add("缺少 director");
        }
        if (!isPopulatedNode(trace.path("summaryTrace"))) {
            evidence.add("缺少 summaryTrace");
        }
        return evidence;
    }

    private boolean hasCompleteTrace(JsonNode trace) {
        return isPopulatedNode(trace.path("readiness"))
                && isPopulatedNode(trace.path("anchors"))
                && StringUtils.hasText(trace.path("anchors").path("anchorSummary").asText(""))
                && isPopulatedNode(trace.path("director"))
                && isPopulatedNode(trace.path("summaryTrace"));
    }

    private boolean isPopulatedNode(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return false;
        }
        if (node.isArray() || node.isObject()) {
            return node.size() > 0;
        }
        return StringUtils.hasText(node.asText(""));
    }

    private Map<Long, AIWritingRecord> resolveLatestRecords(Long projectId) {
        Map<Long, AIWritingRecord> result = new LinkedHashMap<>();
        for (AIWritingRecord record : aiWritingRecordMapper.findByProjectId(projectId)) {
            if (record == null || record.getChapterId() == null || result.containsKey(record.getChapterId())) {
                continue;
            }
            result.put(record.getChapterId(), record);
        }
        return result;
    }

    private Set<String> resolveCanonNames(List<Character> characters) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (characters == null) {
            return names;
        }
        for (Character character : characters) {
            if (character != null && StringUtils.hasText(character.getName())) {
                names.add(character.getName().trim());
            }
        }
        return names;
    }

    private Set<String> resolveLeadNames(List<Character> characters) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (characters == null) {
            return names;
        }
        for (Character character : characters) {
            if (character == null || !StringUtils.hasText(character.getName())) {
                continue;
            }
            if (isLeadRole(character.getProjectRole()) || isLeadRole(character.getRoleType())) {
                names.add(character.getName().trim());
            }
        }
        return names;
    }

    private boolean isLeadRole(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("主角")
                || normalized.contains("主人公")
                || normalized.contains("protagonist")
                || normalized.contains("lead")
                || normalized.contains("main");
    }

    private String resolveInspectionContent(Chapter chapter, AIWritingRecord latestRecord) {
        if (latestRecord != null && StringUtils.hasText(latestRecord.getGeneratedContent())) {
            return latestRecord.getGeneratedContent().trim();
        }
        return chapter == null ? "" : safe(chapter.getContent());
    }

    private List<String> extractUnknownNameCandidates(String content, Set<String> canonNames) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }

        Map<String, Integer> counter = new LinkedHashMap<>();
        Matcher matcher = NAME_ACTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (!StringUtils.hasText(candidate)) {
                continue;
            }

            String normalized = candidate.trim();
            if (normalized.length() < 2 || normalized.length() > 4) {
                continue;
            }
            if (NAME_STOPWORDS.contains(normalized)) {
                continue;
            }
            if (canonNames.contains(normalized)) {
                continue;
            }
            counter.merge(normalized, 1, Integer::sum);
        }

        return counter.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
    }

    private boolean containsAny(String content, Set<String> names) {
        if (!StringUtils.hasText(content) || names == null || names.isEmpty()) {
            return false;
        }
        for (String name : names) {
            if (StringUtils.hasText(name) && content.contains(name.trim())) {
                return true;
            }
        }
        return false;
    }

    private JsonNode readJson(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return NullNode.getInstance();
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (Exception exception) {
            return NullNode.getInstance();
        }
    }

    private void registerIssue(
            StoryConsistencyReportVO report,
            String severity,
            String type,
            Chapter chapter,
            String message,
            List<String> evidence) {
        StoryConsistencyIssueVO issue = new StoryConsistencyIssueVO();
        issue.setSeverity(severity);
        issue.setType(type);
        issue.setChapterId(chapter == null ? null : chapter.getId());
        issue.setChapterTitle(chapter == null ? "" : safe(chapter.getTitle()));
        issue.setMessage(message);
        if (evidence != null) {
            evidence.stream().filter(StringUtils::hasText).map(String::trim).forEach(issue::addEvidence);
        }
        report.addIssue(issue);
    }

    private void finalizeScore(StoryConsistencyReportVO report) {
        int score = 100;
        boolean hasCritical = false;

        for (StoryConsistencyIssueVO issue : report.getIssues()) {
            String severity = safe(issue.getSeverity()).toLowerCase(Locale.ROOT);
            switch (severity) {
                case "critical" -> {
                    score -= 14;
                    hasCritical = true;
                }
                case "warning" -> score -= 8;
                default -> score -= 3;
            }
        }

        score = Math.max(0, score);
        report.setScore(score);
        if (hasCritical || score < 60) {
            report.setStatus("critical");
            return;
        }
        if (!report.getIssues().isEmpty() || score < 85) {
            report.setStatus("warning");
            return;
        }
        report.setStatus("healthy");
    }

    private double rate(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return Math.round((numerator * 10000D) / denominator) / 100D;
    }

    private List<String> deduplicate(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    private List<StoryConsistencyIssueVO> sortIssues(List<StoryConsistencyIssueVO> issues) {
        if (issues == null || issues.isEmpty()) {
            return List.of();
        }
        List<StoryConsistencyIssueVO> sorted = new ArrayList<>(issues);
        sorted.sort(Comparator
                .comparingInt((StoryConsistencyIssueVO issue) -> severityOrder(issue.getSeverity()))
                .thenComparing(issue -> issue.getChapterId() == null ? Long.MAX_VALUE : issue.getChapterId()));
        return sorted;
    }

    private int severityOrder(String severity) {
        String normalized = safe(severity).toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "critical" -> 0;
            case "warning" -> 1;
            default -> 2;
        };
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
