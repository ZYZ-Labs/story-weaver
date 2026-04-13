package com.storyweaver.storyunit.workflow.support;

import com.storyweaver.storyunit.summary.workflow.StructuredPatchProposal;

import java.util.Optional;

public interface SummaryProposalStore {

    StructuredPatchProposal save(StructuredPatchProposal proposal);

    Optional<StructuredPatchProposal> find(String proposalId);

    void remove(String proposalId);
}
