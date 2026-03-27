package com.storyweaver.domain.vo;

import lombok.Data;

@Data
public class AIWritingChatStreamEventVO {

    private String type;

    private String delta;

    private String message;

    private Long selectedProviderId;

    private String selectedModel;

    private AIWritingChatSessionVO session;

    public static AIWritingChatStreamEventVO meta(Long selectedProviderId, String selectedModel) {
        AIWritingChatStreamEventVO event = new AIWritingChatStreamEventVO();
        event.setType("meta");
        event.setSelectedProviderId(selectedProviderId);
        event.setSelectedModel(selectedModel);
        return event;
    }

    public static AIWritingChatStreamEventVO chunk(String delta) {
        AIWritingChatStreamEventVO event = new AIWritingChatStreamEventVO();
        event.setType("chunk");
        event.setDelta(delta);
        return event;
    }

    public static AIWritingChatStreamEventVO complete(AIWritingChatSessionVO session) {
        AIWritingChatStreamEventVO event = new AIWritingChatStreamEventVO();
        event.setType("complete");
        event.setSession(session);
        return event;
    }

    public static AIWritingChatStreamEventVO error(String message) {
        AIWritingChatStreamEventVO event = new AIWritingChatStreamEventVO();
        event.setType("error");
        event.setMessage(message);
        return event;
    }
}
