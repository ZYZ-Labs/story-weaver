package com.storyweaver.domain.dto;

import lombok.Data;

@Data
public class SummarySuggestionRequestDTO {

    private String inputText;

    private String contextMode;

    private String currentContext;

    private Long characterId;
}
