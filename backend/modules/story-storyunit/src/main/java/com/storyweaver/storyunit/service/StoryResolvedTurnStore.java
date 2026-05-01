package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.runtime.StoryResolvedTurn;

import java.util.List;

public interface StoryResolvedTurnStore {

    StoryResolvedTurn recordTurn(StoryResolvedTurn turn);

    List<StoryResolvedTurn> listChapterTurns(Long projectId, Long chapterId);
}
