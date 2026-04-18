package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;

import java.util.Optional;

public interface ReaderRevealStateStore {

    ReaderRevealState saveChapterRevealState(ReaderRevealState state);

    Optional<ReaderRevealState> findChapterRevealState(Long projectId, Long chapterId);
}
