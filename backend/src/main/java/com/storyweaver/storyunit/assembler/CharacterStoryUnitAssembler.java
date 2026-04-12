package com.storyweaver.storyunit.assembler;

import com.storyweaver.domain.entity.ChapterCharacterLink;
import com.storyweaver.domain.entity.ProjectCharacterLink;
import com.storyweaver.storyunit.adapter.AbstractStoryUnitAdapter;
import com.storyweaver.storyunit.assembler.AbstractStoryUnitAssembler;
import com.storyweaver.storyunit.assembler.StoryFacetAssembler;
import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.facet.canon.DefaultCanonFacet;
import com.storyweaver.storyunit.facet.relation.DefaultRelationFacet;
import com.storyweaver.storyunit.facet.summary.DefaultSummaryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryUnit;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.projection.CharacterProjectionSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CharacterStoryUnitAssembler extends AbstractStoryUnitAssembler<CharacterProjectionSource> {

    public CharacterStoryUnitAssembler() {
        super(new CharacterProjectionSourceAdapter(), List.of(
                new CharacterSummaryFacetAssembler(),
                new CharacterCanonFacetAssembler(),
                new CharacterRelationFacetAssembler()
        ));
    }

    private static final class CharacterProjectionSourceAdapter extends AbstractStoryUnitAdapter<CharacterProjectionSource> {

        private CharacterProjectionSourceAdapter() {
            super(StoryUnitType.CHARACTER, CharacterProjectionSource.class);
        }

        @Override
        protected Long extractProjectId(CharacterProjectionSource source) {
            return source.character().getProjectId();
        }

        @Override
        protected Object extractSourceId(CharacterProjectionSource source) {
            return source.character().getId();
        }
    }

    private static final class CharacterSummaryFacetAssembler implements StoryFacetAssembler<CharacterProjectionSource, DefaultSummaryFacet> {

        @Override
        public FacetType facetType() {
            return FacetType.SUMMARY;
        }

        @Override
        public Class<DefaultSummaryFacet> facetClass() {
            return DefaultSummaryFacet.class;
        }

        @Override
        public Optional<DefaultSummaryFacet> assemble(CharacterProjectionSource source, StoryUnit storyUnit) {
            String displayTitle = blankToEmpty(source.character().getName());
            String oneLine = firstNonBlank(joinWithSeparator("，", source.character().getIdentity(), source.character().getDescription()), displayTitle);
            String longSummary = firstNonBlank(source.character().getDescription(), source.character().getAdvancedProfileJson(), oneLine);
            String stateSummary = buildCharacterStateSummary(source);
            String relationSummary = "关联项目 " + source.projectLinks().size() + " 个，涉及章节 " + source.chapterLinks().size() + " 个";
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

        private String buildCharacterStateSummary(CharacterProjectionSource source) {
            List<String> parts = new ArrayList<>();
            if (hasText(source.character().getActiveStage())) {
                parts.add("当前阶段：" + source.character().getActiveStage().trim());
            }
            if (source.character().getIsRetired() != null) {
                parts.add(Integer.valueOf(1).equals(source.character().getIsRetired()) ? "状态：已退场" : "状态：活跃");
            }
            return String.join("；", parts);
        }
    }

    private static final class CharacterCanonFacetAssembler implements StoryFacetAssembler<CharacterProjectionSource, DefaultCanonFacet> {

        @Override
        public FacetType facetType() {
            return FacetType.CANON;
        }

        @Override
        public Class<DefaultCanonFacet> facetClass() {
            return DefaultCanonFacet.class;
        }

        @Override
        public Optional<DefaultCanonFacet> assemble(CharacterProjectionSource source, StoryUnit storyUnit) {
            Map<String, Object> fields = new LinkedHashMap<>();
            putIfHasText(fields, "name", source.character().getName());
            putIfHasText(fields, "identity", source.character().getIdentity());
            putIfHasText(fields, "description", source.character().getDescription());
            putIfHasText(fields, "coreGoal", source.character().getCoreGoal());
            putIfHasText(fields, "growthArc", source.character().getGrowthArc());
            putIfHasText(fields, "attributes", source.character().getAttributes());
            putIfHasText(fields, "advancedProfileJson", source.character().getAdvancedProfileJson());

            List<String> tags = new ArrayList<>();
            for (ProjectCharacterLink projectLink : source.projectLinks()) {
                if (hasText(projectLink.getRoleType())) {
                    tags.add(projectLink.getRoleType().trim());
                } else if (hasText(projectLink.getProjectRole())) {
                    tags.add(projectLink.getProjectRole().trim());
                }
            }
            return Optional.of(new DefaultCanonFacet(fields, distinct(tags)));
        }
    }

    private static final class CharacterRelationFacetAssembler implements StoryFacetAssembler<CharacterProjectionSource, DefaultRelationFacet> {

        @Override
        public FacetType facetType() {
            return FacetType.RELATION;
        }

        @Override
        public Class<DefaultRelationFacet> facetClass() {
            return DefaultRelationFacet.class;
        }

        @Override
        public Optional<DefaultRelationFacet> assemble(CharacterProjectionSource source, StoryUnit storyUnit) {
            Map<String, List<String>> relations = new LinkedHashMap<>();
            relations.put("projectRefs", source.projectLinks().stream()
                    .map(ProjectCharacterLink::getProjectId)
                    .filter(id -> id != null)
                    .map(id -> "project:" + id)
                    .distinct()
                    .toList());
            relations.put("chapterRefs", source.chapterLinks().stream()
                    .map(ChapterCharacterLink::getChapterId)
                    .filter(id -> id != null)
                    .map(id -> "chapter:" + id)
                    .distinct()
                    .toList());
            if (source.character().getFirstAppearanceChapterId() != null) {
                relations.put("firstAppearanceChapterRef", List.of("chapter:" + source.character().getFirstAppearanceChapterId()));
            }
            return Optional.of(new DefaultRelationFacet(relations));
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

    private static String joinWithSeparator(String separator, String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (hasText(value)) {
                parts.add(value.trim());
            }
        }
        return String.join(separator, parts);
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static void putIfHasText(Map<String, Object> target, String key, String value) {
        if (hasText(value)) {
            target.put(key, value.trim());
        }
    }

    private static List<String> distinct(List<String> values) {
        return List.copyOf(new LinkedHashSet<>(values));
    }
}
