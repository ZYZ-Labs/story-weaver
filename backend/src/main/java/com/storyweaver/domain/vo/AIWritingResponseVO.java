package com.storyweaver.domain.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AIWritingResponseVO {

    private Long id;

    private Long chapterId;

    private String originalContent;

    private String generatedContent;

    private String writingType;

    private String userInstruction;

    private Long selectedProviderId;

    private String selectedModel;

    private String promptSnapshot;

    private Long directorDecisionId;

    private JsonNode generationTrace;

    private String status;

    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getGeneratedContent() {
        return generatedContent;
    }

    public void setGeneratedContent(String generatedContent) {
        this.generatedContent = generatedContent;
    }

    public String getWritingType() {
        return writingType;
    }

    public void setWritingType(String writingType) {
        this.writingType = writingType;
    }

    public String getUserInstruction() {
        return userInstruction;
    }

    public void setUserInstruction(String userInstruction) {
        this.userInstruction = userInstruction;
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

    public Long getDirectorDecisionId() {
        return directorDecisionId;
    }

    public void setDirectorDecisionId(Long directorDecisionId) {
        this.directorDecisionId = directorDecisionId;
    }

    public JsonNode getGenerationTrace() {
        return generationTrace;
    }

    public void setGenerationTrace(JsonNode generationTrace) {
        this.generationTrace = generationTrace;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
