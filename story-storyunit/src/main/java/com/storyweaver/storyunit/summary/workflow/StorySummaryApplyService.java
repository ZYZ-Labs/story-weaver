package com.storyweaver.storyunit.summary.workflow;

public interface StorySummaryApplyService {

    default SummaryApplyResult apply(Long userId, SummaryApplyCommand command) {
        return apply(command);
    }

    SummaryApplyResult apply(SummaryApplyCommand command);
}
