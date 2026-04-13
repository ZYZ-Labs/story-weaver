package com.storyweaver.domain.dto;

import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.summary.workflow.SummaryInputIntent;
import com.storyweaver.storyunit.summary.workflow.SummaryOperatorMode;
import lombok.Data;

@Data
public class SummaryWorkflowProposalRequestDTO {

    private StoryUnitType targetType;

    private Long targetSourceId;

    private Long projectId;

    private String summaryText;

    private SummaryInputIntent intent = SummaryInputIntent.UPDATE;

    private SummaryOperatorMode operatorMode = SummaryOperatorMode.DEFAULT;
}
