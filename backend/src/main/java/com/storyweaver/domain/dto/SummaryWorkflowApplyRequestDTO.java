package com.storyweaver.domain.dto;

import com.storyweaver.storyunit.model.StoryUnitRef;
import lombok.Data;

@Data
public class SummaryWorkflowApplyRequestDTO {

    private String proposalId;

    private StoryUnitRef targetRef;

    private Boolean confirmed = Boolean.TRUE;
}
