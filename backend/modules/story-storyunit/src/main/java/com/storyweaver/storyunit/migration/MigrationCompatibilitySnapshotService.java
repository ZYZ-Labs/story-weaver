package com.storyweaver.storyunit.migration;

import java.util.Optional;

public interface MigrationCompatibilitySnapshotService {

    Optional<MigrationCompatibilitySnapshot> getChapterSnapshot(Long projectId, Long chapterId);
}
