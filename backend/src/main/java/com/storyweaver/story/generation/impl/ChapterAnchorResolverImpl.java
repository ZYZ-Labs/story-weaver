package com.storyweaver.story.generation.impl;

import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Outline;
import com.storyweaver.repository.OutlineMapper;
import com.storyweaver.service.ChapterService;
import com.storyweaver.service.OutlineService;
import com.storyweaver.story.generation.ChapterAnchorBundle;
import com.storyweaver.story.generation.ChapterAnchorResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
public class ChapterAnchorResolverImpl implements ChapterAnchorResolver {

    private final ChapterService chapterService;
    private final OutlineService outlineService;
    private final OutlineMapper outlineMapper;

    public ChapterAnchorResolverImpl(
            ChapterService chapterService,
            OutlineService outlineService,
            OutlineMapper outlineMapper) {
        this.chapterService = chapterService;
        this.outlineService = outlineService;
        this.outlineMapper = outlineMapper;
    }

    @Override
    public ChapterAnchorBundle resolve(Long userId, Long chapterId) {
        Chapter hydratedChapter = chapterService.getChapterWithAuth(chapterId, userId);
        if (hydratedChapter == null) {
            throw new IllegalArgumentException("章节不存在或无权访问");
        }

        Chapter rawChapter = chapterService.getById(chapterId);
        Long explicitOutlineId = rawChapter == null ? null : rawChapter.getOutlineId();

        ChapterAnchorBundle bundle = new ChapterAnchorBundle();
        bundle.setChapterId(hydratedChapter.getId());
        bundle.setProjectId(hydratedChapter.getProjectId());
        bundle.setChapterStatus(hydratedChapter.getChapterStatus());

        applyExplicitChapterAnchors(bundle, hydratedChapter);
        Outline outline = resolveOutline(hydratedChapter, explicitOutlineId, userId);
        if (outline != null) {
            applyOutlineAnchors(bundle, outline, explicitOutlineId);
        }

        deduplicate(bundle);
        markMissingSources(bundle);
        return bundle;
    }

    private void applyExplicitChapterAnchors(ChapterAnchorBundle bundle, Chapter chapter) {
        if (StringUtils.hasText(chapter.getSummary())) {
            bundle.setChapterSummary(chapter.getSummary().trim());
            bundle.markSource("chapterSummary", "explicit");
        }

        if (chapter.getMainPovCharacterId() != null) {
            bundle.setMainPovCharacterId(chapter.getMainPovCharacterId());
            bundle.setMainPovCharacterName(trimToNull(chapter.getMainPovCharacterName()));
            bundle.markSource("mainPov", "explicit");
        }

        if (chapter.getRequiredCharacterIds() != null && !chapter.getRequiredCharacterIds().isEmpty()) {
            bundle.setRequiredCharacterIds(new ArrayList<>(chapter.getRequiredCharacterIds()));
            bundle.setRequiredCharacterNames(copyList(chapter.getRequiredCharacterNames()));
            bundle.markSource("requiredCharacters", "explicit");
        }

        if (chapter.getStoryBeatIds() != null && !chapter.getStoryBeatIds().isEmpty()) {
            bundle.setStoryBeatIds(new ArrayList<>(chapter.getStoryBeatIds()));
            bundle.setStoryBeatTitles(copyList(chapter.getStoryBeatTitles()));
            bundle.markSource("storyBeats", "explicit");
        }
    }

    private void applyOutlineAnchors(ChapterAnchorBundle bundle, Outline outline, Long explicitOutlineId) {
        bundle.setChapterOutlineId(outline.getId());
        bundle.markSource("chapterOutline", Objects.equals(outline.getId(), explicitOutlineId) ? "explicit" : "derived_from_outline");

        Long volumeOutlineId = resolveVolumeOutlineId(outline);
        if (volumeOutlineId != null) {
            bundle.setVolumeOutlineId(volumeOutlineId);
            bundle.markSource("volumeOutline", "derived_from_outline");
        }

        if (!StringUtils.hasText(bundle.getChapterSummary())) {
            String outlineSummary = firstNonBlank(
                    outline.getSummary(),
                    outline.getStageGoal(),
                    outline.getExpectedEnding(),
                    outline.getContent()
            );
            if (StringUtils.hasText(outlineSummary)) {
                bundle.setChapterSummary(outlineSummary);
                bundle.markSource("chapterSummary", "derived_from_outline");
            }
        }

        if (bundle.getMainPovCharacterId() == null
                && outline.getFocusCharacterIdList() != null
                && !outline.getFocusCharacterIdList().isEmpty()) {
            bundle.setMainPovCharacterId(outline.getFocusCharacterIdList().get(0));
            String focusName = outline.getFocusCharacterNames() != null && !outline.getFocusCharacterNames().isEmpty()
                    ? outline.getFocusCharacterNames().get(0)
                    : null;
            bundle.setMainPovCharacterName(trimToNull(focusName));
            bundle.markSource("mainPov", "derived_from_outline");
        }

        if ((bundle.getRequiredCharacterIds() == null || bundle.getRequiredCharacterIds().isEmpty())
                && outline.getFocusCharacterIdList() != null
                && !outline.getFocusCharacterIdList().isEmpty()) {
            bundle.setRequiredCharacterIds(new ArrayList<>(outline.getFocusCharacterIdList()));
            bundle.setRequiredCharacterNames(copyList(outline.getFocusCharacterNames()));
            bundle.markSource("requiredCharacters", "derived_from_outline");
        }

        if ((bundle.getStoryBeatIds() == null || bundle.getStoryBeatIds().isEmpty())
                && outline.getRelatedPlotIdList() != null
                && !outline.getRelatedPlotIdList().isEmpty()) {
            bundle.setStoryBeatIds(new ArrayList<>(outline.getRelatedPlotIdList()));
            bundle.setStoryBeatTitles(copyList(outline.getRelatedPlotTitles()));
            bundle.markSource("storyBeats", "derived_from_outline");
        }

        if (outline.getRelatedWorldSettingIdList() != null && !outline.getRelatedWorldSettingIdList().isEmpty()) {
            bundle.setRelatedWorldSettingIds(new ArrayList<>(outline.getRelatedWorldSettingIdList()));
            bundle.setRelatedWorldSettingNames(copyList(outline.getRelatedWorldSettingNames()));
            bundle.markSource("worldSettings", "derived_from_outline");
        }
    }

