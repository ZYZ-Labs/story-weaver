package com.storyweaver.storyunit.workflow.service;

import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.summary.workflow.StorySummaryProposalService;
import com.storyweaver.storyunit.summary.workflow.StructuredPatchProposal;
import com.storyweaver.storyunit.summary.workflow.SummaryInputDraft;
import com.storyweaver.storyunit.workflow.handler.StorySummaryTargetHandler;
import com.storyweaver.storyunit.workflow.support.SummaryProposalStore;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DefaultStorySummaryProposalWorkflowService implements StorySummaryProposalService {

    private final Map<StoryUnitType, StorySummaryTargetHandler> handlers;

    private final SummaryProposalStore proposalStore;

    public DefaultStorySummaryProposalWorkflowService(
            List<StorySummaryTargetHandler> handlers,
            SummaryProposalStore proposalStore) {
        Map<StoryUnitType, StorySummaryTargetHandler> mapping = new LinkedHashMap<>();
        for (StorySummaryTargetHandler handler : handlers) {
            mapping.put(handler.targetType(), handler);
        }
        this.handlers = Map.copyOf(mapping);
        this.proposalStore = proposalStore;
    }

    public StructuredPatchProposal propose(Long userId, SummaryInputDraft inputDraft) {
        StorySummaryTargetHandler handler = requireHandler(inputDraft.targetType());
        return proposalStore.save(handler.propose(userId, inputDraft));
    }

    @Override
    public StructuredPatchProposal propose(SummaryInputDraft inputDraft) {
        return propose(null, inputDraft);
    }

    private StorySummaryTargetHandler requireHandler(StoryUnitType unitType) {
        StorySummaryTargetHandler handler = handlers.get(unitType);
        if (handler == null) {
            throw new IllegalArgumentException("不支持的 Summary First 目标类型: " + unitType);
        }
        return handler;
    }
}
