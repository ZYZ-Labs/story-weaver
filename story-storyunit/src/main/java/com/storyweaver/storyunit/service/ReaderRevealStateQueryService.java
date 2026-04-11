package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;

public interface ReaderRevealStateQueryService {

    ReaderRevealState getProjectRevealState(Long projectId);

    ReaderRevealState getChapterRevealState(Long projectId, Long chapterId);
}
