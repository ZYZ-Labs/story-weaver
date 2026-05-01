package com.storyweaver.story.generation.orchestration;

import com.storyweaver.domain.entity.Chapter;

import java.util.List;
import java.util.Map;

public interface ChapterNarrativeRuntimeModeService {

    ChapterNarrativeRuntimeMode getMode(Long projectId, Long chapterId);

    Map<Long, ChapterNarrativeRuntimeMode> getModes(List<Chapter> chapters);

    ChapterNarrativeRuntimeMode updateMode(Chapter chapter, ChapterNarrativeRuntimeMode targetMode);

    void assertSceneMode(Long projectId, Long chapterId, String actionLabel);

    void assertSceneMode(Chapter chapter, String actionLabel);

    void assertNodeMode(Long projectId, Long chapterId, String actionLabel);

    void assertNodeMode(Chapter chapter, String actionLabel);
}
