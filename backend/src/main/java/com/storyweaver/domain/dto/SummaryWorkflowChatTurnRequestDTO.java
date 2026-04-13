package com.storyweaver.domain.dto;

import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.summary.workflow.SummaryInputIntent;
import com.storyweaver.storyunit.summary.workflow.SummaryOperatorMode;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SummaryWorkflowChatTurnRequestDTO {

    private StoryUnitType targetType;

    private Long targetSourceId;

    private Long projectId;

    private String title;

    private String existingSummary;

    private String currentDraftSummary;

    private SummaryInputIntent intent = SummaryInputIntent.REFINE;

    private SummaryOperatorMode operatorMode = SummaryOperatorMode.DEFAULT;

    private Long selectedProviderId;

    private String selectedModel;

    private List<SummaryWorkflowChatMessageDTO> messages = new ArrayList<>();
}
