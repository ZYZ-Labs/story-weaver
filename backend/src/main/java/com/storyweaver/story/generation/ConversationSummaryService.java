package com.storyweaver.story.generation;

public interface ConversationSummaryService {

    SummarySuggestionPack suggestProjectBrief(Long userId, Long projectId, String inputText);

    SummarySuggestionPack suggestCharacterCard(Long userId, Long projectId, Long characterId, String inputText);

    SummarySuggestionPack suggestChapterBrief(Long userId, Long projectId, Long chapterId, String inputText);
}
