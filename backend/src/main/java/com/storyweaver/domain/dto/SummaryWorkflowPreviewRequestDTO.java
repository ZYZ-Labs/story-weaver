package com.storyweaver.domain.dto;

import com.storyweaver.storyunit.summary.workflow.StructuredPatchProposal;
import lombok.Data;

@Data
public class SummaryWorkflowPreviewRequestDTO {

    private String proposalId;

    private StructuredPatchProposal proposal;
}
