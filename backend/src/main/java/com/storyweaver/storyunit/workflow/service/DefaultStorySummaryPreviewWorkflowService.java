package com.storyweaver.storyunit.workflow.service;

import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.summary.workflow.StorySummaryPreviewService;
import com.storyweaver.storyunit.summary.workflow.StructuredPatchProposal;
import com.storyweaver.storyunit.summary.workflow.SummaryChangePreview;
import com.storyweaver.storyunit.workflow.handler.StorySummaryTargetHandler;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DefaultStorySummaryPreviewWorkflowService implements StorySummaryPreviewService {

    private final Map<StoryUnitType, StorySummaryTargetHandler> handlers;

    public DefaultStorySummaryPreviewWorkflowService(List<StorySummaryTargetHandler> handlers) {
        Map<StoryUnitType, StorySummaryTargetHandler> mapping = new LinkedHashMap<>();
        for (StorySummaryTargetHandler handler : handlers) {
            mapping.put(handler.targetType(), handler);
        }
        this.handlers = Map.copyOf(mapping);
    }

    public SummaryChangePreview preview(Long userId, StructuredPatchProposal proposal) {
        return requireHandler(proposal.targetRef().unitType()).preview(userId, proposal);
    }

    @Override
    public SummaryChangePreview preview(StructuredPatchProposal proposal) {
        return preview(null, proposal);
    }

    private StorySummaryTargetHandler requireHandler(StoryUnitType unitType) {
        StorySummaryTargetHandler handler = handlers.get(unitType);
        if (handler == null) {
            throw new IllegalArgumentException("不支持的 Summary First 目标类型: " + unitType);
        }
        return handler;
    }
}
