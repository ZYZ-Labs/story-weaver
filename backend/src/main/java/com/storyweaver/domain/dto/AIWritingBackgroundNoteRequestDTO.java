package com.storyweaver.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AIWritingBackgroundNoteRequestDTO {

    @NotBlank(message = "背景信息内容不能为空")
    private String content;
}
