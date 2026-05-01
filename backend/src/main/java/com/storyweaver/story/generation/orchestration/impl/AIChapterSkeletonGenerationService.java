package com.storyweaver.story.generation.orchestration.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.storyweaver.ai.application.support.StructuredJsonSupport;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.service.AIModelRoutingService;
import com.storyweaver.service.AIProviderService;
import com.storyweaver.service.ChapterService;
import com.storyweaver.story.generation.orchestration.ChapterSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonGenerationService;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonStore;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonStreamEvent;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.story.generation.orchestration.StorySessionContextAssembler;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Service
public class AIChapterSkeletonGenerationService implements ChapterSkeletonGenerationService {

    private static final int MIN_SCENE_COUNT = 2;
    private static final int MAX_SCENE_COUNT = 6;

    private final StorySessionContextAssembler storySessionContextAssembler;
    private final ChapterSkeletonStore chapterSkeletonStore;
    private final AIModelRoutingService aiModelRoutingService;
    private final AIProviderService aiProviderService;
    private final StructuredJsonSupport structuredJsonSupport;
    private final ChapterService chapterService;

    public AIChapterSkeletonGenerationService(
            StorySessionContextAssembler storySessionContextAssembler,
            ChapterSkeletonStore chapterSkeletonStore,
            AIModelRoutingService aiModelRoutingService,
            AIProviderService aiProviderService,
            StructuredJsonSupport structuredJsonSupport,
            ChapterService chapterService) {
        this.storySessionContextAssembler = storySessionContextAssembler;
        this.chapterSkeletonStore = chapterSkeletonStore;
        this.aiModelRoutingService = aiModelRoutingService;
        this.aiProviderService = aiProviderService;
        this.structuredJsonSupport = structuredJsonSupport;
        this.chapterService = chapterService;
    }

    @Override
    public Optional<ChapterSkeleton> generate(Long projectId, Long chapterId, boolean forceRefresh) {
        return generateInternal(projectId, chapterId, forceRefresh, null);
    }

    @Override
    public void generateStream(
            Long projectId,
            Long chapterId,
            boolean forceRefresh,
            Consumer<ChapterSkeletonStreamEvent> eventConsumer) {
        Optional<ChapterSkeleton> skeleton = generateInternal(projectId, chapterId, forceRefresh, eventConsumer);
        if (skeleton.isEmpty()) {
            throw new IllegalStateException("章节不存在或无法生成镜头骨架");
        }
        eventConsumer.accept(ChapterSkeletonStreamEvent.complete(skeleton.get()));
    }

