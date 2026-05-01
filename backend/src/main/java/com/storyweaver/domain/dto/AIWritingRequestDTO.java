package com.storyweaver.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AIWritingRequestDTO {

    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    private String currentContent;

    private String userInstruction;

    private String writingType; // draft, continue, polish, expand, rewrite

    private Integer maxTokens;

    private Long selectedProviderId;

    private String selectedModel;

    private String promptSnapshot;

    private String entryPoint;

    private String sceneId;

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

    public String getEntryPoint() {
        return entryPoint;
    }

    public void setEntryPoint(String entryPoint) {
        this.entryPoint = entryPoint;
    }

    public String getSceneId() {
        return sceneId;
    }

    public void setSceneId(String sceneId) {
        this.sceneId = sceneId;
    }
}
