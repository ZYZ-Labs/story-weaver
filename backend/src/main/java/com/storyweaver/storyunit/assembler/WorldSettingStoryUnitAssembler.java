package com.storyweaver.storyunit.assembler;

import com.storyweaver.domain.entity.OutlineWorldSettingLink;
import com.storyweaver.domain.entity.ProjectWorldSettingLink;
import com.storyweaver.storyunit.adapter.AbstractStoryUnitAdapter;
import com.storyweaver.storyunit.assembler.AbstractStoryUnitAssembler;
import com.storyweaver.storyunit.assembler.StoryFacetAssembler;
import com.storyweaver.storyunit.facet.canon.DefaultCanonFacet;
import com.storyweaver.storyunit.facet.relation.DefaultRelationFacet;
import com.storyweaver.storyunit.facet.summary.DefaultSummaryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryUnit;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.projection.WorldSettingProjectionSource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class WorldSettingStoryUnitAssembler extends AbstractStoryUnitAssembler<WorldSettingProjectionSource> {

    public WorldSettingStoryUnitAssembler() {
        super(new WorldSettingProjectionSourceAdapter(), List.of(
                new WorldSettingSummaryFacetAssembler(),
                new WorldSettingCanonFacetAssembler(),
                new WorldSettingRelationFacetAssembler()
        ));
    }

    private static final class WorldSettingProjectionSourceAdapter extends AbstractStoryUnitAdapter<WorldSettingProjectionSource> {

        private WorldSettingProjectionSourceAdapter() {
            super(StoryUnitType.WORLD_SETTING, WorldSettingProjectionSource.class);
        }

        @Override
        protected Long extractProjectId(WorldSettingProjectionSource source) {
            return source.worldSetting().getProjectId();
        }

        @Override
        protected Object extractSourceId(WorldSettingProjectionSource source) {
            return source.worldSetting().getId();
        }
    }

    private static final class WorldSettingSummaryFacetAssembler implements StoryFacetAssembler<WorldSettingProjectionSource, DefaultSummaryFacet> {

        @Override
        public FacetType facetType() {
            return FacetType.SUMMARY;
        }

        @Override
        public Class<DefaultSummaryFacet> facetClass() {
            return DefaultSummaryFacet.class;
        }

        @Override
        public Optional<DefaultSummaryFacet> assemble(WorldSettingProjectionSource source, StoryUnit storyUnit) {
            String displayTitle = firstNonBlank(source.worldSetting().getTitle(), source.worldSetting().getName());
            String oneLine = firstNonBlank(source.worldSetting().getDescription(), displayTitle);
            String longSummary = firstNonBlank(source.worldSetting().getContent(), source.worldSetting().getDescription(), oneLine);
            String relationSummary = "关联项目 " + source.projectLinks().size() + " 个，关联大纲 " + source.outlineLinks().size() + " 个";
            return Optional.of(new DefaultSummaryFacet(
                    displayTitle,
                    oneLine,
                    longSummary,
                    "",
                    relationSummary,
                    "",
                    List.of()
            ));
        }
    }

    private static final class WorldSettingCanonFacetAssembler implements StoryFacetAssembler<WorldSettingProjectionSource, DefaultCanonFacet> {

        @Override
        public FacetType facetType() {
            return FacetType.CANON;
        }

        @Override
        public Class<DefaultCanonFacet> facetClass() {
            return DefaultCanonFacet.class;
        }

        @Override
        public Optional<DefaultCanonFacet> assemble(WorldSettingProjectionSource source, StoryUnit storyUnit) {
            Map<String, Object> fields = new LinkedHashMap<>();
            putIfHasText(fields, "title", source.worldSetting().getTitle());
            putIfHasText(fields, "name", source.worldSetting().getName());
            putIfHasText(fields, "description", source.worldSetting().getDescription());
            putIfHasText(fields, "content", source.worldSetting().getContent());
            putIfHasText(fields, "category", source.worldSetting().getCategory());
            List<String> tags = hasText(source.worldSetting().getCategory())
                    ? List.of(source.worldSetting().getCategory().trim())
                    : List.of();
            return Optional.of(new DefaultCanonFacet(fields, tags));
        }
    }

    private static final class WorldSettingRelationFacetAssembler implements StoryFacetAssembler<WorldSettingProjectionSource, DefaultRelationFacet> {

        @Override
        public FacetType facetType() {
            return FacetType.RELATION;
        }

        @Override
        public Class<DefaultRelationFacet> facetClass() {
            return DefaultRelationFacet.class;
        }

        @Override
        public Optional<DefaultRelationFacet> assemble(WorldSettingProjectionSource source, StoryUnit storyUnit) {
            Map<String, List<String>> relations = new LinkedHashMap<>();
            relations.put("projectRefs", source.projectLinks().stream()
                    .map(ProjectWorldSettingLink::getProjectId)
                    .filter(id -> id != null)
                    .map(id -> "project:" + id)
                    .distinct()
                    .toList());
            relations.put("outlineRefs", source.outlineLinks().stream()
                    .map(OutlineWorldSettingLink::getOutlineId)
                    .filter(id -> id != null)
                    .map(id -> "outline:" + id)
                    .distinct()
                    .toList());
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

    private static void putIfHasText(Map<String, Object> target, String key, String value) {
        if (hasText(value)) {
            target.put(key, value.trim());
        }
    }
}
