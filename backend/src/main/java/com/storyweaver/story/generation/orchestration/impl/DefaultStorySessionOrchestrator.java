package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.DirectorSessionService;
import com.storyweaver.story.generation.orchestration.SceneBindingMode;
import com.storyweaver.story.generation.orchestration.SceneExecutionRequest;
import com.storyweaver.story.generation.orchestration.SceneExecutionWriteResult;
import com.storyweaver.story.generation.orchestration.SceneExecutionWriteService;
import com.storyweaver.story.generation.orchestration.SessionExecutionTrace;
import com.storyweaver.story.generation.orchestration.SessionExecutionTraceItem;
import com.storyweaver.story.generation.orchestration.SessionTraceStatus;
import com.storyweaver.story.generation.orchestration.SelectorSessionService;
import com.storyweaver.story.generation.orchestration.StorySessionExecution;
import com.storyweaver.story.generation.orchestration.StorySessionContextAssembler;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.story.generation.orchestration.StorySessionOrchestrator;
import com.storyweaver.story.generation.orchestration.StorySessionPreview;
import com.storyweaver.story.generation.orchestration.ReviewerSessionService;
import com.storyweaver.story.generation.orchestration.WriterSessionResult;
import com.storyweaver.story.generation.orchestration.WriterSessionService;
import com.storyweaver.story.generation.orchestration.WriterExecutionBriefBuilder;
import com.storyweaver.storyunit.session.DirectorCandidate;
import com.storyweaver.storyunit.session.ReviewDecision;
import com.storyweaver.storyunit.session.SelectionDecision;
import com.storyweaver.storyunit.session.SessionRole;
import com.storyweaver.storyunit.session.WriterExecutionBrief;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DefaultStorySessionOrchestrator implements StorySessionOrchestrator {

    private final StorySessionContextAssembler storySessionContextAssembler;
    private final DirectorSessionService directorSessionService;
    private final SelectorSessionService selectorSessionService;
    private final WriterExecutionBriefBuilder writerExecutionBriefBuilder;
    private final WriterSessionService writerSessionService;
    private final ReviewerSessionService reviewerSessionService;
    private final SceneExecutionWriteService sceneExecutionWriteService;

    public DefaultStorySessionOrchestrator(
            StorySessionContextAssembler storySessionContextAssembler,
            DirectorSessionService directorSessionService,
            SelectorSessionService selectorSessionService,
            WriterExecutionBriefBuilder writerExecutionBriefBuilder,
            WriterSessionService writerSessionService,
            ReviewerSessionService reviewerSessionService,
            SceneExecutionWriteService sceneExecutionWriteService) {
        this.storySessionContextAssembler = storySessionContextAssembler;
        this.directorSessionService = directorSessionService;
        this.selectorSessionService = selectorSessionService;
        this.writerExecutionBriefBuilder = writerExecutionBriefBuilder;
        this.writerSessionService = writerSessionService;
        this.reviewerSessionService = reviewerSessionService;
        this.sceneExecutionWriteService = sceneExecutionWriteService;
    }

    @Override
    public Optional<StorySessionContextPacket> prepareContext(Long projectId, Long chapterId, String sceneId) {
        return storySessionContextAssembler.assemble(projectId, chapterId, sceneId);
    }

    @Override
    public SessionExecutionTrace initializeTrace(StorySessionContextPacket contextPacket) {
        return new SessionExecutionTrace(
                contextPacket.projectId(),
                contextPacket.chapterId(),
                contextPacket.sceneId(),
                java.util.List.of()
        );
    }

    @Override
    public Optional<StorySessionPreview> preview(Long projectId, Long chapterId, String sceneId) {
        return buildPreview(projectId, chapterId, sceneId);
    }

    @Override
    public Optional<StorySessionExecution> execute(SceneExecutionRequest request) {
        return buildPreview(request.projectId(), request.chapterId(), request.sceneId())
                .map(preview -> {
                    SceneExecutionWriteResult writeResult = sceneExecutionWriteService.write(
                            preview.contextPacket(),
                            preview.writerExecutionBrief(),
                            preview.writerSessionResult(),
                            preview.reviewDecision()
                    );
                    SessionExecutionTrace trace = preview.trace().append(new SessionExecutionTraceItem(
                            SessionRole.ORCHESTRATOR,
                            "scene-writeback",
                            SessionTraceStatus.COMPLETED,
                            "已写回 scene runtime state 与 handoff 快照。",
                            "sceneExecutionWriteResult",
                            1,
                            false,
                            traceDetails(
                                    "sceneId", writeResult.sceneExecutionState().sceneId(),
                                    "status", writeResult.sceneExecutionState().status().name(),
                                    "nextSceneId", writeResult.handoffSnapshot().toSceneId()
                            )
                    ));
                    return new StorySessionExecution(preview, writeResult, trace);
                });
    }

    private Optional<StorySessionPreview> buildPreview(Long projectId, Long chapterId, String sceneId) {
        Optional<StorySessionContextPacket> contextPacket = prepareContext(projectId, chapterId, sceneId);
        if (contextPacket.isEmpty()) {
            return Optional.empty();
        }

        List<DirectorCandidate> candidates = directorSessionService.proposeCandidates(contextPacket.get());
        SelectionDecision selectionDecision = selectorSessionService.selectCandidate(contextPacket.get(), candidates);
        DirectorCandidate chosenCandidate = candidates.stream()
                .filter(candidate -> candidate.candidateId().equals(selectionDecision.chosenCandidateId()))
                .findFirst()
                .orElse(candidates.getFirst());
        WriterExecutionBrief writerExecutionBrief = writerExecutionBriefBuilder.build(contextPacket.get(), chosenCandidate);
        WriterSessionResult writerSessionResult = writerSessionService.write(contextPacket.get(), writerExecutionBrief);
        ReviewDecision reviewDecision = reviewerSessionService.review(contextPacket.get(), writerSessionResult);

        SessionExecutionTrace trace = initializeTrace(contextPacket.get())
                .append(new SessionExecutionTraceItem(
                        SessionRole.ORCHESTRATOR,
                        "context-scene-binding",
                        bindingStatus(contextPacket.get().sceneBindingContext().mode()),
                        contextPacket.get().sceneBindingContext().summary(),
                        "sceneBindingContext",
                        1,
                        contextPacket.get().sceneBindingContext().mode() == SceneBindingMode.SCENE_FALLBACK_TO_LATEST,
                        traceDetails(
                                "requestedSceneId", contextPacket.get().sceneBindingContext().requestedSceneId(),
                                "resolvedSceneId", contextPacket.get().sceneBindingContext().resolvedSceneId(),
                                "bindingMode", contextPacket.get().sceneBindingContext().mode().name(),
                                "fallbackUsed", contextPacket.get().sceneBindingContext().fallbackUsed()
                        )
                ))
                .append(new SessionExecutionTraceItem(
                        SessionRole.DIRECTOR,
                        "director-candidates",
                        SessionTraceStatus.COMPLETED,
                        "已生成 " + candidates.size() + " 个候选。",
                        "directorCandidates",
                        1,
                        false,
                        traceDetails("candidateCount", candidates.size())
                ))
                .append(new SessionExecutionTraceItem(
                        SessionRole.SELECTOR,
                        "selector-decision",
                        SessionTraceStatus.COMPLETED,
                        selectionDecision.whyChosen(),
                        "selectionDecision",
                        1,
                        false,
                        traceDetails(
                                "chosenCandidateId", selectionDecision.chosenCandidateId(),
                                "riskCount", selectionDecision.risks().size()
                        )
                ))
                .append(new SessionExecutionTraceItem(
                        SessionRole.WRITER,
                        "writer-brief",
                        SessionTraceStatus.COMPLETED,
                        "已基于已选候选生成 writer brief。",
                        "writerExecutionBrief",
                        1,
                        false,
                        traceDetails(
                                "candidateId", writerExecutionBrief.chosenCandidateId(),
                                "targetWords", writerExecutionBrief.targetWords(),
                                "continuityNoteCount", writerExecutionBrief.continuityNotes().size()
                        )
                ))
                .append(new SessionExecutionTraceItem(
                        SessionRole.WRITER,
                        "writer-result",
                        SessionTraceStatus.COMPLETED,
                        writerSessionResult.summary(),
                        "writerSessionResult",
                        1,
                        false,
                        traceDetails(
                                "sceneId", writerSessionResult.sceneId(),
                                "candidateId", writerSessionResult.candidateId()
                        )
                ))
                .append(new SessionExecutionTraceItem(
                        SessionRole.REVIEWER,
                        "reviewer-decision",
                        reviewDecision.result() == com.storyweaver.storyunit.session.ReviewResult.PASS
                                ? SessionTraceStatus.COMPLETED
                                : SessionTraceStatus.FAILED,
                        reviewDecision.summary(),
                        "reviewDecision",
                        1,
                        reviewDecision.canAutoRepair(),
                        traceDetails(
                                "reviewResult", reviewDecision.result().name(),
                                "issueCount", reviewDecision.issues().size(),
                                "canAutoRepair", reviewDecision.canAutoRepair()
                        )
                ));

        return Optional.of(new StorySessionPreview(
                contextPacket.get(),
                candidates,
                selectionDecision,
                writerExecutionBrief,
                writerSessionResult,
                reviewDecision,
                trace
        ));
    }

    private SessionTraceStatus bindingStatus(SceneBindingMode mode) {
        return switch (mode) {
            case SCENE_BOUND, CHAPTER_COLD_START -> SessionTraceStatus.COMPLETED;
            case SCENE_FALLBACK_TO_LATEST, SCENE_QUERY_UNAVAILABLE -> SessionTraceStatus.SKIPPED;
        };
    }

    private Map<String, Object> traceDetails(Object... values) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            Object key = values[i];
            if (key != null) {
                details.put(String.valueOf(key), values[i + 1]);
            }
        }
        return Map.copyOf(details);
    }
}
