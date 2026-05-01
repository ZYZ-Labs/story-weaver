package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.domain.entity.AIWritingRecord;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.KnowledgeDocument;
import com.storyweaver.domain.vo.AIWritingRollbackResponseVO;
import com.storyweaver.repository.AIWritingRecordMapper;
import com.storyweaver.service.ChapterService;
import com.storyweaver.service.KnowledgeDocumentService;
import com.storyweaver.story.generation.orchestration.ChapterSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonPlanner;
import com.storyweaver.story.generation.orchestration.SceneSkeletonItem;
import com.storyweaver.story.generation.orchestration.impl.ChapterSceneWorkflowGuardService;
import com.storyweaver.storyunit.context.ChapterAnchorBundleView;
import com.storyweaver.storyunit.context.CharacterRuntimeStateView;
import com.storyweaver.storyunit.context.StoryContextQueryService;
import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
import com.storyweaver.storyunit.facet.state.ChapterIncrementalState;
import com.storyweaver.storyunit.service.ChapterIncrementalStateStore;
import com.storyweaver.storyunit.service.ReaderRevealStateStore;
import com.storyweaver.storyunit.service.SceneRuntimeStateStore;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneExecutionStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChapterWorkspaceAcceptedSceneRollbackServiceTest {

    @Test
    void shouldRollbackLatestAcceptedSceneUsingAcceptanceSnapshotEvenIfEarlierRuntimeExpired() {
        Chapter currentChapter = new Chapter();
        currentChapter.setId(31L);
        currentChapter.setProjectId(28L);
        currentChapter.setTitle("退役者归来");
        currentChapter.setOrderNum(2);
        currentChapter.setContent("章节旧正文\n\nscene1-text\n\n人工补充\n\nscene2-text");
        currentChapter.setWordCount(currentChapter.getContent().length());

        Chapter previousChapter = new Chapter();
        previousChapter.setId(30L);
        previousChapter.setProjectId(28L);
        previousChapter.setTitle("上一章");
        previousChapter.setSummary("旧战队已经重组。");
        previousChapter.setOrderNum(1);

        SceneSkeletonItem scene1 = new SceneSkeletonItem(
                "scene-1",
                1,
                SceneExecutionStatus.COMPLETED,
                "收到邀请",
                List.of("旧战队发来邀请"),
                List.of(),
                "决定是否重返游戏。",
                900,
                "ai-skeleton"
        );
        SceneSkeletonItem scene2 = new SceneSkeletonItem(
                "scene-2",
                2,
                SceneExecutionStatus.COMPLETED,
                "重新登录游戏",
                List.of("林沉舟决定回归"),
                List.of(),
                "进入游戏大厅。",
                900,
                "ai-skeleton"
        );
        SceneSkeletonItem scene3 = new SceneSkeletonItem(
                "scene-3",
                3,
                SceneExecutionStatus.PLANNED,
                "与老陈会面",
                List.of("准备与老陈会面"),
                List.of(),
                "抵达集合点。",
                900,
                "ai-skeleton"
        );

        AIWritingRecord acceptedScene1 = new AIWritingRecord();
        acceptedScene1.setId(101L);
        acceptedScene1.setChapterId(31L);
        acceptedScene1.setStatus("accepted");
        acceptedScene1.setWritingType("continue");
        acceptedScene1.setOriginalContent("stale-baseline");
        acceptedScene1.setGeneratedContent("scene1-text");
        acceptedScene1.setGenerationTraceJson("""
                {
                  "orchestration": {
                    "entryPoint": "phase8.chapter-workspace.scene-draft",
                    "sceneId": "scene-1"
                  },
                  "acceptance": {
                    "contentBeforeAccept": "章节旧正文",
                    "contentAfterAccept": "章节旧正文\\n\\nscene1-text"
                  }
                }
                """);

        AIWritingRecord acceptedScene2 = new AIWritingRecord();
        acceptedScene2.setId(102L);
        acceptedScene2.setChapterId(31L);
        acceptedScene2.setStatus("accepted");
        acceptedScene2.setWritingType("continue");
        acceptedScene2.setOriginalContent("stale-scene1");
        acceptedScene2.setGeneratedContent("scene2-text");
        acceptedScene2.setGenerationTraceJson("""
                {
                  "orchestration": {
                    "entryPoint": "phase8.chapter-workspace.scene-draft",
                    "sceneId": "scene-2"
                  },
                  "acceptance": {
                    "contentBeforeAccept": "章节旧正文\\n\\nscene1-text\\n\\n人工补充",
                    "contentAfterAccept": "章节旧正文\\n\\nscene1-text\\n\\n人工补充\\n\\nscene2-text"
                  }
                }
                """);

        SceneExecutionState scene1Runtime = new SceneExecutionState(
                28L,
                31L,
                "scene-1",
                1,
                SceneExecutionStatus.COMPLETED,
                "candidate-1",
                "收到邀请",
                "决定是否重返游戏。",
                List.of("旧战队发来邀请"),
                List.of(),
                List.of(),
                Map.of("source", "phase8.accepted-scene-draft"),
                "他看着重新亮起的游戏图标。",
                "林沉舟重新想起旧战队。"
        );
        SceneExecutionState scene2Runtime = new SceneExecutionState(
                28L,
                31L,
                "scene-2",
                2,
                SceneExecutionStatus.COMPLETED,
                "candidate-2",
                "重新登录游戏",
                "进入游戏大厅。",
                List.of("林沉舟决定回归"),
                List.of(),
                List.of(),
                Map.of("source", "phase8.accepted-scene-draft"),
                "他躺进零界舱。",
                "林沉舟重新登录旧日王座。"
        );

        AtomicReference<ReaderRevealState> savedRevealState = new AtomicReference<>();
        AtomicReference<ChapterIncrementalState> savedChapterState = new AtomicReference<>();
        List<String> deletedSceneIds = new ArrayList<>();
        List<String> deletedOutgoingHandoffs = new ArrayList<>();

        AIWritingRecordMapper recordMapper = proxy(AIWritingRecordMapper.class, (proxy, method, args) -> switch (method.getName()) {
            case "findByChapterId" -> List.of(acceptedScene1, acceptedScene2);
            case "updateById" -> 1;
            default -> defaultValue(method.getReturnType());
        });
        ChapterService chapterService = proxy(ChapterService.class, (proxy, method, args) -> switch (method.getName()) {
            case "getById" -> currentChapter;
            case "list" -> List.of(previousChapter, currentChapter);
            case "updateById" -> true;
            default -> defaultValue(method.getReturnType());
        });
        KnowledgeDocumentService knowledgeDocumentService = proxy(KnowledgeDocumentService.class, (proxy, method, args) -> switch (method.getName()) {
            case "getOne" -> null;
            case "save", "updateById" -> true;
            default -> defaultValue(method.getReturnType());
        });
        SceneRuntimeStateStore sceneRuntimeStateStore = new SceneRuntimeStateStore() {
            private final Map<String, SceneExecutionState> stateMap = new LinkedHashMap<>(Map.of(
                    "scene-2", scene2Runtime
            ));

            @Override
            public Optional<SceneExecutionState> getSceneState(Long projectId, Long chapterId, String sceneId) {
                return Optional.ofNullable(stateMap.get(sceneId));
            }

            @Override
            public List<SceneExecutionState> listChapterScenes(Long projectId, Long chapterId) {
                return List.copyOf(stateMap.values());
            }

            @Override
            public SceneExecutionState saveSceneState(SceneExecutionState sceneExecutionState) {
                stateMap.put(sceneExecutionState.sceneId(), sceneExecutionState);
                return sceneExecutionState;
            }

            @Override
            public void deleteSceneState(Long projectId, Long chapterId, String sceneId) {
                deletedSceneIds.add(sceneId);
                stateMap.remove(sceneId);
            }

            @Override
            public Optional<com.storyweaver.storyunit.session.SceneHandoffSnapshot> findHandoffToScene(Long projectId, Long chapterId, String sceneId) {
                return Optional.empty();
            }

            @Override
            public List<com.storyweaver.storyunit.session.SceneHandoffSnapshot> listChapterHandoffs(Long projectId, Long chapterId) {
                return List.of();
            }

            @Override
            public com.storyweaver.storyunit.session.SceneHandoffSnapshot saveHandoff(com.storyweaver.storyunit.session.SceneHandoffSnapshot snapshot) {
                return snapshot;
            }

            @Override
            public void deleteHandoffsFromScene(Long projectId, Long chapterId, String sceneId) {
                deletedOutgoingHandoffs.add(sceneId);
            }

            @Override
            public void deleteHandoffsReferencingScene(Long projectId, Long chapterId, String sceneId) {
            }
        };
        ReaderRevealStateStore readerRevealStateStore = new ReaderRevealStateStore() {
            @Override
            public ReaderRevealState saveChapterRevealState(ReaderRevealState state) {
                savedRevealState.set(state);
                return state;
            }

            @Override
            public Optional<ReaderRevealState> findChapterRevealState(Long projectId, Long chapterId) {
                return Optional.empty();
            }
        };
        ChapterIncrementalStateStore chapterIncrementalStateStore = new ChapterIncrementalStateStore() {
            @Override
            public ChapterIncrementalState saveChapterState(ChapterIncrementalState state) {
                savedChapterState.set(state);
                return state;
            }

            @Override
            public Optional<ChapterIncrementalState> findChapterState(Long projectId, Long chapterId) {
                return Optional.empty();
            }
        };
        StoryContextQueryService storyContextQueryService = proxy(StoryContextQueryService.class, (proxy, method, args) -> switch (method.getName()) {
            case "getChapterAnchorBundle" -> Optional.of(new ChapterAnchorBundleView(
                    28L,
                    31L,
                    "退役者归来",
                    9L,
                    "第一卷",
                    15L,
                    "林沉舟",
                    List.of("林沉舟"),
                    List.of("旧日王座"),
                    List.of("回归旧战队"),
                    "主角回归旧战队"
            ));
            case "getCharacterRuntimeState" -> Optional.of(new CharacterRuntimeStateView(
                    28L,
                    15L,
                    "林沉舟",
                    "出租屋",
                    "压抑",
                    "迟疑",
                    List.of(),
                    List.of(),
                    List.of("观察中")
            ));
            default -> Optional.empty();
        });

        ChapterSkeletonPlanner planner = (projectId, chapterId) -> Optional.of(new ChapterSkeleton(
                28L,
                31L,
                "skeleton-31",
                3,
                "抵达集合点后停住。",
                List.of(scene1, scene2, scene3),
                List.of(),
                List.of()
        ));
        ChapterSceneWorkflowGuardService workflowGuardService = new ChapterSceneWorkflowGuardService(planner);

        ChapterWorkspaceAcceptedSceneRollbackService service = new ChapterWorkspaceAcceptedSceneRollbackService(
                recordMapper,
                chapterService,
                knowledgeDocumentService,
                workflowGuardService,
                sceneRuntimeStateStore,
                readerRevealStateStore,
                chapterIncrementalStateStore,
                storyContextQueryService,
                new ObjectMapper()
        );

        AIWritingRollbackResponseVO result = service.rollbackLatestAcceptedScene(31L);

        assertEquals(List.of("scene-2"), result.rolledBackSceneIds());
        assertEquals("scene-2", result.currentUnlockedSceneId());
        assertEquals("章节旧正文\n\nscene1-text\n\n人工补充", currentChapter.getContent());
        assertEquals("rolled_back", acceptedScene2.getStatus());
        assertTrue(acceptedScene2.getGenerationTraceJson().contains("\"rollback\""));
        assertEquals(List.of("上一章：旧战队已经重组。", "旧战队发来邀请"), savedRevealState.get().readerKnown());
        assertEquals(List.of("scene:scene-2:pending"), savedChapterState.get().openLoops());
        assertEquals(List.of("scene:scene-1:pending"), savedChapterState.get().resolvedLoops());
        assertEquals(List.of("scene-2"), deletedSceneIds);
        assertEquals(List.of("scene-2"), deletedOutgoingHandoffs);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0D;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == char.class) {
            return (char) 0;
        }
        return null;
    }
}
