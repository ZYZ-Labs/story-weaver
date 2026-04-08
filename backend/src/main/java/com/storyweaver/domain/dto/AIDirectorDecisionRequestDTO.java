package com.storyweaver.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AIDirectorDecisionRequestDTO {

    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    private String currentContent;

    private String userInstruction;

    private String writingType;

    private String entryPoint;

    private String sourceType;

    private Boolean forceRefresh;

    private Long selectedProviderId;

    private String selectedModel;
}
