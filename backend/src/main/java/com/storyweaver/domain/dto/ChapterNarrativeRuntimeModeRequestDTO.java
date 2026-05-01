package com.storyweaver.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChapterNarrativeRuntimeModeRequestDTO {

    @NotBlank(message = "章节运行模式不能为空")
    private String mode;
}
