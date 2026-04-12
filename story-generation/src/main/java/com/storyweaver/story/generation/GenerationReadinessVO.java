package com.storyweaver.story.generation;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GenerationReadinessVO {

    private int score;

    private String status;

    private List<String> blockingIssues = new ArrayList<>();

    private List<String> warnings = new ArrayList<>();

    private ChapterAnchorBundle resolvedAnchors;

    private List<String> recommendedModules = new ArrayList<>();

    public void addBlockingIssue(String issue) {
        if (issue == null || issue.isBlank()) {
            return;
        }
        blockingIssues.add(issue.trim());
    }

    public void addWarning(String warning) {
        if (warning == null || warning.isBlank()) {
            return;
        }
        warnings.add(warning.trim());
    }

    public void addRecommendedModule(String module) {
        if (module == null || module.isBlank()) {
            return;
        }
        recommendedModules.add(module.trim());
    }

    public boolean isBlocked() {
        return blockingIssues != null && !blockingIssues.isEmpty();
    }
}
