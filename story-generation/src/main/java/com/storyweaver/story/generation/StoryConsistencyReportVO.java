package com.storyweaver.story.generation;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StoryConsistencyReportVO {

    private Long projectId;

    private String projectName;

    private int chapterCount;

    private int generatedChapterCount;

    private int score;

    private String status;

    private int namingRiskCount;

    private int povIssueCount;

    private int storyBeatIssueCount;

    private int blockedReadinessCount;

    private int directorFallbackCount;

    private double directorFallbackRate;

    private int traceMissingCount;

    private double traceCoverageRate;

    private List<StoryConsistencyIssueVO> issues = new ArrayList<>();

    private List<String> recommendations = new ArrayList<>();

    public void addIssue(StoryConsistencyIssueVO issue) {
        if (issue == null) {
            return;
        }
        issues.add(issue);
    }

    public void addRecommendation(String recommendation) {
        if (recommendation == null || recommendation.isBlank()) {
            return;
        }
        recommendations.add(recommendation.trim());
    }
}
