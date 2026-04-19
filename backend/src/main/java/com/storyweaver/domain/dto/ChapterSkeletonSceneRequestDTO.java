package com.storyweaver.domain.dto;

import java.util.List;

public class ChapterSkeletonSceneRequestDTO {

    private String goal;

    private List<String> readerReveal;

    private List<String> mustUseAnchors;

    private String stopCondition;

    private Integer targetWords;

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public List<String> getReaderReveal() {
        return readerReveal;
    }

    public void setReaderReveal(List<String> readerReveal) {
        this.readerReveal = readerReveal;
    }

    public List<String> getMustUseAnchors() {
        return mustUseAnchors;
    }

    public void setMustUseAnchors(List<String> mustUseAnchors) {
        this.mustUseAnchors = mustUseAnchors;
    }

    public String getStopCondition() {
        return stopCondition;
    }

    public void setStopCondition(String stopCondition) {
        this.stopCondition = stopCondition;
    }

    public Integer getTargetWords() {
        return targetWords;
    }

    public void setTargetWords(Integer targetWords) {
        this.targetWords = targetWords;
    }
}
