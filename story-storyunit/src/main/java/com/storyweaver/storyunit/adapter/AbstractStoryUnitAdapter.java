package com.storyweaver.storyunit.adapter;

import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryFacetRef;
import com.storyweaver.storyunit.model.StoryScope;
import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.model.StoryUnit;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitStatus;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.model.StoryUnitVersion;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractStoryUnitAdapter<S> implements StoryUnitAdapter<S> {

    private final StoryUnitType unitType;

    private final Class<S> sourceType;

    protected AbstractStoryUnitAdapter(StoryUnitType unitType, Class<S> sourceType) {
        this.unitType = Objects.requireNonNull(unitType, "unitType must not be null");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
    }

    @Override
    public StoryUnitType unitType() {
        return unitType;
    }

    @Override
    public Class<S> sourceType() {
        return sourceType;
    }

    @Override
    public StoryUnitRef toUnitRef(S source) {
        Objects.requireNonNull(source, "source must not be null");
        String sourceId = normalizeSourceId(extractSourceId(source));
        return new StoryUnitRef(sourceId, defaultUnitKey(sourceId), unitType());
    }

    @Override
    public StoryUnit toStoryUnit(S source) {
        Objects.requireNonNull(source, "source must not be null");
        return new StoryUnit(
                toUnitRef(source),
                extractProjectId(source),
                determineScope(source),
                facetRefs(source),
                determineStatus(source),
                determineVersion(source),
                determineSnapshotId(source),
                new StorySourceTrace(determineCreatedBy(source), determineUpdatedBy(source), sourceType.getSimpleName(), normalizeSourceId(extractSourceId(source))));
    }

    protected abstract Long extractProjectId(S source);

    protected abstract Object extractSourceId(S source);

    protected StoryScope determineScope(S source) {
        return StoryScope.PROJECT;
    }

    protected StoryUnitStatus determineStatus(S source) {
        return StoryUnitStatus.DRAFT;
    }

    protected StoryUnitVersion determineVersion(S source) {
        return new StoryUnitVersion(0L);
    }

    protected String determineSnapshotId(S source) {
        return null;
    }

    protected Map<FacetType, StoryFacetRef> facetRefs(S source) {
        return Map.of();
    }

    protected String determineCreatedBy(S source) {
        return "phase1a-adapter";
    }

    protected String determineUpdatedBy(S source) {
        return "phase1a-adapter";
    }

    protected String defaultUnitKey(String sourceId) {
        return unitType.name().toLowerCase(Locale.ROOT) + ":" + sourceId;
    }

    private String normalizeSourceId(Object sourceId) {
        return Objects.requireNonNull(sourceId, "sourceId must not be null").toString().trim();
    }
}
