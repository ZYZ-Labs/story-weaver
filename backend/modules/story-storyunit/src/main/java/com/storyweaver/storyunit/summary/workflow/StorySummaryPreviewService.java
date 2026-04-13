package com.storyweaver.storyunit.summary.workflow;

public interface StorySummaryPreviewService {

    default SummaryChangePreview preview(Long userId, StructuredPatchProposal proposal) {
        return preview(proposal);
    }

    SummaryChangePreview preview(StructuredPatchProposal proposal);
}