    private Outline resolveOutline(Chapter chapter, Long explicitOutlineId, Long userId) {
        if (explicitOutlineId != null) {
            return outlineService.getOutlineWithAuth(explicitOutlineId, userId);
        }

        List<Outline> boundOutlines = outlineMapper.findByChapterId(chapter.getId());
        if (boundOutlines.isEmpty()) {
            return null;
        }
        return outlineService.getOutlineWithAuth(boundOutlines.get(0).getId(), userId);
    }

    private Long resolveVolumeOutlineId(Outline outline) {
        if (outline == null) {
            return null;
        }
        if ("volume".equalsIgnoreCase(trimToNull(outline.getOutlineType()))) {
            return outline.getId();
        }

        Long parentOutlineId = outline.getParentOutlineId();
        if (parentOutlineId != null) {
            Outline parent = outlineService.getById(parentOutlineId);
            if (isAvailable(parent) && "volume".equalsIgnoreCase(trimToNull(parent.getOutlineType()))) {
                return parent.getId();
            }
        }

        Long rootOutlineId = outline.getRootOutlineId();
        if (rootOutlineId != null && !Objects.equals(rootOutlineId, outline.getId())) {
            Outline root = outlineService.getById(rootOutlineId);
            if (isAvailable(root) && "volume".equalsIgnoreCase(trimToNull(root.getOutlineType()))) {
                return root.getId();
            }
        }
        return null;
    }

    private boolean isAvailable(Outline outline) {
        return outline != null && !Integer.valueOf(1).equals(outline.getDeleted());
    }

    private void deduplicate(ChapterAnchorBundle bundle) {
        bundle.setRequiredCharacterIds(deduplicateList(bundle.getRequiredCharacterIds()));
        bundle.setRequiredCharacterNames(deduplicateList(bundle.getRequiredCharacterNames()));
        bundle.setStoryBeatIds(deduplicateList(bundle.getStoryBeatIds()));
        bundle.setStoryBeatTitles(deduplicateList(bundle.getStoryBeatTitles()));
        bundle.setRelatedWorldSettingIds(deduplicateList(bundle.getRelatedWorldSettingIds()));
        bundle.setRelatedWorldSettingNames(deduplicateList(bundle.getRelatedWorldSettingNames()));
    }

    private void markMissingSources(ChapterAnchorBundle bundle) {
        if (bundle.getChapterOutlineId() == null) {
            bundle.markSource("chapterOutline", "missing");
        }
        if (bundle.getMainPovCharacterId() == null) {
            bundle.markSource("mainPov", "missing");
        }
        if (bundle.getRequiredCharacterIds() == null || bundle.getRequiredCharacterIds().isEmpty()) {
            bundle.markSource("requiredCharacters", "missing");
        }
        if (bundle.getStoryBeatIds() == null || bundle.getStoryBeatIds().isEmpty()) {
            bundle.markSource("storyBeats", "missing");
        }
        if (bundle.getRelatedWorldSettingIds() == null || bundle.getRelatedWorldSettingIds().isEmpty()) {
            bundle.markSource("worldSettings", "missing");
        }
        if (!StringUtils.hasText(bundle.getChapterSummary())) {
            bundle.markSource("chapterSummary", "missing");
        }
        if (bundle.getVolumeOutlineId() == null) {
            bundle.markSource("volumeOutline", "missing");
        }
    }

    private <T> List<T> deduplicateList(List<T> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    private List<String> copyList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(values);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
