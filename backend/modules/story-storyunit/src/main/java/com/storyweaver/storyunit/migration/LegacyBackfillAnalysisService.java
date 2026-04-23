package com.storyweaver.storyunit.migration;

import java.util.Optional;

public interface LegacyBackfillAnalysisService {

    Optional<LegacyChapterBackfillAnalysis> analyzeChapter(Long projectId, Long chapterId);
}
