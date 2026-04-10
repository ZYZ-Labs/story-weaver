package com.storyweaver.story.generation.impl;

import com.storyweaver.domain.entity.Causality;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Plot;
import com.storyweaver.repository.PlotMapper;
import com.storyweaver.service.CausalityService;
import com.storyweaver.service.ChapterService;
import com.storyweaver.service.ProjectService;
import com.storyweaver.story.generation.StoryProgressPredictor;
import com.storyweaver.story.generation.StoryProgressSuggestionVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class StoryProgressPredictorImpl implements StoryProgressPredictor {

    private static final Map<String, List<String>> PROGRESS_KEYWORDS = createProgressKeywords();

    private final ProjectService projectService;
    private final ChapterService chapterService;
    private final PlotMapper plotMapper;
    private final CausalityService causalityService;

    public StoryProgressPredictorImpl(
            ProjectService projectService,
            ChapterService chapterService,
            PlotMapper plotMapper,
            CausalityService causalityService) {
        this.projectService = projectService;
        this.chapterService = chapterService;
        this.plotMapper = plotMapper;
        this.causalityService = causalityService;
    }

    @Override
    public StoryProgressSuggestionVO suggestOutlineProgress(Long userId, Long projectId, Long targetOutlineId, String contextText) {
        return suggest("outline", userId, projectId, targetOutlineId, null, contextText);
    }

    @Override
    public StoryProgressSuggestionVO suggestPlotProgress(Long userId, Long projectId, Long targetOutlineId, String contextText) {
        return suggest("plot", userId, projectId, targetOutlineId, null, contextText);
    }

    @Override
    public StoryProgressSuggestionVO suggestChapterProgress(Long userId, Long projectId, Long targetChapterId, String contextText) {
        return suggest("chapter", userId, projectId, null, targetChapterId, contextText);
    }

    private StoryProgressSuggestionVO suggest(
            String scope,
            Long userId,
            Long projectId,
            Long targetOutlineId,
            Long targetChapterId,
            String contextText) {
        requireProjectAccess(projectId, userId);

        List<Chapter> chapters = chapterService.getProjectChapters(projectId, userId);
        List<Plot> plots = plotMapper.findByProjectId(projectId);
        List<Causality> causalities = causalityService.getProjectCausalities(projectId, userId);

        StoryProgressSuggestionVO suggestion = new StoryProgressSuggestionVO();
        suggestion.setScope(scope);
        suggestion.setBasedOn(buildEvidence(chapters, plots, causalities, targetOutlineId, targetChapterId));

        String normalizedContext = normalizeText(contextText);
        String keywordDrivenType = detectKeywordDrivenType(normalizedContext);
        double progressRatio = estimateProgressRatio(chapters, targetChapterId);

        if (keywordDrivenType != null) {
            suggestion.setPredictedProgressType(keywordDrivenType);
            suggestion.setConfidence(78);
            suggestion.addReason("输入文本已明显体现 `" + keywordDrivenType + "` 的语义信号。");
            suggestion.addReason("仍建议结合现有章节和因果链手动确认。");
        } else {
            String inferredType = inferProgressType(progressRatio, chapters.size(), plots.size(), causalities.size());
            suggestion.setPredictedProgressType(inferredType);
            suggestion.setConfidence(resolveConfidence(progressRatio, chapters.size(), plots.size(), causalities.size()));
            suggestion.addReason("当前项目已有 " + chapters.size() + " 章、" + plots.size() + " 个剧情节点、" + causalities.size() + " 条因果。");
            suggestion.addReason("基于当前进度比约 " + Math.round(progressRatio * 100) + "% 的启发式判断，当前更像 `" + inferredType + "`。");
        }

        applyRecommendedFields(suggestion);
        return suggestion;
    }

    private StoryProgressSuggestionVO.BasedOn buildEvidence(
            List<Chapter> chapters,
            List<Plot> plots,
            List<Causality> causalities,
            Long targetOutlineId,
            Long targetChapterId) {
        StoryProgressSuggestionVO.BasedOn evidence = new StoryProgressSuggestionVO.BasedOn();
        evidence.setChapterCount(chapters.size());
        evidence.setPlotCount(plots.size());
        evidence.setCausalityCount(causalities.size());
        evidence.setExistingChapterIds(sampleIds(chapters.stream().map(Chapter::getId).filter(Objects::nonNull).toList()));
        evidence.setExistingPlotIds(sampleIds(plots.stream().map(Plot::getId).filter(Objects::nonNull).toList()));
        evidence.setExistingCausalityIds(sampleIds(causalities.stream().map(Causality::getId).filter(Objects::nonNull).toList()));
        evidence.setCurrentOutlineId(targetOutlineId);
        evidence.setCurrentChapterId(targetChapterId);
        return evidence;
    }

    private void applyRecommendedFields(StoryProgressSuggestionVO suggestion) {
        String progressType = suggestion.getPredictedProgressType();
        if (!StringUtils.hasText(progressType)) {
            return;
        }

        switch (progressType) {
            case "foreshadow" -> {
                suggestion.putRecommendedField("storyFunction", "铺垫");
                suggestion.putRecommendedField("needPayoffTarget", true);
            }
            case "side_branch" -> {
                suggestion.putRecommendedField("storyFunction", "扩展支线");
                suggestion.putRecommendedField("needRelationToMainline", true);
            }
            case "turning" -> {
                suggestion.putRecommendedField("storyFunction", "制造转折");
                suggestion.putRecommendedField("needPreviousState", true);
            }
            case "payoff" -> {
                suggestion.putRecommendedField("storyFunction", "兑现前文铺垫");
                suggestion.putRecommendedField("needUpstreamCause", true);
            }
            case "convergence" -> {
                suggestion.putRecommendedField("storyFunction", "收束并汇合线程");
                suggestion.putRecommendedField("needMergeThreads", true);
            }
            default -> {
                suggestion.putRecommendedField("storyFunction", "推进主线");
                suggestion.putRecommendedField("needConflictStep", true);
            }
        }
    }

    private String inferProgressType(double progressRatio, int chapterCount, int plotCount, int causalityCount) {
        if (chapterCount <= 1 && plotCount <= 2) {
            return "foreshadow";
        }
        if (plotCount > Math.max(4, chapterCount * 2) && causalityCount < Math.max(2, chapterCount / 2)) {
            return "side_branch";
        }
        if (progressRatio >= 0.8) {
            return causalityCount >= Math.max(3, plotCount / 2) ? "convergence" : "payoff";
        }
        if (progressRatio >= 0.5 && causalityCount >= Math.max(3, chapterCount / 2)) {
            return "turning";
        }
        if (progressRatio >= 0.65) {
            return "payoff";
        }
        return "mainline_advance";
    }

    private int resolveConfidence(double progressRatio, int chapterCount, int plotCount, int causalityCount) {
        int confidence = 55;
        if (chapterCount >= 3) {
            confidence += 8;
        }
        if (plotCount >= 4) {
            confidence += 6;
        }
        if (causalityCount >= 2) {
            confidence += 6;
        }
        if (progressRatio >= 0.45 && progressRatio <= 0.8) {
            confidence += 4;
        }
        return Math.min(confidence, 82);
    }

    private String detectKeywordDrivenType(String normalizedContext) {
        if (!StringUtils.hasText(normalizedContext)) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : PROGRESS_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (normalizedContext.contains(keyword)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private double estimateProgressRatio(List<Chapter> chapters, Long targetChapterId) {
        if (chapters == null || chapters.isEmpty()) {
            return 0.0;
        }

        int maxOrder = chapters.stream()
                .map(Chapter::getOrderNum)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(chapters.size());

        int candidateOrder = maxOrder + 1;
        if (targetChapterId != null) {
            candidateOrder = chapters.stream()
                    .filter(item -> Objects.equals(item.getId(), targetChapterId))
                    .map(Chapter::getOrderNum)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(candidateOrder);
        }

        int denominator = Math.max(maxOrder + 1, 12);
        return Math.min(1.0, Math.max(0.0, candidateOrder / (double) denominator));
    }

    private List<Long> sampleIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().limit(8).toList();
    }

    private void requireProjectAccess(Long projectId, Long userId) {
        if (projectId == null || !projectService.hasProjectAccess(projectId, userId)) {
            throw new IllegalArgumentException("项目不存在或无权访问");
        }
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }

    private static Map<String, List<String>> createProgressKeywords() {
        Map<String, List<String>> keywords = new LinkedHashMap<>();
        keywords.put("mainline_advance", List.of("主线", "推进", "升级", "核心冲突"));
        keywords.put("side_branch", List.of("支线", "旁支", "插曲", "岔开"));
        keywords.put("foreshadow", List.of("伏笔", "铺垫", "埋线", "暗示"));
        keywords.put("turning", List.of("转折", "反转", "突变", "变局"));
        keywords.put("payoff", List.of("兑现", "回收", "揭晓", "收获"));
        keywords.put("convergence", List.of("收束", "汇合", "合流", "归拢"));
        return keywords;
    }
}
