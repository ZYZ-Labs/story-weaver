package com.storyweaver.story.generation.impl;

import com.storyweaver.story.generation.ChapterAnchorBundle;
import com.storyweaver.story.generation.ChapterAnchorResolver;
import com.storyweaver.story.generation.GenerationReadinessService;
import com.storyweaver.story.generation.GenerationReadinessVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;

@Service
public class GenerationReadinessServiceImpl implements GenerationReadinessService {

    private final ChapterAnchorResolver chapterAnchorResolver;

    public GenerationReadinessServiceImpl(ChapterAnchorResolver chapterAnchorResolver) {
        this.chapterAnchorResolver = chapterAnchorResolver;
    }

    @Override
    public GenerationReadinessVO evaluate(Long userId, Long chapterId) {
        ChapterAnchorBundle anchors = chapterAnchorResolver.resolve(userId, chapterId);

        GenerationReadinessVO readiness = new GenerationReadinessVO();
        readiness.setResolvedAnchors(anchors);

        int score = 100;
        LinkedHashSet<String> recommendedModules = new LinkedHashSet<>();

        boolean hasBriefOrOutline = StringUtils.hasText(anchors.getChapterSummary()) || anchors.getChapterOutlineId() != null;
        if (!hasBriefOrOutline) {
            readiness.addBlockingIssue("缺少章纲或章节 brief，无法稳定约束本章生成范围。");
            recommendedModules.add("chapter_brief");
            score -= 30;
        }

        if (anchors.getMainPovCharacterId() == null) {
            readiness.addBlockingIssue("缺少 POV 人物锚点。");
            recommendedModules.add("main_pov");
            score -= 25;
        }

        if (!anchors.hasCharacterAnchor()) {
            readiness.addBlockingIssue("缺少人物锚点，至少需要 POV 或必出人物。");
            recommendedModules.add("required_characters");
            score -= 20;
        } else if (anchors.getRequiredCharacterIds() == null || anchors.getRequiredCharacterIds().isEmpty()) {
            readiness.addWarning("建议补充本章必出人物，避免人物出场与戏份分配漂移。");
            recommendedModules.add("required_characters");
            score -= 10;
        }

        if (anchors.getStoryBeatIds() == null || anchors.getStoryBeatIds().isEmpty()) {
            readiness.addBlockingIssue("缺少剧情推进锚点，当前章节目标不够明确。");
            recommendedModules.add("story_beats");
            score -= 25;
        }

        if ("derived_from_outline".equals(anchors.sourceOf("mainPov"))) {
            readiness.addWarning("当前 POV 来自章纲派生，建议显式确认。");
            recommendedModules.add("main_pov");
            score -= 5;
        }
        if ("derived_from_outline".equals(anchors.sourceOf("requiredCharacters"))) {
            readiness.addWarning("当前必出人物来自章纲派生，建议显式确认。");
            recommendedModules.add("required_characters");
            score -= 5;
        }
        if ("derived_from_outline".equals(anchors.sourceOf("storyBeats"))) {
            readiness.addWarning("当前剧情推进点来自章纲派生，建议显式确认。");
            recommendedModules.add("story_beats");
            score -= 5;
        }
        if ("derived_from_outline".equals(anchors.sourceOf("chapterSummary"))) {
            readiness.addWarning("当前章节摘要来自章纲派生，建议补充章节 brief。");
            recommendedModules.add("chapter_brief");
            score -= 5;
        }
        if ("derived_from_outline".equals(anchors.sourceOf("chapterOutline"))) {
            readiness.addWarning("当前章纲绑定来自派生关系，建议做一次显式绑定。");
            recommendedModules.add("outline_binding");
            score -= 5;
        }

        readiness.setScore(Math.max(0, score));
        readiness.setStatus(resolveStatus(readiness));
        readiness.setRecommendedModules(new ArrayList<>(recommendedModules));
        return readiness;
    }

    private String resolveStatus(GenerationReadinessVO readiness) {
        if (readiness.isBlocked()) {
            return "blocked";
        }
        return readiness.getWarnings() == null || readiness.getWarnings().isEmpty() ? "ready" : "warning";
    }
}
