package com.storyweaver.story.generation;

public interface ChapterAnchorResolver {

    ChapterAnchorBundle resolve(Long userId, Long chapterId);
}
