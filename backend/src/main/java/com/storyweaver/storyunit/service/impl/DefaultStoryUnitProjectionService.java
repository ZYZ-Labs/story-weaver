package com.storyweaver.storyunit.service.impl;

import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.service.ProjectedStoryUnit;
import com.storyweaver.storyunit.service.StoryUnitProjectionService;
import com.storyweaver.storyunit.service.TypedStoryUnitProjectionService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DefaultStoryUnitProjectionService implements StoryUnitProjectionService {

    private final Map<StoryUnitType, TypedStoryUnitProjectionService> delegates;

    public DefaultStoryUnitProjectionService(List<TypedStoryUnitProjectionService> delegates) {
        Map<StoryUnitType, TypedStoryUnitProjectionService> mapping = new LinkedHashMap<>();
        for (TypedStoryUnitProjectionService delegate : delegates) {
            mapping.put(delegate.unitType(), delegate);
        }
        this.delegates = Map.copyOf(mapping);
    }

    @Override
    public Optional<ProjectedStoryUnit> project(StoryUnitRef ref) {
        if (ref == null || ref.unitType() == null) {
            return Optional.empty();
        }
        TypedStoryUnitProjectionService delegate = delegates.get(ref.unitType());
        if (delegate == null) {
            return Optional.empty();
        }
        return delegate.projectByUnitId(ref.unitId());
    }

    @Override
    public List<ProjectedStoryUnit> listProjected(Long projectId, StoryUnitType unitType) {
        TypedStoryUnitProjectionService delegate = delegates.get(unitType);
        if (delegate == null) {
            return List.of();
        }
        return delegate.listByProjectId(projectId);
    }
}
