package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.SceneExecutionWriteResult;
import com.storyweaver.story.generation.orchestration.SceneExecutionWriteService;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.story.generation.orchestration.WriterSessionResult;
import com.storyweaver.storyunit.service.SceneRuntimeStateStore;
import com.storyweaver.storyunit.session.ReviewDecision;
import com.storyweaver.storyunit.session.ReviewResult;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import com.storyweaver.storyunit.session.SceneHandoffSnapshot;
import com.storyweaver.storyunit.session.WriterExecutionBrief;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DefaultSceneExecutionWriteService implements SceneExecutionWriteService {

    private static final int SUMMARY_LIMIT = 160;
    private static final int HANDOFF_LIMIT = 120;

    private final SceneRuntimeStateStore sceneRuntimeStateStore;

    public DefaultSceneExecutionWriteService(SceneRuntimeStateStore sceneRuntimeStateStore) {
        this.sceneRuntimeStateStore = sceneRuntimeStateStore;
    }

    @Override
    public SceneExecutionWriteResult write(
            StorySessionContextPacket contextPacket,
            WriterExecutionBrief writerExecutionBrief,
            WriterSessionResult writerSessionResult,
            ReviewDecision reviewDecision) {
        SceneExecutionState sceneExecutionState = buildSceneExecutionState(
                contextPacket,
                writerExecutionBrief,
                writerSessionResult,
                reviewDecision
        );
        SceneHandoffSnapshot handoffSnapshot = buildHandoffSnapshot(
                contextPacket,
                writerSessionResult,
                reviewDecision,
                sceneExecutionState
        );
        sceneRuntimeStateStore.saveSceneState(sceneExecutionState);
        sceneRuntimeStateStore.saveHandoff(handoffSnapshot);
        return new SceneExecutionWriteResult(sceneExecutionState, handoffSnapshot);
    }

    private SceneExecutionState buildSceneExecutionState(
            StorySessionContextPacket contextPacket,
            WriterExecutionBrief writerExecutionBrief,
            WriterSessionResult writerSessionResult,
            ReviewDecision reviewDecision) {
        String sceneId = sceneIdentity(writerSessionResult.sceneId(), contextPacket.sceneId());
        int sceneIndex = parseSceneIndex(sceneId, contextPacket.existingSceneStates().size() + 1);
        Map<String, Object> stateDelta = new LinkedHashMap<>();
        stateDelta.put("source", "phase6.runtime-store");
        stateDelta.put("candidateId", writerSessionResult.candidateId());
        stateDelta.put("reviewResult", reviewDecision.result().name());
        stateDelta.put("targetWords", writerExecutionBrief.targetWords());
        stateDelta.put("createdAt", LocalDateTime.now().toString());

        return new SceneExecutionState(
                contextPacket.projectId(),
                contextPacket.chapterId(),
                sceneId,
                sceneIndex,
                mapStatus(reviewDecision),
                writerSessionResult.candidateId(),
                writerExecutionBrief.goal(),
                writerExecutionBrief.stopCondition(),
                writerExecutionBrief.readerReveal(),
                List.of(),
                List.of(),
                Map.copyOf(stateDelta),
                resolveHandoffLine(writerSessionResult, writerExecutionBrief),
                truncate(firstNonBlank(writerSessionResult.summary(), writerSessionResult.draftText()), SUMMARY_LIMIT)
        );
    }

    private SceneHandoffSnapshot buildHandoffSnapshot(
            StorySessionContextPacket contextPacket,
            WriterSessionResult writerSessionResult,
            ReviewDecision reviewDecision,
            SceneExecutionState sceneExecutionState) {
        String nextSceneId = "scene-" + (sceneExecutionState.sceneIndex() + 1);
        return new SceneHandoffSnapshot(
                contextPacket.projectId(),
                contextPacket.chapterId(),
                sceneExecutionState.sceneId(),
                nextSceneId,
                sceneExecutionState.handoffLine(),
                sceneExecutionState.outcomeSummary(),
                sceneExecutionState.readerRevealDelta(),
                sceneExecutionState.openLoops(),
                sceneExecutionState.resolvedLoops(),
                sceneExecutionState.stateDelta(),
                reviewDecision.result().name(),
                reviewDecision.summary(),
                LocalDateTime.now()
        );
    }

    private SceneExecutionStatus mapStatus(ReviewDecision reviewDecision) {
        if (reviewDecision.result() == ReviewResult.PASS) {
            return SceneExecutionStatus.COMPLETED;
        }
        if (reviewDecision.canAutoRepair()) {
            return SceneExecutionStatus.REVIEWING;
        }
        return SceneExecutionStatus.FAILED;
    }

    private String resolveHandoffLine(WriterSessionResult writerSessionResult, WriterExecutionBrief writerExecutionBrief) {
        String draftText = truncate(writerSessionResult.draftText(), HANDOFF_LIMIT);
        if (StringUtils.hasText(draftText)) {
            return draftText;
        }
        return truncate(writerExecutionBrief.handoffLine(), HANDOFF_LIMIT);
    }

    private String sceneIdentity(String writerSceneId, String contextSceneId) {
        if (StringUtils.hasText(writerSceneId)) {
            return writerSceneId.trim();
        }
        if (StringUtils.hasText(contextSceneId)) {
            return contextSceneId.trim();
        }
        return "scene-1";
    }

    private int parseSceneIndex(String sceneId, int fallback) {
        if (!StringUtils.hasText(sceneId)) {
            return Math.max(1, fallback);
        }
        if (!sceneId.startsWith("scene-")) {
            return Math.max(1, fallback);
        }
        try {
            return Math.max(1, Integer.parseInt(sceneId.substring("scene-".length())));
        } catch (NumberFormatException exception) {
            return Math.max(1, fallback);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String truncate(String value, int limit) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= limit ? normalized : normalized.substring(0, limit) + "...";
    }
}
