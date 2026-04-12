package com.storyweaver.domain.dto;

import com.storyweaver.story.generation.StructuredCreationSuggestion;
import lombok.Data;

@Data
public class StructuredCreationApplyRequestDTO {

    private Long projectId;

    private StructuredCreationSuggestion suggestion;
}
