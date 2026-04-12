package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.model.StoryUnitType;

import java.util.List;
import java.util.Optional;

public interface TypedStoryUnitProjectionService {

    StoryUnitType unitType();

    Optional<ProjectedStoryUnit> projectByUnitId(String unitId);

    List<ProjectedStoryUnit> listByProjectId(Long projectId);
}
