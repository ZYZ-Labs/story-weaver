package com.storyweaver.story.generation;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReaderRevealConstraint {

    private Long chapterId;

    private String openingMode;

    private List<String> readerKnownFacts = new ArrayList<>();

    private List<String> revealTargets = new ArrayList<>();

    private List<String> forbiddenAssumptions = new ArrayList<>();

    public void addReaderKnownFact(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        readerKnownFacts.add(value.trim());
    }

    public void addRevealTarget(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        revealTargets.add(value.trim());
    }

    public void addForbiddenAssumption(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        forbiddenAssumptions.add(value.trim());
    }
}
