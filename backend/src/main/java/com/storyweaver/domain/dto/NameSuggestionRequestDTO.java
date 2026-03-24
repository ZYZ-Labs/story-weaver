package com.storyweaver.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NameSuggestionRequestDTO {
    @NotBlank(message = "命名对象不能为空")
    private String entityType;

    private String brief;

    private String extraRequirements;

    private Integer count;
}
