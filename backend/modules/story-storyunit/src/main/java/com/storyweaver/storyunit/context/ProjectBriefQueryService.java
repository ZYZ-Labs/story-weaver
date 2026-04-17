package com.storyweaver.storyunit.context;

import java.util.Optional;

public interface ProjectBriefQueryService {

    Optional<ProjectBriefView> getProjectBrief(Long projectId);
}
