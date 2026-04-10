package com.storyweaver.story.generation;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StoryConsistencyIssueVO {

    private String severity;

    private String type;

    private Long chapterId;

    private String chapterTitle;

    private String message;

    private List<String> evidence = new ArrayList<>();

    public void addEvidence(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        evidence.add(value.trim());
    }
}
