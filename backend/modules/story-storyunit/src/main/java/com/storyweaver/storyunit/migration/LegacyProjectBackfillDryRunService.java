package com.storyweaver.storyunit.migration;

import java.util.Optional;

public interface LegacyProjectBackfillDryRunService {

    Optional<LegacyProjectBackfillDryRun> buildProjectDryRun(Long projectId);
}
