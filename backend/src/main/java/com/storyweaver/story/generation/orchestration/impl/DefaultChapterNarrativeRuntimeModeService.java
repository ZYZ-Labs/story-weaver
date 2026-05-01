package com.storyweaver.story.generation.orchestration.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.storyweaver.config.StoryCompatibilityProperties;
import com.storyweaver.domain.entity.AIWritingRecord;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.SystemConfig;
import com.storyweaver.exception.SceneWorkflowConflictException;
import com.storyweaver.repository.AIWritingRecordMapper;
import com.storyweaver.service.SystemConfigService;
import com.storyweaver.story.generation.orchestration.ChapterNarrativeRuntimeMode;
import com.storyweaver.story.generation.orchestration.ChapterNarrativeRuntimeModeService;
import com.storyweaver.storyunit.service.StoryNodeCheckpointStore;
import com.storyweaver.storyunit.service.StoryResolvedTurnStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DefaultChapterNarrativeRuntimeModeService implements ChapterNarrativeRuntimeModeService {

    private static final String CHAPTER_WORKSPACE_ENTRY_POINT = "phase8.chapter-workspace.scene-draft";
    private static final String CONFIG_DESCRIPTION = "章节工作区叙事运行模式（scene/node）";

    private final SystemConfigService systemConfigService;
    private final AIWritingRecordMapper aiWritingRecordMapper;
    private final StoryResolvedTurnStore storyResolvedTurnStore;
    private final StoryNodeCheckpointStore storyNodeCheckpointStore;
    private final StoryCompatibilityProperties storyCompatibilityProperties;

    public DefaultChapterNarrativeRuntimeModeService(
            SystemConfigService systemConfigService,
            AIWritingRecordMapper aiWritingRecordMapper,
            StoryResolvedTurnStore storyResolvedTurnStore,
            StoryNodeCheckpointStore storyNodeCheckpointStore,
            StoryCompatibilityProperties storyCompatibilityProperties) {
        this.systemConfigService = systemConfigService;
        this.aiWritingRecordMapper = aiWritingRecordMapper;
        this.storyResolvedTurnStore = storyResolvedTurnStore;
        this.storyNodeCheckpointStore = storyNodeCheckpointStore;
        this.storyCompatibilityProperties = storyCompatibilityProperties;
    }

    @Override
    public ChapterNarrativeRuntimeMode getMode(Long projectId, Long chapterId) {
        if (projectId == null || chapterId == null) {
            return ChapterNarrativeRuntimeMode.SCENE;
        }
        String value = systemConfigService.getConfigValue(configKey(projectId, chapterId));
        return ChapterNarrativeRuntimeMode.fromValue(value);
    }

    @Override
    public Map<Long, ChapterNarrativeRuntimeMode> getModes(List<Chapter> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            return Map.of();
        }

        Map<Long, ChapterNarrativeRuntimeMode> modes = new LinkedHashMap<>();
        Map<String, Long> chapterIdsByConfigKey = new LinkedHashMap<>();
        for (Chapter chapter : chapters) {
            if (chapter == null || chapter.getId() == null || chapter.getProjectId() == null) {
                continue;
            }
            chapterIdsByConfigKey.put(configKey(chapter.getProjectId(), chapter.getId()), chapter.getId());
        }
        if (chapterIdsByConfigKey.isEmpty()) {
            return modes;
        }

        List<SystemConfig> configs = systemConfigService.list(new LambdaQueryWrapper<SystemConfig>()
                .in(SystemConfig::getConfigKey, chapterIdsByConfigKey.keySet()));
        Map<String, String> configValues = configs.stream()
                .filter(item -> StringUtils.hasText(item.getConfigKey()))
                .collect(Collectors.toMap(SystemConfig::getConfigKey, SystemConfig::getConfigValue, (left, right) -> right, LinkedHashMap::new));

        for (Map.Entry<String, Long> entry : chapterIdsByConfigKey.entrySet()) {
            modes.put(entry.getValue(), ChapterNarrativeRuntimeMode.fromValue(configValues.get(entry.getKey())));
        }
        return modes;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChapterNarrativeRuntimeMode updateMode(Chapter chapter, ChapterNarrativeRuntimeMode targetMode) {
        if (chapter == null || chapter.getId() == null || chapter.getProjectId() == null) {
            throw new IllegalArgumentException("章节缺少有效的项目或章节信息，无法切换运行模式。");
        }
        ChapterNarrativeRuntimeMode resolvedTargetMode = targetMode == null ? ChapterNarrativeRuntimeMode.SCENE : targetMode;
        ChapterNarrativeRuntimeMode currentMode = getMode(chapter.getProjectId(), chapter.getId());
        if (currentMode == resolvedTargetMode) {
            return currentMode;
        }

        validateSwitch(chapter, currentMode, resolvedTargetMode);
        if (resolvedTargetMode == ChapterNarrativeRuntimeMode.SCENE) {
            systemConfigService.remove(new QueryWrapper<SystemConfig>()
                    .eq("config_key", configKey(chapter.getProjectId(), chapter.getId())));
            return ChapterNarrativeRuntimeMode.SCENE;
        }

        SystemConfig config = new SystemConfig();
        config.setConfigKey(configKey(chapter.getProjectId(), chapter.getId()));
        config.setConfigValue(resolvedTargetMode.apiValue());
        config.setDescription(CONFIG_DESCRIPTION);
        saveOrUpdateConfig(config);
        return resolvedTargetMode;
    }

    @Override
    public void assertSceneMode(Long projectId, Long chapterId, String actionLabel) {
        assertMode(getMode(projectId, chapterId), ChapterNarrativeRuntimeMode.SCENE, actionLabel);
    }

    @Override
    public void assertSceneMode(Chapter chapter, String actionLabel) {
        if (chapter == null) {
            throw new IllegalArgumentException("章节不存在，无法校验运行模式。");
        }
        assertMode(getMode(chapter.getProjectId(), chapter.getId()), ChapterNarrativeRuntimeMode.SCENE, actionLabel);
    }

    @Override
    public void assertNodeMode(Long projectId, Long chapterId, String actionLabel) {
        assertMode(getMode(projectId, chapterId), ChapterNarrativeRuntimeMode.NODE, actionLabel);
    }

    @Override
    public void assertNodeMode(Chapter chapter, String actionLabel) {
        if (chapter == null) {
            throw new IllegalArgumentException("章节不存在，无法校验运行模式。");
        }
        assertMode(getMode(chapter.getProjectId(), chapter.getId()), ChapterNarrativeRuntimeMode.NODE, actionLabel);
    }

    private void validateSwitch(
            Chapter chapter,
            ChapterNarrativeRuntimeMode currentMode,
            ChapterNarrativeRuntimeMode targetMode) {
        if (targetMode == ChapterNarrativeRuntimeMode.NODE) {
            if (!storyCompatibilityProperties.isChapterWorkspaceNodeResolveEnabled()) {
                throw new SceneWorkflowConflictException("node runtime 推进兼容开关当前未打开，暂时不能把章节切到 node mode。");
            }
            if (hasAcceptedSceneProgress(chapter.getId())) {
                throw new SceneWorkflowConflictException("当前章节已经存在已接纳的 scene 写作链，请先撤回已接纳镜头，再切到 node mode。");
            }
            return;
        }

        if (currentMode == ChapterNarrativeRuntimeMode.NODE && hasNodeRuntimeProgress(chapter.getProjectId(), chapter.getId())) {
            throw new SceneWorkflowConflictException("当前章节已经存在 node runtime checkpoint / turn，暂时不能直接切回 scene mode。");
        }
    }

    private void assertMode(
            ChapterNarrativeRuntimeMode currentMode,
            ChapterNarrativeRuntimeMode expectedMode,
            String actionLabel) {
        if (currentMode == expectedMode) {
            return;
        }
        String modeLabel = expectedMode == ChapterNarrativeRuntimeMode.SCENE ? "scene mode" : "node mode";
        throw new SceneWorkflowConflictException("当前章节已切换到 " + currentMode.apiValue() + " mode，不能继续" + actionLabel + "。请先切回 " + modeLabel + "。");
    }

    private boolean hasAcceptedSceneProgress(Long chapterId) {
        if (chapterId == null) {
            return false;
        }
        List<AIWritingRecord> records = aiWritingRecordMapper.findByChapterId(chapterId);
        return records.stream().anyMatch(this::isAcceptedSceneRecord);
    }

    private boolean isAcceptedSceneRecord(AIWritingRecord record) {
        if (record == null) {
            return false;
        }
        if (!"accepted".equalsIgnoreCase(normalizeText(record.getStatus()))) {
            return false;
        }
        String trace = normalizeText(record.getGenerationTraceJson());
        if (!trace.contains(CHAPTER_WORKSPACE_ENTRY_POINT)) {
            return false;
        }
        return true;
    }

    private boolean hasNodeRuntimeProgress(Long projectId, Long chapterId) {
        if (projectId == null || chapterId == null) {
            return false;
        }
        return !storyResolvedTurnStore.listChapterTurns(projectId, chapterId).isEmpty()
                || !storyNodeCheckpointStore.listChapterCheckpoints(projectId, chapterId).isEmpty();
    }

    private void saveOrUpdateConfig(SystemConfig incoming) {
        SystemConfig existing = systemConfigService.getOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, incoming.getConfigKey()), false);
        if (existing == null) {
            systemConfigService.save(incoming);
            return;
        }
        existing.setConfigValue(incoming.getConfigValue());
        existing.setDescription(StringUtils.hasText(incoming.getDescription()) ? incoming.getDescription() : existing.getDescription());
        systemConfigService.updateById(existing);
    }

    private String configKey(Long projectId, Long chapterId) {
        return "chapter.runtime.mode." + projectId + "." + chapterId;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
