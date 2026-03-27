package com.storyweaver.domain.vo;

import lombok.Data;

@Data
public class AIWritingStreamEventVO {

    private String type;

    private String delta;

    private String content;

    private String message;

    private String stage;

    private String stageStatus;

    private String writingType;

    private Long selectedProviderId;

    private String selectedModel;

    private Integer maxTokens;

    private AIWritingResponseVO record;

    public static AIWritingStreamEventVO meta(
            String writingType,
            Long selectedProviderId,
            String selectedModel,
            Integer maxTokens) {
        AIWritingStreamEventVO event = new AIWritingStreamEventVO();
        event.setType("meta");
        event.setWritingType(writingType);
        event.setSelectedProviderId(selectedProviderId);
        event.setSelectedModel(selectedModel);
        event.setMaxTokens(maxTokens);
        return event;
    }

    public static AIWritingStreamEventVO chunk(String delta) {
        AIWritingStreamEventVO event = new AIWritingStreamEventVO();
        event.setType("chunk");
        event.setDelta(delta);
        return event;
    }

    public static AIWritingStreamEventVO complete(AIWritingResponseVO record) {
        AIWritingStreamEventVO event = new AIWritingStreamEventVO();
        event.setType("complete");
        event.setRecord(record);
        return event;
    }

    public static AIWritingStreamEventVO stage(String stage, String stageStatus, String message) {
        AIWritingStreamEventVO event = new AIWritingStreamEventVO();
        event.setType("stage");
        event.setStage(stage);
        event.setStageStatus(stageStatus);
        event.setMessage(message);
        return event;
    }

    public static AIWritingStreamEventVO log(String stage, String message) {
        AIWritingStreamEventVO event = new AIWritingStreamEventVO();
        event.setType("log");
        event.setStage(stage);
        event.setMessage(message);
        return event;
    }

    public static AIWritingStreamEventVO replace(String content) {
        AIWritingStreamEventVO event = new AIWritingStreamEventVO();
        event.setType("replace");
        event.setContent(content);
        return event;
    }

    public static AIWritingStreamEventVO error(String message) {
        AIWritingStreamEventVO event = new AIWritingStreamEventVO();
        event.setType("error");
        event.setMessage(message);
        return event;
    }
}
