package com.storyweaver.story.generation;

public interface GenerationReadinessService {

    GenerationReadinessVO evaluate(Long userId, Long chapterId);
}
