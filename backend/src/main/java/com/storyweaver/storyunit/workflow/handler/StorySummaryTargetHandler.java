package com.storyweaver.storyunit.workflow.handler;

import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.summary.workflow.StructuredPatchProposal;
import com.storyweaver.storyunit.summary.workflow.SummaryApplyCommand;
import com.storyweaver.storyunit.summary.workflow.SummaryApplyResult;
import com.storyweaver.storyunit.summary.workflow.SummaryChangePreview;
import com.storyweaver.storyunit.summary.workflow.SummaryInputDraft;

public interface StorySummaryTargetHandler {

    StoryUnitType targetType();

    StructuredPatchProposal propose(Long userId, SummaryInputDraft inputDraft);

    SummaryChangePreview preview(Long userId, StructuredPatchProposal proposal);

    SummaryApplyResult apply(Long userId, SummaryApplyCommand command, StructuredPatchProposal proposal);
}
