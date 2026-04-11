package com.storyweaver.storyunit.event;

import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.model.StoryUnitRef;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record StoryEvent(
        String eventId,
        StoryEventType eventType,
        Long projectId,
        Long chapterId,
        String sceneId,
        StoryUnitRef primaryUnit,
        String summary,
        Map<String, Object> payload,
        StorySourceTrace sourceTrace) {

    public StoryEvent {
        eventId = Objects.requireNonNull(eventId, "eventId must not be null").trim();
        eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        sceneId = sceneId == null ? null : sceneId.trim();
        summary = summary == null ? "" : summary.trim();
        payload = payload == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(payload));
    }
}
