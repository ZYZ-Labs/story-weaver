package com.storyweaver.story.generation.orchestration;

import java.util.Optional;

public interface ChapterExecutionReviewService {

    Optional<ChapterExecutionReview> review(Long projectId, Long chapterId);
}
