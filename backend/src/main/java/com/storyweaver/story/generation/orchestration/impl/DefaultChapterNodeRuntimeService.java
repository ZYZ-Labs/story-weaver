package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.ChapterNodeRuntimeService;
import com.storyweaver.story.generation.orchestration.ChapterNodeRuntimeView;
import com.storyweaver.story.generation.orchestration.ChapterNodeSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonPlanner;
import com.storyweaver.story.generation.orchestration.NodeActionOption;
import com.storyweaver.story.generation.orchestration.NodeActionRequest;
import com.storyweaver.story.generation.orchestration.NodeResolutionResult;
import com.storyweaver.story.generation.orchestration.StoryNodeSkeletonItem;
import com.storyweaver.story.generation.orchestration.StorySessionContextAssembler;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.storyunit.event.StoryEvent;
import com.storyweaver.storyunit.event.StoryEventType;
import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
import com.storyweaver.storyunit.facet.state.ChapterIncrementalState;
import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.runtime.StoryActionIntent;
import com.storyweaver.storyunit.runtime.StoryLoopStatus;
import com.storyweaver.storyunit.runtime.StoryNodeCheckpoint;
import com.storyweaver.storyunit.runtime.StoryOpenLoop;
import com.storyweaver.storyunit.runtime.StoryResolvedTurn;
import com.storyweaver.storyunit.service.ChapterIncrementalStateStore;
import com.storyweaver.storyunit.service.ReaderRevealStateStore;
import com.storyweaver.storyunit.service.StoryActionIntentStore;
import com.storyweaver.storyunit.service.StoryEventStore;
import com.storyweaver.storyunit.service.StoryNodeCheckpointStore;
import com.storyweaver.storyunit.service.StoryOpenLoopStore;
import com.storyweaver.storyunit.service.StoryResolvedTurnStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DefaultChapterNodeRuntimeService implements ChapterNodeRuntimeService {

    private static final String OPTION_ADVANCE = "advance-goal";
    private static final String OPTION_PROBE = "probe-info";
    private static final String OPTION_HOLD = "hold-line";

    private final ChapterSkeletonPlanner chapterSkeletonPlanner;
    private final StorySessionContextAssembler storySessionContextAssembler;
    private final StoryActionIntentStore storyActionIntentStore;
    private final StoryResolvedTurnStore storyResolvedTurnStore;
    private final StoryNodeCheckpointStore storyNodeCheckpointStore;
    private final StoryOpenLoopStore storyOpenLoopStore;
    private final StoryEventStore storyEventStore;
    private final ReaderRevealStateStore readerRevealStateStore;
    private final ChapterIncrementalStateStore chapterIncrementalStateStore;

    public DefaultChapterNodeRuntimeService(
            ChapterSkeletonPlanner chapterSkeletonPlanner,
            StorySessionContextAssembler storySessionContextAssembler,
            StoryActionIntentStore storyActionIntentStore,
            StoryResolvedTurnStore storyResolvedTurnStore,
            StoryNodeCheckpointStore storyNodeCheckpointStore,
            StoryOpenLoopStore storyOpenLoopStore,
            StoryEventStore storyEventStore,
            ReaderRevealStateStore readerRevealStateStore,
            ChapterIncrementalStateStore chapterIncrementalStateStore) {
        this.chapterSkeletonPlanner = chapterSkeletonPlanner;
        this.storySessionContextAssembler = storySessionContextAssembler;
        this.storyActionIntentStore = storyActionIntentStore;
        this.storyResolvedTurnStore = storyResolvedTurnStore;
        this.storyNodeCheckpointStore = storyNodeCheckpointStore;
        this.storyOpenLoopStore = storyOpenLoopStore;
        this.storyEventStore = storyEventStore;
        this.readerRevealStateStore = readerRevealStateStore;
        this.chapterIncrementalStateStore = chapterIncrementalStateStore;
    }

    @Override
    public Optional<ChapterNodeRuntimeView> preview(Long projectId, Long chapterId) {
        Optional<ChapterSkeleton> sceneSkeleton = chapterSkeletonPlanner.plan(projectId, chapterId);
        if (sceneSkeleton.isEmpty()) {
            return Optional.empty();
        }
        StorySessionContextPacket context = storySessionContextAssembler.assemble(projectId, chapterId, "scene-1").orElse(null);
        ChapterNodeSkeleton nodeSkeleton = toNodeSkeleton(sceneSkeleton.get(), context);
        List<StoryResolvedTurn> turns = storyResolvedTurnStore.listChapterTurns(projectId, chapterId);
        List<StoryNodeCheckpoint> checkpoints = storyNodeCheckpointStore.listChapterCheckpoints(projectId, chapterId);
        List<StoryOpenLoop> activeLoops = storyOpenLoopStore.listChapterLoops(projectId, chapterId).stream()
                .filter(loop -> loop.status() != StoryLoopStatus.RESOLVED)
                .toList();
        int completedCount = Math.min(turns.size(), nodeSkeleton.nodes().size());
        List<String> completedNodeIds = nodeSkeleton.nodes().stream()
                .limit(completedCount)
                .map(StoryNodeSkeletonItem::nodeId)
                .toList();
        String currentNodeId = completedCount < nodeSkeleton.nodes().size()
                ? nodeSkeleton.nodes().get(completedCount).nodeId()
                : "";
        String latestCheckpointId = checkpoints.isEmpty() ? "" : checkpoints.get(checkpoints.size() - 1).checkpointId();
        return Optional.of(new ChapterNodeRuntimeView(
                projectId,
                chapterId,
                nodeSkeleton,
                currentNodeId,
                latestCheckpointId,
                completedNodeIds,
                checkpoints,
                activeLoops
        ));
    }

    @Override
    public Optional<NodeResolutionResult> resolve(NodeActionRequest request) {
        Optional<ChapterNodeRuntimeView> preview = preview(request.projectId(), request.chapterId());
        if (preview.isEmpty()) {
            return Optional.empty();
        }

        ChapterNodeRuntimeView runtimeView = preview.get();
        if (!StringUtils.hasText(runtimeView.currentNodeId())) {
            throw new IllegalStateException("当前章节的节点链已全部完成，不能继续推进。");
        }

        String requestedNodeId = StringUtils.hasText(request.nodeId()) ? request.nodeId().trim() : runtimeView.currentNodeId();
        if (!runtimeView.currentNodeId().equals(requestedNodeId)) {
            throw new IllegalStateException("当前只允许推进节点 " + runtimeView.currentNodeId() + "。");
        }

        String latestCheckpointId = runtimeView.latestCheckpointId();
        if (StringUtils.hasText(request.checkpointId())
                && StringUtils.hasText(latestCheckpointId)
                && !latestCheckpointId.equals(request.checkpointId().trim())) {
            throw new IllegalStateException("checkpoint 已变化，请刷新后基于最新存档继续。");
        }

        StoryNodeSkeletonItem node = runtimeView.skeleton().nodes().stream()
                .filter(candidate -> candidate.nodeId().equals(requestedNodeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("节点不存在: " + requestedNodeId));
        NodeActionOption selectedOption = resolveSelectedOption(node, request.selectedOptionId());
        String customAction = normalizeText(request.customAction());
        if (selectedOption == null && customAction.isEmpty()) {
            throw new IllegalArgumentException("必须选择一个推荐动作，或提供自定义动作。");
        }

        StorySessionContextPacket context = storySessionContextAssembler.assemble(request.projectId(), request.chapterId(), "scene-1").orElse(null);
        String actorName = resolveActorName(context);
        String rawAction = !customAction.isEmpty() ? customAction : selectedOption.label();
        String normalizedAction = !customAction.isEmpty()
                ? customAction
                : firstNonBlank(selectedOption.intentSummary(), node.playerGoal(), node.situation());
        StorySourceTrace sourceTrace = sourceTrace("NodeRuntimeService", requestedNodeId);

        StoryActionIntent intent = storyActionIntentStore.recordIntent(new StoryActionIntent(
                "intent-" + UUID.randomUUID(),
                request.projectId(),
                request.chapterId(),
                latestCheckpointId,
                requestedNodeId,
                actorName,
                "player",
                selectedOption == null ? null : selectedOption.optionId(),
                rawAction,
                normalizedAction,
                Map.of("mode", selectedOption == null ? "custom" : "recommended"),
                sourceTrace
        ));

        StoryEvent intentEvent = storyEventStore.appendEvent(new StoryEvent(
                "event-" + UUID.randomUUID(),
                StoryEventType.ACTION_INTENT_RECORDED,
                request.projectId(),
                request.chapterId(),
                requestedNodeId,
                new StoryUnitRef(requestedNodeId, "node:" + request.chapterId() + ":" + requestedNodeId, StoryUnitType.SCENE_EXECUTION),
                actorName + " 选择动作：" + rawAction,
                Map.of("intentId", intent.intentId(), "nodeId", requestedNodeId),
                sourceTrace
        ));

        int currentIndex = node.nodeIndex();
        StoryNodeSkeletonItem nextNode = runtimeView.skeleton().nodes().stream()
                .filter(candidate -> candidate.nodeIndex() == currentIndex + 1)
                .findFirst()
                .orElse(null);
        String resolutionSummary = buildResolutionSummary(node, selectedOption, customAction, nextNode);

        StoryEvent resolvedEvent = storyEventStore.appendEvent(new StoryEvent(
                "event-" + UUID.randomUUID(),
                StoryEventType.TURN_RESOLVED,
                request.projectId(),
                request.chapterId(),
                requestedNodeId,
                new StoryUnitRef(requestedNodeId, "node:" + request.chapterId() + ":" + requestedNodeId, StoryUnitType.SCENE_EXECUTION),
                resolutionSummary,
                Map.of(
                        "intentId", intent.intentId(),
                        "currentNodeId", requestedNodeId,
                        "nextNodeId", nextNode == null ? "" : nextNode.nodeId()
                ),
                sourceTrace
        ));

        String nextCheckpointId = "checkpoint-" + UUID.randomUUID();
        StoryResolvedTurn resolvedTurn = storyResolvedTurnStore.recordTurn(new StoryResolvedTurn(
                "turn-" + UUID.randomUUID(),
                request.projectId(),
                request.chapterId(),
                latestCheckpointId,
                requestedNodeId,
                intent.intentId(),
                resolutionSummary,
                List.of(intentEvent.eventId(), resolvedEvent.eventId()),
                Map.of(
                        "currentNodeId", requestedNodeId,
                        "nextNodeId", nextNode == null ? "" : nextNode.nodeId(),
                        "selectedMode", selectedOption == null ? "custom" : selectedOption.optionId()
                ),
                buildReaderRevealDelta(node, selectedOption, customAction),
                nextNode == null ? List.of() : List.of(loopIdForNode(nextNode.nodeId())),
                List.of(loopIdForNode(requestedNodeId)),
                nextCheckpointId,
                sourceTrace
        ));

        List<StoryOpenLoop> updatedLoops = updateLoops(request.projectId(), request.chapterId(), requestedNodeId, nextNode, sourceTrace);
        ReaderRevealState readerRevealState = updateReaderRevealState(request.projectId(), request.chapterId(), node, selectedOption, customAction);
        ChapterIncrementalState chapterState = updateChapterState(request.projectId(), request.chapterId(), updatedLoops, context, node, nextNode);

        StoryNodeCheckpoint checkpoint = storyNodeCheckpointStore.saveCheckpoint(new StoryNodeCheckpoint(
                nextCheckpointId,
                request.projectId(),
                request.chapterId(),
                nextNode == null ? requestedNodeId : nextNode.nodeId(),
                latestCheckpointId,
                nextNode == null ? currentIndex : nextNode.nodeIndex(),
                resolutionSummary,
                readerRevealState.summary(),
                updatedLoops.stream()
                        .filter(loop -> loop.status() != StoryLoopStatus.RESOLVED)
                        .map(StoryOpenLoop::loopId)
                        .toList(),
                Map.of(actorName, firstNonBlank(context == null ? "" : context.chapterAnchorBundle().chapterTitle(), "当前章节")),
                Map.of(actorName, nextNode == null ? "完成当前章节" : nextNode.playerGoal()),
                nextNode == null ? List.of() : nextNode.recommendedActions().stream().map(NodeActionOption::optionId).toList(),
                sourceTrace
        ));

        storyEventStore.appendEvent(new StoryEvent(
                "event-" + UUID.randomUUID(),
                StoryEventType.CHECKPOINT_SAVED,
                request.projectId(),
                request.chapterId(),
                checkpoint.nodeId(),
                new StoryUnitRef(checkpoint.checkpointId(), "checkpoint:" + request.chapterId() + ":" + checkpoint.checkpointId(), StoryUnitType.CHAPTER),
                "checkpoint 已保存：" + checkpoint.checkpointId(),
                Map.of("checkpointId", checkpoint.checkpointId(), "nodeId", checkpoint.nodeId()),
                sourceTrace
        ));

        List<StoryOpenLoop> activeLoops = updatedLoops.stream()
                .filter(loop -> loop.status() != StoryLoopStatus.RESOLVED)
                .toList();
        return Optional.of(new NodeResolutionResult(
                request.projectId(),
                request.chapterId(),
                requestedNodeId,
                latestCheckpointId,
                intent,
                resolvedTurn,
                checkpoint,
                activeLoops,
                readerRevealState,
                chapterState
        ));
    }

    private ChapterNodeSkeleton toNodeSkeleton(ChapterSkeleton skeleton, StorySessionContextPacket context) {
        List<StoryNodeSkeletonItem> nodes = new ArrayList<>();
        List<com.storyweaver.story.generation.orchestration.SceneSkeletonItem> orderedScenes = skeleton.scenes().stream()
                .sorted(java.util.Comparator.comparingInt(com.storyweaver.story.generation.orchestration.SceneSkeletonItem::sceneIndex))
                .toList();
        for (int index = 0; index < orderedScenes.size(); index++) {
            com.storyweaver.story.generation.orchestration.SceneSkeletonItem scene = orderedScenes.get(index);
            com.storyweaver.story.generation.orchestration.SceneSkeletonItem nextScene =
                    index + 1 < orderedScenes.size() ? orderedScenes.get(index + 1) : null;
            nodes.add(new StoryNodeSkeletonItem(
                    toNodeId(scene.sceneId()),
                    scene.sceneIndex(),
                    "节点 " + scene.sceneIndex(),
                    buildSituation(scene, context),
                    scene.goal(),
                    buildOptions(scene, nextScene),
                    true,
                    scene.stopCondition(),
                    "完成本节点结算后写入新的 checkpoint。",
                    nextScene == null ? List.of() : List.of(firstNonBlank(nextScene.goal(), nextScene.stopCondition()))
            ));
        }
        return new ChapterNodeSkeleton(
                skeleton.projectId(),
                skeleton.chapterId(),
                "node-" + skeleton.skeletonId(),
                nodes.size(),
                skeleton.globalStopCondition(),
                nodes,
                appendPlanningNote(skeleton.planningNotes(), "当前 node skeleton 由现有 scene skeleton 适配生成，供 node mode 第一阶段使用。")
        );
    }

    private List<NodeActionOption> buildOptions(
            com.storyweaver.story.generation.orchestration.SceneSkeletonItem scene,
            com.storyweaver.story.generation.orchestration.SceneSkeletonItem nextScene) {
        return List.of(
                new NodeActionOption(
                        OPTION_ADVANCE,
                        "正面推进",
                        firstNonBlank(scene.goal(), "按当前节点目标继续推进"),
                        "最容易触发局面变化，也最容易提前吃掉后续节点空间。",
                        firstNonBlank(firstListValue(scene.readerReveal()), "让当前节点的一条关键信息明确落地")
                ),
                new NodeActionOption(
                        OPTION_PROBE,
                        "试探信息",
                        "围绕当前节点目标试探人物、信息或环境，但不直接用尽后续空间。",
                        "信息获取更稳，但推进速度会更慢。",
                        nextScene == null ? "为本章收口前再补一层信息准备。" : "为下一节点预留更清晰的入口。"
                ),
                new NodeActionOption(
                        OPTION_HOLD,
                        "克制收束",
                        "在保持当前目标方向的前提下，控制节奏并停在当前节点停点前。",
                        "更利于保留节点边界，但爆点可能偏弱。",
                        firstNonBlank(scene.stopCondition(), "只把局面送到本节点停点附近。")
                )
        );
    }

    private String buildSituation(com.storyweaver.story.generation.orchestration.SceneSkeletonItem scene, StorySessionContextPacket context) {
        List<String> parts = new ArrayList<>();
        if (!scene.mustUseAnchors().isEmpty()) {
            parts.add("沿用锚点：" + String.join("，", scene.mustUseAnchors()));
        }
        if (context != null && StringUtils.hasText(context.chapterAnchorBundle().mainPovCharacterName())) {
            parts.add("主视角角色：" + context.chapterAnchorBundle().mainPovCharacterName());
        }
        if (!scene.readerReveal().isEmpty()) {
            parts.add("本节点允许读者知道：" + String.join("，", scene.readerReveal()));
        }
        return parts.isEmpty() ? firstNonBlank(scene.goal(), scene.stopCondition(), "推进当前章节节点。") : String.join("；", parts);
    }

    private StoryOpenLoop buildLoop(
            Long projectId,
            Long chapterId,
            String nodeId,
            StoryLoopStatus status,
            String payoffHint,
            String resolvedByTurnId,
            StorySourceTrace sourceTrace) {
        return new StoryOpenLoop(
                loopIdForNode(nodeId),
                projectId,
                chapterId,
                nodeId,
                "进入节点 " + nodeId,
                status,
                "chapter-runtime",
                payoffHint,
                null,
                resolvedByTurnId,
                List.of("chapter:" + chapterId),
                sourceTrace
        );
    }

    private List<StoryOpenLoop> updateLoops(
            Long projectId,
            Long chapterId,
            String currentNodeId,
            StoryNodeSkeletonItem nextNode,
            StorySourceTrace sourceTrace) {
        Map<String, StoryOpenLoop> loopById = new LinkedHashMap<>();
        storyOpenLoopStore.listChapterLoops(projectId, chapterId).forEach(loop -> loopById.put(loop.loopId(), loop));

        String currentLoopId = loopIdForNode(currentNodeId);
        StoryOpenLoop currentLoop = loopById.get(currentLoopId);
        if (currentLoop != null && currentLoop.status() != StoryLoopStatus.RESOLVED) {
            loopById.put(currentLoopId, storyOpenLoopStore.saveLoop(new StoryOpenLoop(
                    currentLoop.loopId(),
                    currentLoop.projectId(),
                    currentLoop.chapterId(),
                    currentLoop.sourceNodeId(),
                    currentLoop.label(),
                    StoryLoopStatus.RESOLVED,
                    currentLoop.owner(),
                    currentLoop.payoffHint(),
                    currentLoop.sourceTurnId(),
                    "turn-pending",
                    currentLoop.relatedUnitRefs(),
                    currentLoop.sourceTrace()
            )));
        }

        if (nextNode != null) {
            String nextLoopId = loopIdForNode(nextNode.nodeId());
            StoryOpenLoop nextLoop = new StoryOpenLoop(
                    nextLoopId,
                    projectId,
                    chapterId,
                    nextNode.nodeId(),
                    "进入节点 " + nextNode.nodeId(),
                    StoryLoopStatus.OPEN,
                    "chapter-runtime",
                    "在推进到 " + nextNode.nodeId() + " 并完成结算后回收。",
                    null,
                    null,
                    List.of("chapter:" + chapterId),
                    sourceTrace
            );
            loopById.put(nextLoopId, storyOpenLoopStore.saveLoop(nextLoop));
        }

        return loopById.values().stream()
                .map(storyOpenLoopStore::saveLoop)
                .distinct()
                .toList();
    }

    private ReaderRevealState updateReaderRevealState(
            Long projectId,
            Long chapterId,
            StoryNodeSkeletonItem node,
            NodeActionOption selectedOption,
            String customAction) {
        ReaderRevealState current = readerRevealStateStore.findChapterRevealState(projectId, chapterId)
                .orElse(new ReaderRevealState(projectId, chapterId, List.of(), List.of(), List.of(), List.of(), ""));
        LinkedHashSet<String> readerKnown = new LinkedHashSet<>(current.readerKnown());
        buildReaderRevealDelta(node, selectedOption, customAction).forEach(readerKnown::add);
        ReaderRevealState next = new ReaderRevealState(
                projectId,
                chapterId,
                current.systemKnown(),
                current.authorKnown(),
                List.copyOf(readerKnown),
                current.unrevealed(),
                "读者当前已知 " + readerKnown.size() + " 条节点级信息。"
        );
        return readerRevealStateStore.saveChapterRevealState(next);
    }

    private ChapterIncrementalState updateChapterState(
            Long projectId,
            Long chapterId,
            List<StoryOpenLoop> loops,
            StorySessionContextPacket context,
            StoryNodeSkeletonItem currentNode,
            StoryNodeSkeletonItem nextNode) {
        ChapterIncrementalState current = chapterIncrementalStateStore.findChapterState(projectId, chapterId)
                .orElse(new ChapterIncrementalState(projectId, chapterId, List.of(), List.of(), List.of(), Map.of(), Map.of(), Map.of(), ""));
        List<String> openLoops = loops.stream()
                .filter(loop -> loop.status() != StoryLoopStatus.RESOLVED)
                .map(StoryOpenLoop::loopId)
                .toList();
        List<String> resolvedLoops = loops.stream()
                .filter(loop -> loop.status() == StoryLoopStatus.RESOLVED)
                .map(StoryOpenLoop::loopId)
                .toList();
        List<String> activeLocations = current.activeLocations().isEmpty()
                ? resolveFallbackLocations(context)
                : current.activeLocations();
        String povName = resolveActorName(context);
        Map<String, String> emotions = current.characterEmotions().isEmpty()
                ? Map.of(povName, nextNode == null ? "收束" : "推进中")
                : current.characterEmotions();
        Map<String, String> attitudes = current.characterAttitudes().isEmpty()
                ? Map.of(povName, nextNode == null ? "完成当前章节目标" : "准备进入下一节点")
                : current.characterAttitudes();
        Map<String, List<String>> tags = current.characterStateTags().isEmpty()
                ? Map.of(povName, List.of("node:" + currentNode.nodeId()))
                : current.characterStateTags();
        ChapterIncrementalState nextState = new ChapterIncrementalState(
                projectId,
                chapterId,
                openLoops,
                resolvedLoops,
                activeLocations,
                emotions,
                attitudes,
                tags,
                nextNode == null ? "当前章节节点链已推进到末尾。" : "当前章节等待推进 " + nextNode.nodeId() + "。"
        );
        return chapterIncrementalStateStore.saveChapterState(nextState);
    }

    private List<String> buildReaderRevealDelta(StoryNodeSkeletonItem node, NodeActionOption selectedOption, String customAction) {
        if (!customAction.isEmpty()) {
            return List.of("本节点确认动作：" + customAction);
        }
        if (selectedOption == null) {
            return List.of("本节点已完成一次推进。");
        }
        return List.of(firstNonBlank(selectedOption.revealHint(), selectedOption.intentSummary(), node.stopCondition(), "本节点已完成一次推进。"));
    }

    private String buildResolutionSummary(
            StoryNodeSkeletonItem currentNode,
            NodeActionOption selectedOption,
            String customAction,
            StoryNodeSkeletonItem nextNode) {
        String actionText = !customAction.isEmpty()
                ? customAction
                : (selectedOption == null ? currentNode.playerGoal() : selectedOption.intentSummary());
        String landing = nextNode == null
                ? "并把当前章节推进到收口前。"
                : "并把局面送到 " + nextNode.nodeId() + " 的入口前。";
        return firstNonBlank(actionText, currentNode.playerGoal(), "完成当前节点结算") + landing;
    }

    private NodeActionOption resolveSelectedOption(StoryNodeSkeletonItem node, String selectedOptionId) {
        String optionId = normalizeText(selectedOptionId);
        if (optionId.isEmpty()) {
            return null;
        }
        return node.recommendedActions().stream()
                .filter(option -> option.optionId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("推荐动作不存在: " + optionId));
    }

    private String resolveActorName(StorySessionContextPacket context) {
        if (context != null && StringUtils.hasText(context.chapterAnchorBundle().mainPovCharacterName())) {
            return context.chapterAnchorBundle().mainPovCharacterName().trim();
        }
        return "玩家";
    }

    private List<String> resolveFallbackLocations(StorySessionContextPacket context) {
        if (context == null) {
            return List.of();
        }
        if (StringUtils.hasText(context.chapterAnchorBundle().chapterTitle())) {
            return List.of(context.chapterAnchorBundle().chapterTitle().trim());
        }
        return List.of();
    }

    private List<String> appendPlanningNote(List<String> notes, String note) {
        List<String> result = new ArrayList<>(notes == null ? List.of() : notes);
        result.add(note);
        return List.copyOf(result);
    }

    private String toNodeId(String sceneId) {
        String normalized = normalizeText(sceneId);
        if (normalized.startsWith("scene-")) {
            return "node-" + normalized.substring("scene-".length());
        }
        return normalized;
    }

    private String loopIdForNode(String nodeId) {
        return "node-loop:" + nodeId;
    }

    private StorySourceTrace sourceTrace(String sourceType, String sourceRef) {
        return new StorySourceTrace("system", "system", sourceType, sourceRef);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String firstListValue(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return firstNonBlank(values.getFirst());
    }
}
