package com.storyweaver.storyunit.session;

import java.util.List;
import java.util.Objects;

public record WriterExecutionBrief(
        Long projectId,
        Long chapterId,
        String sceneId,
        String chosenCandidateId,
        String goal,
        List<String> readerReveal,
        List<String> mustUseAnchors,
        List<String> forbiddenMoves,
        String stopCondition,
        Integer targetWords,
        List<String> continuityNotes,
        String previousSceneSummary,
        String handoffLine,
        String nextSceneId,
        String nextSceneGoal,
        SceneContinuityState continuityState) {

    public WriterExecutionBrief {
        sceneId = Objects.requireNonNull(sceneId, "sceneId must not be null").trim();
        chosenCandidateId = Objects.requireNonNull(chosenCandidateId, "chosenCandidateId must not be null").trim();
        goal = Objects.requireNonNull(goal, "goal must not be null").trim();
        readerReveal = readerReveal == null ? List.of() : List.copyOf(readerReveal);
        mustUseAnchors = mustUseAnchors == null ? List.of() : List.copyOf(mustUseAnchors);
        forbiddenMoves = forbiddenMoves == null ? List.of() : List.copyOf(forbiddenMoves);
        stopCondition = Objects.requireNonNull(stopCondition, "stopCondition must not be null").trim();
        targetWords = targetWords == null ? null : Math.max(targetWords, 0);
        continuityNotes = continuityNotes == null ? List.of() : List.copyOf(continuityNotes);
        previousSceneSummary = previousSceneSummary == null ? "" : previousSceneSummary.trim();
        handoffLine = handoffLine == null ? "" : handoffLine.trim();
        nextSceneId = nextSceneId == null ? "" : nextSceneId.trim();
        nextSceneGoal = nextSceneGoal == null ? "" : nextSceneGoal.trim();
        continuityState = continuityState == null ? SceneContinuityState.empty() : continuityState;
    }

    public WriterExecutionBrief(
            Long projectId,
            Long chapterId,
            String sceneId,
            String chosenCandidateId,
            String goal,
            List<String> readerReveal,
            List<String> mustUseAnchors,
            List<String> forbiddenMoves,
            String stopCondition,
            Integer targetWords,
            List<String> continuityNotes,
            String previousSceneSummary,
            String handoffLine,
            String nextSceneId,
            String nextSceneGoal) {
        this(
                projectId,
                chapterId,
                sceneId,
                chosenCandidateId,
                goal,
                readerReveal,
                mustUseAnchors,
                forbiddenMoves,
                stopCondition,
                targetWords,
                continuityNotes,
                previousSceneSummary,
                handoffLine,
                nextSceneId,
                nextSceneGoal,
                SceneContinuityState.empty()
        );
    }
}