    private Optional<ChapterSkeleton> generateInternal(
            Long projectId,
            Long chapterId,
            boolean forceRefresh,
            Consumer<ChapterSkeletonStreamEvent> eventConsumer) {
        emitStage(eventConsumer, "prepare", "started", forceRefresh ? "开始重新规划章节镜头骨架。" : "开始生成章节镜头骨架。");
        if (!forceRefresh) {
            Optional<ChapterSkeleton> storedSkeleton = chapterSkeletonStore.find(projectId, chapterId);
            if (storedSkeleton.isPresent()) {
                emitLog(eventConsumer, "prepare", "检测到当前章节已有已保存骨架，直接返回现有结果。");
                emitStage(eventConsumer, "prepare", "completed", "已直接返回当前章节的已保存骨架。");
                return storedSkeleton;
            }
        }

        Optional<StorySessionContextPacket> contextPacket = storySessionContextAssembler.assemble(projectId, chapterId, "scene-1");
        if (contextPacket.isEmpty()) {
            emitLog(eventConsumer, "prepare", "当前章节上下文缺失，无法生成镜头骨架。");
            return Optional.empty();
        }

        Chapter chapter = chapterService.getById(chapterId);
        if (chapter == null || chapter.getProjectId() == null || !chapter.getProjectId().equals(projectId)) {
            emitLog(eventConsumer, "prepare", "当前章节不存在或项目归属不匹配，无法生成镜头骨架。");
            return Optional.empty();
        }
        emitStage(eventConsumer, "prepare", "completed", "章节上下文、读者状态和已完成镜头前缀已装载。");

        AIModelRoutingService.ResolvedModelSelection selection = aiModelRoutingService.resolve(null, null, "director");
        emitMeta(eventConsumer, selection, forceRefresh);
        emitStage(eventConsumer, "plan", "started", "AI 正在规划章节镜头骨架。");
        String rawResponse = generateSkeletonResponseWithHeartbeat(selection, chapter, contextPacket.get(), eventConsumer);
        emitStage(eventConsumer, "plan", "completed", "AI 已返回镜头骨架规划结果。");

        emitStage(eventConsumer, "check", "started", "开始校验镜头骨架结果。");
        GeneratedSkeleton generatedSkeleton = parseGeneratedSkeleton(rawResponse, contextPacket.get());
        emitStage(eventConsumer, "check", "completed", "镜头骨架结构校验通过。");
        ChapterSkeleton skeleton = new ChapterSkeleton(
                projectId,
                chapterId,
                "skeleton_" + chapterId + "_" + System.currentTimeMillis(),
                generatedSkeleton.scenes().size(),
                generatedSkeleton.globalStopCondition(),
                generatedSkeleton.scenes(),
                List.of(),
                generatedSkeleton.planningNotes()
        );
        emitStage(eventConsumer, "persist", "started", "开始保存章节骨架。");
        ChapterSkeleton savedSkeleton = chapterSkeletonStore.save(skeleton);
        emitStage(eventConsumer, "persist", "completed", "章节骨架已保存，可进入逐镜头写作。");
        return Optional.of(savedSkeleton);
    }

