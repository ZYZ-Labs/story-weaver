package com.storyweaver.story.generation;

import lombok.Data;

@Data
public class StructuredSummaryApplyResult {

    private String scope;

    private Long targetId;

    private Object target;

    private Long canonDocumentId;

    private GenerationReadinessVO generationReadiness;
}
