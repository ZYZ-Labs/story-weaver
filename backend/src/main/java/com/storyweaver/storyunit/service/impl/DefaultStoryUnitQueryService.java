package com.storyweaver.storyunit.service.impl;

import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.service.ProjectedStoryUnit;
import com.storyweaver.storyunit.service.StoryUnitProjectionService;
import com.storyweaver.storyunit.service.StoryUnitQueryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DefaultStoryUnitQueryService implements StoryUnitQueryService {

    private final StoryUnitProjectionService projectionService;

    public DefaultStoryUnitQueryService(StoryUnitProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @Override
    public Optional<ProjectedStoryUnit> getProjected(StoryUnitRef ref) {
        return projectionService.project(ref);
    }

    @Override
    public List<ProjectedStoryUnit> listProjected(Long projectId, StoryUnitType unitType) {
        return projectionService.listProjected(projectId, unitType);
    }
}
