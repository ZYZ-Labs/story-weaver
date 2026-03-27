package com.storyweaver.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AIWritingChatMessageRequestDTO {

    @NotBlank(message = "消息内容不能为空")
    private String content;

    private Long selectedProviderId;

    private String selectedModel;

    private String entryPoint;
}
