package com.storyweaver.storyunit.assembler;

import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.Plot;
import com.storyweaver.storyunit.adapter.AbstractStoryUnitAdapter;
import com.storyweaver.storyunit.assembler.AbstractStoryUnitAssembler;
import com.storyweaver.storyunit.assembler.StoryFacetAssembler;
import com.storyweaver.storyunit.facet.canon.DefaultCanonFacet;
import com.storyweaver.storyunit.facet.execution.DefaultExecutionFacet;
import com.storyweaver.storyunit.facet.relation.DefaultRelationFacet;
import com.storyweaver.storyunit.facet.summary.DefaultSummaryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryScope;
import com.storyweaver.storyunit.model.StoryUnit;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.projection.ChapterProjectionSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ChapterStoryUnitAssembler extends AbstractStoryUnitAssembler<ChapterProjectionSource> {

    public ChapterStoryUnitAssembler() {
        super(new ChapterProjectionSourceAdapter(), List.of(
                new ChapterSummaryFacetAssembler(),
                new ChapterCanonFacetAssembler(),
                new ChapterRelationFacetAssembler(),
                new ChapterExecutionFacetAssembler()
        ));
    }

    private static final class ChapterProjectionSourceAdapter extends AbstractStoryUnitAdapter<ChapterProjectionSource> {

        private ChapterProjectionSourceAdapter() {
            super(StoryUnitType.CHAPTER, ChapterProjectionSource.class);
        }

        @Override
        protected Long extractProjectId(ChapterProjectionSource source) {
            return source.chapter().getProjectId();
        }

        @Override
        protected Object extractSourceId(ChapterProjectionSource source) {
            return source.chapter().getId();
        }

        @Override
        protected StoryScope determineScope(ChapterProjectionSource source) {
            return StoryScope.CHAPTER;
        }
    }

    private static final class ChapterSummaryFacetAssembler implements StoryFacetAssembler<ChapterProjectionSource, DefaultSummaryFacet> {

        @Override
        public FacetType facetType() {
            return FacetType.SUMMARY;
        }

        @Override
        public Class<DefaultSummaryFacet> facetClass() {
            return DefaultSummaryFacet.class;
        }

        @Override
        public Optional<DefaultSummaryFacet> assemble(ChapterProjectionSource source, StoryUnit storyUnit) {
            String displayTitle = blankToEmpty(source.chapter().getTitle());
            String oneLine = firstNonBlank(source.chapter().getSummary(), displayTitle);
            String longSummary = firstNonBlank(source.chapter().getSummary(), abbreviate(source.chapter().getContent(), 240), oneLine);
            String stateSummary = buildStateSummary(source);
            String relationSummary = buildRelationSummary(source);
            return Optional.of(new DefaultSummaryFacet(
                    displayTitle,
                    oneLine,
                    longSummary,
                    stateSummary,
                    relationSummary,
                    "",
                    List.of()
            ));
        }

        private String buildStateSummary(ChapterProjectionSource source) {
            List<String> parts = new ArrayList<>();
            if (hasText(source.chapter().getChapterStatus())) {
                parts.add("章节状态：" + source.chapter().getChapterStatus().trim());
            }
            if (source.chapter().getWordCount() != null) {
                parts.add("字数：" + source.chapter().getWordCount());
            }
            return String.join("；", parts);
        }

        private String buildRelationSummary(ChapterProjectionSource source) {
            List<String> parts = new ArrayList<>();
            if (source.outline() != null && hasText(source.outline().getTitle())) {
                parts.add("关联大纲：" + source.outline().getTitle().trim());
            }
            if (source.mainPovCharacter() != null && hasText(source.mainPovCharacter().getName())) {
                parts.add("POV：" + source.mainPovCharacter().getName().trim());
            }
            if (!source.requiredCharacters().isEmpty()) {
                parts.add("涉及人物 " + source.requiredCharacters().size() + " 个");
            }
            if (!source.plots().isEmpty()) {
                parts.add("关联剧情 " + source.plots().size() + " 个");
            }
            return String.join("；", parts);
        }
    }

    private static final class ChapterCanonFacetAssembler implements StoryFacetAssembler<ChapterProjectionSource, DefaultCanonFacet> {

        @Override
        public FacetType facetType() {
            return FacetType.CANON;
        }

        @Override
        public Class<DefaultCanonFacet> facetClass() {
            return DefaultCanonFacet.class;
        }

        @Override
        public Optional<DefaultCanonFacet> assemble(ChapterProjectionSource source, StoryUnit storyUnit) {
            Map<String, Object> fields = new LinkedHashMap<>();
            putIfHasText(fields, "title", source.chapter().getTitle());
            putIfHasText(fields, "summary", source.chapter().getSummary());
            if (source.chapter().getOrderNum() != null) {
                fields.put("orderNum", source.chapter().getOrderNum());
            }
            if (source.chapter().getStatus() != null) {
                fields.put("status", source.chapter().getStatus());
            }
            if (hasText(source.chapter().getChapterStatus())) {
                fields.put("chapterStatus", source.chapter().getChapterStatus().trim());
            }
            return Optional.of(new DefaultCanonFacet(fields, List.of()));
        }
    }

    private static final class ChapterRelationFacetAssembler implements StoryFacetAssembler<ChapterProjectionSource, DefaultRelationFacet> {

        @Override
        public FacetType facetType() {
            return FacetType.RELATION;
        }

        @Override
        public Class<DefaultRelationFacet> facetClass() {
            return DefaultRelationFacet.class;
        }

        @Override
        public Optional<DefaultRelationFacet> assemble(ChapterProjectionSource source, StoryUnit storyUnit) {
            Map<String, List<String>> relations = new LinkedHashMap<>();
            if (source.outline() != null && source.outline().getId() != null) {
                relations.put("outlineRefs", List.of("outline:" + source.outline().getId()));
            }
            if (source.mainPovCharacter() != null && source.mainPovCharacter().getId() != null) {
                relations.put("mainPovCharacterRefs", List.of("character:" + source.mainPovCharacter().getId()));
            }
            relations.put("characterRefs", source.requiredCharacters().stream()
                    .map(Character::getId)
                    .filter(id -> id != null)
                    .map(id -> "character:" + id)
                    .toList());
            relations.put("plotRefs", source.plots().stream()
                    .map(Plot::getId)
                    .filter(id -> id != null)
                    .map(id -> "plot:" + id)
                    .toList());
            return Optional.of(new DefaultRelationFacet(relations));
        }
    }

    private static final class ChapterExecutionFacetAssembler implements StoryFacetAssembler<ChapterProjectionSource, DefaultExecutionFacet> {

        @Override
        public FacetType facetType() {
            return FacetType.EXECUTION;
        }

        @Override
        public Class<DefaultExecutionFacet> facetClass() {
            return DefaultExecutionFacet.class;
        }

        @Override
        public Optional<DefaultExecutionFacet> assemble(ChapterProjectionSource source, StoryUnit storyUnit) {
            String summary = "已预留章节锚点、readiness 与 handoff 扩展位";
            if (hasText(source.chapter().getChapterStatus())) {
                summary = "章节状态：" + source.chapter().getChapterStatus().trim() + "；" + summary;
            }
            return Optional.of(new DefaultExecutionFacet(summary, List.of(), ""));
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String abbreviate(String value, int maxLength) {
        if (!hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength).trim() + "...";
    }

    private static void putIfHasText(Map<String, Object> target, String key, String value) {
        if (hasText(value)) {
            target.put(key, value.trim());
        }
    }
}
