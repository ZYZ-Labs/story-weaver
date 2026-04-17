package com.storyweaver.storyunit.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.storyweaver.domain.entity.AIWritingRecord;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.repository.AIWritingRecordMapper;
import com.storyweaver.service.ChapterService;
import com.storyweaver.storyunit.service.SceneExecutionStateQueryService;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class DefaultSceneExecutionStateQueryService implements SceneExecutionStateQueryService {

    private static final int SUMMARY_LIMIT = 120;
    private static final int HANDOFF_LIMIT = 80;

    private final AIWritingRecordMapper aiWritingRecordMapper;
    private final ChapterService chapterService;
    private final ObjectMapper objectMapper;

    public DefaultSceneExecutionStateQueryService(
            AIWritingRecordMapper aiWritingRecordMapper,
            ChapterService chapterService,
            ObjectMapper objectMapper) {
        this.aiWritingRecordMapper = aiWritingRecordMapper;
        this.chapterService = chapterService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<SceneExecutionState> getSceneState(Long projectId, Long chapterId, String sceneId) {
        if (!StringUtils.hasText(sceneId)) {
            return Optional.empty();
        }
        return listChapterScenes(projectId, chapterId).stream()
                .filter(scene -> scene.sceneId().equals(sceneId.trim()))
                .findFirst();
    }

    @Override
    public List<SceneExecutionState> listChapterScenes(Long projectId, Long chapterId) {
        if (!belongsToProject(projectId, chapterId)) {
            return List.of();
        }

        List<AIWritingRecord> records = aiWritingRecordMapper.findByChapterId(chapterId);
        if (records == null || records.isEmpty()) {
            return List.of();
        }

        List<SceneExecutionState> states = new ArrayList<>();
        int sceneIndex = 1;
        for (AIWritingRecord record : records) {
            if (!StringUtils.hasText(record.getGeneratedContent())) {
                continue;
            }
            states.add(toSceneExecutionState(record, projectId, chapterId, sceneIndex));
            sceneIndex++;
        }
        return List.copyOf(states);
    }

    @Override
    public Optional<SceneExecutionState> findLatestChapterScene(Long projectId, Long chapterId) {
        List<SceneExecutionState> scenes = listChapterScenes(projectId, chapterId);
        if (scenes.isEmpty()) {
            return Optional.empty();
        }
        for (int index = scenes.size() - 1; index >= 0; index--) {
            SceneExecutionState state = scenes.get(index);
            if (state.status() != SceneExecutionStatus.FAILED && state.status() != SceneExecutionStatus.BLOCKED) {
                return Optional.of(state);
            }
        }
        return Optional.of(scenes.getLast());
    }

    private boolean belongsToProject(Long projectId, Long chapterId) {
        if (projectId == null || chapterId == null) {
            return false;
        }
        Chapter chapter = chapterService.getById(chapterId);
        return chapter != null && projectId.equals(chapter.getProjectId());
    }

    private SceneExecutionState toSceneExecutionState(
            AIWritingRecord record,
            Long projectId,
            Long chapterId,
            int sceneIndex) {
        JsonNode traceRoot = readJson(record.getGenerationTraceJson());
        List<String> readerRevealDelta = readStringArray(traceRoot.path("readerReveal").path("revealTargets"));
        List<String> openLoops = readStringArray(traceRoot.path("openLoops"));
        List<String> resolvedLoops = readStringArray(traceRoot.path("resolvedLoops"));

        Map<String, Object> stateDelta = new LinkedHashMap<>();
        stateDelta.put("source", "aiWritingRecord");
        stateDelta.put("recordId", record.getId());
        stateDelta.put("recordStatus", safe(record.getStatus()));
        stateDelta.put("writingType", safe(record.getWritingType()));
        stateDelta.put("selectedModel", safe(record.getSelectedModel()));
        LocalDateTime createTime = record.getCreateTime();
        if (createTime != null) {
            stateDelta.put("createTime", createTime.toString());
        }
        String directorMode = text(traceRoot.path("director").path("mode"));
        if (StringUtils.hasText(directorMode)) {
            stateDelta.put("directorMode", directorMode);
        }

        return new SceneExecutionState(
                projectId,
                chapterId,
                "scene-" + sceneIndex,
                sceneIndex,
                mapStatus(record),
                resolveChosenCandidateId(record, traceRoot),
                resolveGoal(record, traceRoot),
                resolveStopCondition(traceRoot),
                readerRevealDelta,
                openLoops,
                resolvedLoops,
                Map.copyOf(stateDelta),
                resolveHandoffLine(record.getGeneratedContent()),
                resolveOutcomeSummary(record, traceRoot)
        );
    }

    private SceneExecutionStatus mapStatus(AIWritingRecord record) {
        String status = safe(record.getStatus()).toLowerCase(Locale.ROOT);
        if ("accepted".equals(status) || "completed".equals(status) || "success".equals(status)) {
            return SceneExecutionStatus.COMPLETED;
        }
        if ("rejected".equals(status) || "failed".equals(status) || "error".equals(status)) {
            return SceneExecutionStatus.FAILED;
        }
        if ("blocked".equals(status)) {
            return SceneExecutionStatus.BLOCKED;
        }
        if ("reviewing".equals(status)) {
            return SceneExecutionStatus.REVIEWING;
        }
        if ("writing".equals(status) || "generating".equals(status) || "pending".equals(status)) {
            return SceneExecutionStatus.WRITING;
        }
        return StringUtils.hasText(record.getGeneratedContent()) ? SceneExecutionStatus.WRITTEN : SceneExecutionStatus.PLANNED;
    }

    private String resolveChosenCandidateId(AIWritingRecord record, JsonNode traceRoot) {
        String candidateId = text(traceRoot.path("orchestration").path("selectionDecision").path("chosenCandidateId"));
        if (StringUtils.hasText(candidateId)) {
            return candidateId;
        }
        String directorMode = text(traceRoot.path("director").path("mode"));
        if (StringUtils.hasText(directorMode)) {
            return "legacy-" + directorMode.trim().toLowerCase(Locale.ROOT);
        }
        return safe(record.getWritingType());
    }

    private String resolveGoal(AIWritingRecord record, JsonNode traceRoot) {
        String goal = text(traceRoot.path("orchestration").path("writerExecutionBrief").path("goal"));
        if (StringUtils.hasText(goal)) {
            return truncate(goal, SUMMARY_LIMIT);
        }
        String decisionSummary = text(traceRoot.path("director").path("decisionSummary"));
        if (StringUtils.hasText(decisionSummary)) {
            return truncate(decisionSummary, SUMMARY_LIMIT);
        }
        String userInstruction = safe(record.getUserInstruction());
        if (StringUtils.hasText(userInstruction)) {
            return truncate(userInstruction, SUMMARY_LIMIT);
        }
        String chapterSummary = text(traceRoot.path("anchors").path("chapterSummary"));
        if (StringUtils.hasText(chapterSummary)) {
            return truncate(chapterSummary, SUMMARY_LIMIT);
        }
        return truncate(record.getGeneratedContent(), SUMMARY_LIMIT);
    }

    private String resolveStopCondition(JsonNode traceRoot) {
        String stopCondition = text(traceRoot.path("orchestration").path("writerExecutionBrief").path("stopCondition"));
        if (StringUtils.hasText(stopCondition)) {
            return truncate(stopCondition, SUMMARY_LIMIT);
        }
        return "";
    }

    private String resolveOutcomeSummary(AIWritingRecord record, JsonNode traceRoot) {
        String generatedContent = truncate(record.getGeneratedContent(), SUMMARY_LIMIT);
        if (StringUtils.hasText(generatedContent)) {
            return generatedContent;
        }
        String summary = text(traceRoot.path("summaryTrace").path("userInstructionPreview"));
        if (StringUtils.hasText(summary)) {
            return truncate(summary, SUMMARY_LIMIT);
        }
        return truncate(safe(record.getUserInstruction()), SUMMARY_LIMIT);
    }

    private String resolveHandoffLine(String generatedContent) {
        String normalized = normalizeWhitespace(generatedContent);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        String[] segments = normalized.split("(?<=[。！？!?])");
        for (int index = segments.length - 1; index >= 0; index--) {
            String candidate = segments[index].trim();
            if (StringUtils.hasText(candidate)) {
                return truncate(candidate, HANDOFF_LIMIT);
            }
        }
        return truncate(normalized, HANDOFF_LIMIT);
    }

    private JsonNode readJson(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return NullNode.getInstance();
        }
        try {
            return objectMapper.readTree(rawJson);
        } catch (Exception exception) {
            return NullNode.getInstance();
        }
    }

    private List<String> readStringArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        arrayNode.forEach(node -> {
            String value = text(node);
            if (StringUtils.hasText(value)) {
                result.add(value.trim());
            }
        });
        return List.copyOf(result);
    }

    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        return safe(node.asText());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeWhitespace(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String truncate(String value, int limit) {
        String normalized = normalizeWhitespace(value);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        return normalized.length() <= limit ? normalized : normalized.substring(0, limit) + "...";
    }
}
