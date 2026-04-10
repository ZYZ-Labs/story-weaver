package com.storyweaver.story.generation;

public interface StoryConsistencyInspector {

    StoryConsistencyReportVO inspectProject(Long userId, Long projectId);
}
