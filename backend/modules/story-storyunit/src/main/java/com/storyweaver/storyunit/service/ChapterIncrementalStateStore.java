package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.facet.state.ChapterIncrementalState;

import java.util.Optional;

public interface ChapterIncrementalStateStore {

    ChapterIncrementalState saveChapterState(ChapterIncrementalState state);

    Optional<ChapterIncrementalState> findChapterState(Long projectId, Long chapterId);
}
