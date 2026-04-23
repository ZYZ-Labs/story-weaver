package com.storyweaver.storyunit.migration;

import java.util.Optional;

public interface LegacyBackfillOverviewService {

    Optional<LegacyProjectBackfillOverview> buildProjectOverview(Long projectId);
}
