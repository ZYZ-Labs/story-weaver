package com.storyweaver.storyunit.migration;

import java.util.Optional;

public interface LegacyBackfillDryRunService {

    Optional<LegacyBackfillDryRun> planChapterBackfill(Long projectId, Long chapterId);
}
