package com.storyweaver.storyunit.context;

import java.util.Optional;

public interface ChapterAnchorBundleQueryService {

    Optional<ChapterAnchorBundleView> getChapterAnchorBundle(Long projectId, Long chapterId);
}
