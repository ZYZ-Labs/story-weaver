package com.storyweaver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "story.compatibility")
public class StoryCompatibilityProperties {

    private boolean legacyWritingCenterEnabled = true;

    private boolean legacyWritingApiEnabled = true;

    private boolean chapterWorkspacePrimary = true;

    private boolean stateCenterPrimary = true;

    private boolean generationCenterPrimary = true;

    private boolean storyContextDualReadEnabled = true;

    private boolean summaryWorkflowDualWriteEnabled = true;

    private boolean backfillExecuteEnabled = true;

    private boolean chapterWorkspaceNodePreviewEnabled = true;

    private boolean chapterWorkspaceNodeResolveEnabled = false;

    public boolean isLegacyWritingCenterEnabled() {
        return legacyWritingCenterEnabled;
    }

    public void setLegacyWritingCenterEnabled(boolean legacyWritingCenterEnabled) {
        this.legacyWritingCenterEnabled = legacyWritingCenterEnabled;
    }

    public boolean isLegacyWritingApiEnabled() {
        return legacyWritingApiEnabled;
    }

    public void setLegacyWritingApiEnabled(boolean legacyWritingApiEnabled) {
        this.legacyWritingApiEnabled = legacyWritingApiEnabled;
    }

    public boolean isChapterWorkspacePrimary() {
        return chapterWorkspacePrimary;
    }

    public void setChapterWorkspacePrimary(boolean chapterWorkspacePrimary) {
        this.chapterWorkspacePrimary = chapterWorkspacePrimary;
    }

    public boolean isStateCenterPrimary() {
        return stateCenterPrimary;
    }

    public void setStateCenterPrimary(boolean stateCenterPrimary) {
        this.stateCenterPrimary = stateCenterPrimary;
    }

    public boolean isGenerationCenterPrimary() {
        return generationCenterPrimary;
    }

    public void setGenerationCenterPrimary(boolean generationCenterPrimary) {
        this.generationCenterPrimary = generationCenterPrimary;
    }

    public boolean isStoryContextDualReadEnabled() {
        return storyContextDualReadEnabled;
    }

    public void setStoryContextDualReadEnabled(boolean storyContextDualReadEnabled) {
        this.storyContextDualReadEnabled = storyContextDualReadEnabled;
    }

    public boolean isSummaryWorkflowDualWriteEnabled() {
        return summaryWorkflowDualWriteEnabled;
    }

    public void setSummaryWorkflowDualWriteEnabled(boolean summaryWorkflowDualWriteEnabled) {
        this.summaryWorkflowDualWriteEnabled = summaryWorkflowDualWriteEnabled;
    }

    public boolean isBackfillExecuteEnabled() {
        return backfillExecuteEnabled;
    }

    public void setBackfillExecuteEnabled(boolean backfillExecuteEnabled) {
        this.backfillExecuteEnabled = backfillExecuteEnabled;
    }

    public boolean isChapterWorkspaceNodePreviewEnabled() {
        return chapterWorkspaceNodePreviewEnabled;
    }

    public void setChapterWorkspaceNodePreviewEnabled(boolean chapterWorkspaceNodePreviewEnabled) {
        this.chapterWorkspaceNodePreviewEnabled = chapterWorkspaceNodePreviewEnabled;
    }

    public boolean isChapterWorkspaceNodeResolveEnabled() {
        return chapterWorkspaceNodeResolveEnabled;
    }

    public void setChapterWorkspaceNodeResolveEnabled(boolean chapterWorkspaceNodeResolveEnabled) {
        this.chapterWorkspaceNodeResolveEnabled = chapterWorkspaceNodeResolveEnabled;
    }
}
