package com.storyweaver.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AIWritingRequestDTO {

    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    @NotBlank(message = "当前内容不能为空")
    private String currentContent;

    private String userInstruction;

    private String writingType; // continue, polish, expand, rewrite

    private Integer maxTokens = 500;

    private Long selectedProviderId;

    private String selectedModel;

    private String promptSnapshot;

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public String getCurrentContent() {
        return currentContent;
    }

    public void setCurrentContent(String currentContent) {
        this.currentContent = currentContent;
    }

    public String getUserInstruction() {
        return userInstruction;
    }

    public void setUserInstruction(String userInstruction) {
        this.userInstruction = userInstruction;
    }

    public String getWritingType() {
        return writingType;
    }

    public void setWritingType(String writingType) {
        this.writingType = writingType;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Long getSelectedProviderId() {
        return selectedProviderId;
    }

    public void setSelectedProviderId(Long selectedProviderId) {
        this.selectedProviderId = selectedProviderId;
    }

    public String getSelectedModel() {
        return selectedModel;
    }

    public void setSelectedModel(String selectedModel) {
        this.selectedModel = selectedModel;
    }

    public String getPromptSnapshot() {
        return promptSnapshot;
    }

    public void setPromptSnapshot(String promptSnapshot) {
        this.promptSnapshot = promptSnapshot;
    }
}
