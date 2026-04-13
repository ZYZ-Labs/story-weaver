package com.storyweaver.storyunit.workflow.service;

import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.summary.workflow.StorySummaryApplyService;
import com.storyweaver.storyunit.summary.workflow.StructuredPatchProposal;
import com.storyweaver.storyunit.summary.workflow.SummaryApplyCommand;
import com.storyweaver.storyunit.summary.workflow.SummaryApplyResult;
import com.storyweaver.storyunit.workflow.exception.SummaryProposalConflictException;
import com.storyweaver.storyunit.workflow.exception.SummaryProposalNotFoundException;
import com.storyweaver.storyunit.workflow.handler.StorySummaryTargetHandler;
import com.storyweaver.storyunit.workflow.support.SummaryProposalStore;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DefaultStorySummaryApplyWorkflowService implements StorySummaryApplyService {

    private final Map<StoryUnitType, StorySummaryTargetHandler> handlers;

    private final SummaryProposalStore proposalStore;

    public DefaultStorySummaryApplyWorkflowService(
            List<StorySummaryTargetHandler> handlers,
            SummaryProposalStore proposalStore) {
        Map<StoryUnitType, StorySummaryTargetHandler> mapping = new LinkedHashMap<>();
        for (StorySummaryTargetHandler handler : handlers) {
            mapping.put(handler.targetType(), handler);
        }
        this.handlers = Map.copyOf(mapping);
        this.proposalStore = proposalStore;
    }

    public SummaryApplyResult apply(Long userId, SummaryApplyCommand command) {
        StructuredPatchProposal proposal = proposalStore.find(command.proposalId())
                .orElseThrow(() -> new SummaryProposalNotFoundException("proposal 不存在、已过期或已被清理"));
        if (!proposal.targetRef().equals(command.targetRef())) {
            throw new SummaryProposalConflictException("proposal 目标与当前操作对象不匹配");
        }
        SummaryApplyResult result = requireHandler(proposal.targetRef().unitType()).apply(userId, command, proposal);
        proposalStore.remove(command.proposalId());
        return result;
    }

    @Override
    public SummaryApplyResult apply(SummaryApplyCommand command) {
        return apply(null, command);
    }

    private StorySummaryTargetHandler requireHandler(StoryUnitType unitType) {
        StorySummaryTargetHandler handler = handlers.get(unitType);
        if (handler == null) {
            throw new IllegalArgumentException("不支持的 Summary First 目标类型: " + unitType);
        }
        return handler;
    }
}
