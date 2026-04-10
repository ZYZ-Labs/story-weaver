package com.storyweaver.story.generation;

public interface StoryProgressPredictor {

    StoryProgressSuggestionVO suggestOutlineProgress(Long userId, Long projectId, Long targetOutlineId, String contextText);

    StoryProgressSuggestionVO suggestPlotProgress(Long userId, Long projectId, Long targetOutlineId, String contextText);

    StoryProgressSuggestionVO suggestChapterProgress(Long userId, Long projectId, Long targetChapterId, String contextText);
}
