package com.storyweaver.story.generation;

import java.util.List;

public interface StructuredCreationSuggestionService {

    List<StructuredCreationSuggestion> suggestFromText(Long userId, Long projectId, Long chapterId, String text);
}
