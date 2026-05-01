package com.storyweaver.story.generation.orchestration;

import lombok.Data;

@Data
public class ChapterSkeletonStreamEvent {

    private String type;

    private String stage;

    private String stageStatus;

    private String message;

    private Long selectedProviderId;

    private String selectedModel;

    private Boolean forceRefresh;

    private ChapterSkeleton skeleton;

    public static ChapterSkeletonStreamEvent meta(Long selectedProviderId, String selectedModel, boolean forceRefresh) {
        ChapterSkeletonStreamEvent event = new ChapterSkeletonStreamEvent();
        event.setType("meta");
        event.setSelectedProviderId(selectedProviderId);
        event.setSelectedModel(selectedModel);
        event.setForceRefresh(forceRefresh);
        return event;
    }

    public static ChapterSkeletonStreamEvent stage(String stage, String stageStatus, String message) {
        ChapterSkeletonStreamEvent event = new ChapterSkeletonStreamEvent();
        event.setType("stage");
        event.setStage(stage);
        event.setStageStatus(stageStatus);
        event.setMessage(message);
        return event;
    }

    public static ChapterSkeletonStreamEvent log(String stage, String message) {
        ChapterSkeletonStreamEvent event = new ChapterSkeletonStreamEvent();
        event.setType("log");
        event.setStage(stage);
        event.setMessage(message);
        return event;
    }

    public static ChapterSkeletonStreamEvent complete(ChapterSkeleton skeleton) {
        ChapterSkeletonStreamEvent event = new ChapterSkeletonStreamEvent();
        event.setType("complete");
        event.setSkeleton(skeleton);
        return event;
    }

    public static ChapterSkeletonStreamEvent error(String message) {
        ChapterSkeletonStreamEvent event = new ChapterSkeletonStreamEvent();
        event.setType("error");
        event.setMessage(message);
        return event;
    }
}