    private String generateSkeletonResponseWithHeartbeat(
            AIModelRoutingService.ResolvedModelSelection selection,
            Chapter chapter,
            StorySessionContextPacket contextPacket,
            Consumer<ChapterSkeletonStreamEvent> eventConsumer) {
        AtomicReference<String> rawResponseRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(chapter, contextPacket);

        Thread worker = Thread.startVirtualThread(() -> {
            try {
                rawResponseRef.set(aiProviderService.generateText(
                        selection.provider(),
                        selection.model(),
                        systemPrompt,
                        userPrompt,
                        0.3,
                        1800
                ));
            } catch (Throwable throwable) {
                errorRef.set(throwable);
            }
        });

        long startedAt = System.nanoTime();
        while (worker.isAlive()) {
            try {
                worker.join(5000L);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("镜头骨架生成已中断", interruptedException);
            }
            if (worker.isAlive()) {
                long elapsedSeconds = Math.max(1L, (System.nanoTime() - startedAt) / 1_000_000_000L);
                emitLog(eventConsumer, "plan", "镜头骨架仍在生成中，请稍候（已等待 " + elapsedSeconds + " 秒）。");
            }
        }

        Throwable throwable = errorRef.get();
        if (throwable != null) {
            if (throwable instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("镜头骨架生成失败", throwable);
        }

        String rawResponse = rawResponseRef.get();
        if (!StringUtils.hasText(rawResponse)) {
            throw new IllegalStateException("镜头骨架生成没有返回内容");
        }
        return rawResponse;
    }

    private String buildSystemPrompt() {
        return """
                你是小说章节的镜头骨架规划器。
                你的任务是把当前章节拆成连续、互不重复的多个 scene，用于后续逐镜头生成正文。
                只返回严格 JSON，不要解释，不要 Markdown，不要正文。
                JSON 结构固定为：
                {
                  "planningNotes": ["..."],
                  "scenes": [
                    {
                      "sceneIndex": 1,
                      "goal": "...",
                      "readerReveal": ["..."],
                      "mustUseAnchors": ["..."],
                      "stopCondition": "...",
                      "targetWords": 900
                    }
                  ]
                }
                规则：
                1. scene 必须按时间和因果顺序推进，不能让 scene-2 重复 scene-1 的同一动作。
                2. 每个 scene 只负责一个清晰推进节点，stopCondition 必须是本镜头结束点，不能直接跨到下一镜头。
                3. 如果输入里存在已完成 scene，它们是固定前缀；你只负责规划后续 scene，但返回结果仍要包含完整 scene 序列。
                4. scene 总数控制在 2 到 6 个。
                5. readerReveal 只写当前镜头首次向读者揭示的信息。
                6. mustUseAnchors 只写当前镜头正文里必须落到的锚点，不要重复整章摘要。
                """;
    }

    private String buildUserPrompt(Chapter chapter, StorySessionContextPacket contextPacket) {
        StringBuilder builder = new StringBuilder();
        builder.append("请为当前章节规划镜头骨架。\n");
        builder.append("项目：").append(fallback(contextPacket.projectBrief().projectTitle(), "未命名项目")).append('\n');
        builder.append("章节标题：").append(fallback(chapter.getTitle(), "未命名章节")).append('\n');
        builder.append("章节序号：").append(chapter.getOrderNum() == null ? 0 : chapter.getOrderNum()).append('\n');
        builder.append("章节摘要：").append(fallback(contextPacket.chapterAnchorBundle().chapterSummary(), contextPacket.chapterSummary().summary(), "无")).append('\n');
        builder.append("当前正文长度：").append(chapter.getContent() == null ? 0 : chapter.getContent().trim().length()).append('\n');
        builder.append("POV：").append(fallback(contextPacket.chapterAnchorBundle().mainPovCharacterName(), "未指定")).append('\n');
        builder.append("活动人物：").append(joinOrFallback(contextPacket.chapterAnchorBundle().activeCharacterNames(), "无")).append('\n');
        builder.append("剧情锚点：").append(joinOrFallback(contextPacket.chapterAnchorBundle().activePlotTitles(), "无")).append('\n');
        builder.append("故事节拍：").append(joinOrFallback(contextPacket.chapterAnchorBundle().storyBeats(), "无")).append('\n');
        builder.append("读者已知：").append(joinOrFallback(contextPacket.readerKnownState().knownFacts(), "无")).append('\n');
        builder.append("待揭晓：").append(joinOrFallback(contextPacket.readerKnownState().unrevealedFacts(), "无")).append('\n');

        List<SceneExecutionState> lockedScenes = lockedSceneStates(contextPacket);
        if (lockedScenes.isEmpty()) {
            builder.append("已完成 scene：无，请规划完整章节骨架。\n");
        } else {
            builder.append("已完成 scene（固定前缀，不可改写）：\n");
            for (SceneExecutionState scene : lockedScenes) {
                builder.append("- ")
                        .append(scene.sceneId())
                        .append(" | goal=").append(fallback(scene.goal(), "无"))
                        .append(" | summary=").append(fallback(scene.outcomeSummary(), "无"))
                        .append(" | stop=").append(fallback(scene.stopCondition(), "无"))
                        .append('\n');
            }
        }

        if (!contextPacket.recentStoryProgress().items().isEmpty()) {
            builder.append("最近进度：\n");
            contextPacket.recentStoryProgress().items().stream()
                    .limit(3)
                    .forEach(item -> builder.append("- ")
                            .append(fallback(item.title(), item.itemType(), "未命名"))
                            .append(" | ")
                            .append(fallback(item.summary(), "无摘要"))
                            .append('\n'));
        }

        if (StringUtils.hasText(chapter.getContent())) {
            builder.append("当前正文摘要预览：").append(preview(chapter.getContent(), 220)).append('\n');
        }

        builder.append("请返回完整 scene 数组，sceneIndex 必须连续。");
        return builder.toString();
    }

    private GeneratedSkeleton parseGeneratedSkeleton(String rawResponse, StorySessionContextPacket contextPacket) {
        JsonNode root = structuredJsonSupport.readRoot(
                rawResponse,
                "镜头骨架生成没有返回内容",
                "镜头骨架生成结果不是有效 JSON"
        );
        JsonNode scenesNode = root.path("scenes");
        if (!scenesNode.isArray() || scenesNode.isEmpty()) {
            throw new IllegalStateException("镜头骨架生成结果缺少 scenes 数组");
        }

        Map<Integer, SceneExecutionState> lockedByIndex = new LinkedHashMap<>();
        for (SceneExecutionState state : lockedSceneStates(contextPacket)) {
            lockedByIndex.put(state.sceneIndex(), state);
        }

        Map<Integer, JsonNode> generatedByIndex = new LinkedHashMap<>();
        int sequentialIndex = 1;
        for (JsonNode sceneNode : scenesNode) {
            int parsedIndex = structuredJsonSupport.readInt(sceneNode, sequentialIndex, "sceneIndex", "index");
            int sceneIndex = Math.max(1, Math.min(MAX_SCENE_COUNT, parsedIndex));
            generatedByIndex.putIfAbsent(sceneIndex, sceneNode);
            sequentialIndex += 1;
        }

        int maxGeneratedIndex = generatedByIndex.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        int lockedCount = lockedByIndex.size();
        if (lockedCount > 0 && maxGeneratedIndex <= lockedCount) {
            throw new IllegalStateException("镜头骨架生成结果没有给出已完成 scene 之后的后续镜头");
        }

        int totalSceneCount = Math.max(Math.max(maxGeneratedIndex, lockedCount), MIN_SCENE_COUNT);
        totalSceneCount = Math.min(totalSceneCount, MAX_SCENE_COUNT);

        List<SceneSkeletonItem> scenes = new ArrayList<>();
        for (int sceneIndex = 1; sceneIndex <= totalSceneCount; sceneIndex += 1) {
            SceneExecutionState lockedState = lockedByIndex.get(sceneIndex);
            if (lockedState != null) {
                scenes.add(toLockedScene(lockedState, contextPacket));
                continue;
            }

            JsonNode sceneNode = generatedByIndex.get(sceneIndex);
            if (sceneNode == null) {
                throw new IllegalStateException("镜头骨架生成结果缺少 scene-" + sceneIndex + " 的规划");
            }
            scenes.add(toGeneratedScene(sceneNode, sceneIndex, contextPacket));
        }

        List<String> planningNotes = structuredJsonSupport.readList(root, "planningNotes", "notes");
        List<String> notes = new ArrayList<>(planningNotes);
        notes.add("镜头骨架已通过 AI 重新规划，共 " + scenes.size() + " 个镜头。");
        if (!lockedByIndex.isEmpty()) {
            notes.add("已保留 " + lockedByIndex.size() + " 个已完成镜头作为固定前缀。");
        }

        String globalStopCondition = scenes.get(scenes.size() - 1).stopCondition();
        return new GeneratedSkeleton(List.copyOf(scenes), List.copyOf(new LinkedHashSet<>(notes)), globalStopCondition);
    }

    private SceneSkeletonItem toLockedScene(SceneExecutionState state, StorySessionContextPacket contextPacket) {
        return new SceneSkeletonItem(
                state.sceneId(),
                state.sceneIndex(),
                state.status(),
                fallback(state.goal(), state.outcomeSummary(), "已完成镜头"),
                state.readerRevealDelta(),
                baseAnchors(contextPacket),
                fallback(state.stopCondition(), contextPacket.chapterSummary().summary(), "已完成当前镜头"),
                null,
                "existing-scene-state"
        );
    }

    private SceneSkeletonItem toGeneratedScene(JsonNode sceneNode, int sceneIndex, StorySessionContextPacket contextPacket) {
        String goal = structuredJsonSupport.readText(sceneNode, "goal", "objective");
        String stopCondition = structuredJsonSupport.readText(sceneNode, "stopCondition", "ending", "stop");
        if (!StringUtils.hasText(goal) || !StringUtils.hasText(stopCondition)) {
            throw new IllegalStateException("镜头骨架生成结果缺少必要的 goal / stopCondition 字段");
        }

        List<String> mustUseAnchors = mergeUnique(
                baseAnchors(contextPacket),
                structuredJsonSupport.readList(sceneNode, "mustUseAnchors", "anchors")
        );
        int targetWords = Math.max(500, structuredJsonSupport.readInt(sceneNode, 900, "targetWords", "targetWordCount"));
        return new SceneSkeletonItem(
                "scene-" + sceneIndex,
                sceneIndex,
                SceneExecutionStatus.PLANNED,
                goal,
                structuredJsonSupport.readList(sceneNode, "readerReveal", "reveals"),
                mustUseAnchors,
                stopCondition,
                targetWords,
                "ai-skeleton"
        );
    }

    private List<SceneExecutionState> lockedSceneStates(StorySessionContextPacket contextPacket) {
        return contextPacket.existingSceneStates().stream()
                .filter(state -> state.status() == SceneExecutionStatus.COMPLETED)
                .sorted(java.util.Comparator.comparingInt(SceneExecutionState::sceneIndex).thenComparing(SceneExecutionState::sceneId))
                .toList();
    }

    private List<String> baseAnchors(StorySessionContextPacket contextPacket) {
        List<String> anchors = new ArrayList<>();
        if (StringUtils.hasText(contextPacket.chapterAnchorBundle().chapterTitle())) {
            anchors.add("chapter=" + contextPacket.chapterAnchorBundle().chapterTitle().trim());
        }
        if (StringUtils.hasText(contextPacket.chapterAnchorBundle().mainPovCharacterName())) {
            anchors.add("pov=" + contextPacket.chapterAnchorBundle().mainPovCharacterName().trim());
        }
        if (StringUtils.hasText(contextPacket.chapterAnchorBundle().chapterSummary())) {
            anchors.add("summary=" + preview(contextPacket.chapterAnchorBundle().chapterSummary(), 80));
        }
        return List.copyOf(anchors);
    }

    private List<String> mergeUnique(List<String> left, List<String> right) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.addAll(left == null ? List.of() : left);
        values.addAll(right == null ? List.of() : right);
        return List.copyOf(values);
    }

