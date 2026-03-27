package com.storyweaver.domain.vo;

import lombok.Data;

import java.util.List;
import java.util.stream.Stream;

@Data
public class AIWritingChatParticipationVO {

    private List<String> worldFacts = List.of();

    private List<String> characterConstraints = List.of();

    private List<String> plotGuidance = List.of();

    private List<String> writingPreferences = List.of();

    private List<String> hardConstraints = List.of();

    public static AIWritingChatParticipationVO empty() {
        return new AIWritingChatParticipationVO();
    }

    public boolean hasContent() {
        return Stream.of(worldFacts, characterConstraints, plotGuidance, writingPreferences, hardConstraints)
                .anyMatch(items -> items != null && !items.isEmpty());
    }
}
