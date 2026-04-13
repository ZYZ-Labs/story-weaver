package com.storyweaver.storyunit.summary.workflow;

public interface StorySummaryProposalService {

    default StructuredPatchProposal propose(Long userId, SummaryInputDraft inputDraft) {
        return propose(inputDraft);
    }

    StructuredPatchProposal propose(SummaryInputDraft inputDraft);
}