    private String joinOrFallback(List<String> items, String fallback) {
        if (items == null || items.isEmpty()) {
            return fallback;
        }
        return String.join("；", items);
    }

    private String fallback(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String preview(String value, int limit) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, limit) + "...";
    }

    private void emitMeta(
            Consumer<ChapterSkeletonStreamEvent> eventConsumer,
            AIModelRoutingService.ResolvedModelSelection selection,
            boolean forceRefresh) {
        if (eventConsumer == null) {
            return;
        }
        eventConsumer.accept(ChapterSkeletonStreamEvent.meta(
                selection.provider() == null ? null : selection.provider().getId(),
                selection.model(),
                forceRefresh
        ));
    }

    private void emitStage(Consumer<ChapterSkeletonStreamEvent> eventConsumer, String stage, String status, String message) {
        if (eventConsumer == null) {
            return;
        }
        eventConsumer.accept(ChapterSkeletonStreamEvent.stage(stage, status, message));
    }

    private void emitLog(Consumer<ChapterSkeletonStreamEvent> eventConsumer, String stage, String message) {
        if (eventConsumer == null) {
            return;
        }
        eventConsumer.accept(ChapterSkeletonStreamEvent.log(stage, message));
    }

    private record GeneratedSkeleton(
            List<SceneSkeletonItem> scenes,
            List<String> planningNotes,
            String globalStopCondition) {
    }
}
