package com.storyweaver.domain.vo;

import lombok.Data;

@Data
public class AIWritingStreamEventVO {

    private String type;

    private String delta;

    private String message;

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

    public static AIWritingStreamEventVO error(String message) {
        AIWritingStreamEventVO event = new AIWritingStreamEventVO();
        event.setType("error");
        event.setMessage(message);
        return event;
    }
}
