package com.storyweaver.storyunit.migration;

import java.util.Optional;

public interface LegacyBackfillExecutionService {

    Optional<LegacyBackfillExecutionResult> executeChapterBackfill(Long projectId, Long chapterId);
}
