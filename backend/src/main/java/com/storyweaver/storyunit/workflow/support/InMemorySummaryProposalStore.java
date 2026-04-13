package com.storyweaver.storyunit.workflow.support;

import com.storyweaver.storyunit.summary.workflow.StructuredPatchProposal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySummaryProposalStore implements SummaryProposalStore {

    private final Map<String, StructuredPatchProposal> proposals = new ConcurrentHashMap<>();

    @Override
    public StructuredPatchProposal save(StructuredPatchProposal proposal) {
        proposals.put(proposal.proposalId(), proposal);
        return proposal;
    }

    @Override
    public Optional<StructuredPatchProposal> find(String proposalId) {
        return Optional.ofNullable(proposals.get(proposalId));
    }

    @Override
    public void remove(String proposalId) {
        proposals.remove(proposalId);
    }
}
