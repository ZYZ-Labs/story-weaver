package com.storyweaver.storyunit.context;

import java.util.Optional;

public interface ReaderKnownStateQueryService {

    Optional<ReaderKnownStateView> getReaderKnownState(Long projectId, Long chapterId);
}
