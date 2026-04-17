package com.storyweaver.storyunit.context.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.storyweaver.domain.entity.AIWritingRecord;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.ProjectCharacterLink;
import com.storyweaver.domain.entity.ProjectWorldSettingLink;
import com.storyweaver.domain.entity.WorldSetting;
import com.storyweaver.repository.AIWritingRecordMapper;
import com.storyweaver.repository.ChapterMapper;
import com.storyweaver.repository.CharacterMapper;
import com.storyweaver.repository.ProjectCharacterMapper;
import com.storyweaver.repository.ProjectWorldSettingMapper;
import com.storyweaver.repository.WorldSettingMapper;
import com.storyweaver.storyunit.context.RecentStoryProgressItemView;
import com.storyweaver.storyunit.context.RecentStoryProgressQueryService;
import com.storyweaver.storyunit.context.RecentStoryProgressView;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DefaultRecentStoryProgressQueryService implements RecentStoryProgressQueryService {

    private final ChapterMapper chapterMapper;
    private final ProjectCharacterMapper projectCharacterMapper;
    private final CharacterMapper characterMapper;
    private final ProjectWorldSettingMapper projectWorldSettingMapper;
    private final WorldSettingMapper worldSettingMapper;
    private final AIWritingRecordMapper aiWritingRecordMapper;

    public DefaultRecentStoryProgressQueryService(
            ChapterMapper chapterMapper,
            ProjectCharacterMapper projectCharacterMapper,
            CharacterMapper characterMapper,
            ProjectWorldSettingMapper projectWorldSettingMapper,
            WorldSettingMapper worldSettingMapper,
            AIWritingRecordMapper aiWritingRecordMapper) {
        this.chapterMapper = chapterMapper;
        this.projectCharacterMapper = projectCharacterMapper;
        this.characterMapper = characterMapper;
        this.projectWorldSettingMapper = projectWorldSettingMapper;
        this.worldSettingMapper = worldSettingMapper;
        this.aiWritingRecordMapper = aiWritingRecordMapper;
    }

    @Override
    public RecentStoryProgressView getRecentStoryProgress(Long projectId, int limit) {
        if (projectId == null) {
            return new RecentStoryProgressView(null, List.of());
        }

        int safeLimit = Math.max(1, limit);
        List<RecentStoryProgressItemView> items = new ArrayList<>();
        items.addAll(loadChapterItems(projectId));
        items.addAll(loadCharacterItems(projectId));
        items.addAll(loadWorldSettingItems(projectId));
        items.addAll(loadGenerationItems(projectId));

        List<RecentStoryProgressItemView> sorted = items.stream()
                .sorted(Comparator.comparing(RecentStoryProgressItemView::createTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .toList();
        return new RecentStoryProgressView(projectId, sorted);
    }

    private List<RecentStoryProgressItemView> loadChapterItems(Long projectId) {
        return chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                        .eq(Chapter::getProjectId, projectId)
                        .eq(Chapter::getDeleted, 0)
                        .orderByDesc(Chapter::getCreateTime)
                        .last("LIMIT 10"))
                .stream()
                .map(chapter -> new RecentStoryProgressItemView(
                        "chapter",
                        chapter.getId(),
                        chapter.getTitle(),
                        ContextViewSupport.firstNonBlank(chapter.getSummary(), ContextViewSupport.truncate(chapter.getContent(), 80)),
                        ContextViewSupport.firstNonBlank(chapter.getChapterStatus(), chapter.getStatus() == null ? "" : chapter.getStatus().toString()),
                        firstTime(chapter.getUpdateTime(), chapter.getCreateTime())
                ))
                .toList();
    }

    private List<RecentStoryProgressItemView> loadCharacterItems(Long projectId) {
        List<ProjectCharacterLink> links = projectCharacterMapper.selectList(new LambdaQueryWrapper<ProjectCharacterLink>()
                .eq(ProjectCharacterLink::getProjectId, projectId)
                .orderByDesc(ProjectCharacterLink::getUpdateTime)
                .last("LIMIT 10"));
        if (links.isEmpty()) {
            return List.of();
        }
        List<Long> characterIds = links.stream()
                .map(ProjectCharacterLink::getCharacterId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, Character> characterMap = characterMapper.selectBatchIds(characterIds).stream()
                .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), Map::putAll);

        return links.stream()
                .map(link -> toCharacterItem(link, characterMap.get(link.getCharacterId())))
                .filter(item -> item != null)
                .toList();
    }

    private RecentStoryProgressItemView toCharacterItem(ProjectCharacterLink link, Character character) {
        if (character == null) {
            return null;
        }
        return new RecentStoryProgressItemView(
                "character",
                character.getId(),
                character.getName(),
                ContextViewSupport.firstNonBlank(character.getDescription(), character.getCoreGoal()),
                ContextViewSupport.firstNonBlank(link.getRoleType(), link.getProjectRole()),
                firstTime(link.getUpdateTime(), character.getUpdateTime(), character.getCreateTime())
        );
    }

    private List<RecentStoryProgressItemView> loadWorldSettingItems(Long projectId) {
        List<ProjectWorldSettingLink> links = projectWorldSettingMapper.selectList(new LambdaQueryWrapper<ProjectWorldSettingLink>()
                .eq(ProjectWorldSettingLink::getProjectId, projectId)
                .orderByDesc(ProjectWorldSettingLink::getUpdateTime)
                .last("LIMIT 10"));
        if (links.isEmpty()) {
            return List.of();
        }
        List<Long> worldSettingIds = links.stream()
                .map(ProjectWorldSettingLink::getWorldSettingId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, WorldSetting> worldSettingMap = worldSettingMapper.selectBatchIds(worldSettingIds).stream()
                .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), Map::putAll);

        return links.stream()
                .map(link -> toWorldSettingItem(link, worldSettingMap.get(link.getWorldSettingId())))
                .filter(item -> item != null)
                .toList();
    }

    private RecentStoryProgressItemView toWorldSettingItem(ProjectWorldSettingLink link, WorldSetting worldSetting) {
        if (worldSetting == null) {
            return null;
        }
        return new RecentStoryProgressItemView(
                "world_setting",
                worldSetting.getId(),
                ContextViewSupport.firstNonBlank(worldSetting.getName(), worldSetting.getTitle()),
                ContextViewSupport.firstNonBlank(worldSetting.getDescription(), worldSetting.getContent()),
                ContextViewSupport.firstNonBlank(worldSetting.getCategory(), "world_setting"),
                firstTime(link.getUpdateTime(), worldSetting.getUpdateTime(), worldSetting.getCreateTime())
        );
    }

    private List<RecentStoryProgressItemView> loadGenerationItems(Long projectId) {
        List<AIWritingRecord> records = aiWritingRecordMapper.findByProjectId(projectId);
        if (records.isEmpty()) {
            return List.of();
        }
        return records.stream()
                .limit(10)
                .map(record -> new RecentStoryProgressItemView(
                        "generation",
                        record.getId(),
                        "章节生成",
                        ContextViewSupport.firstNonBlank(record.getUserInstruction(), ContextViewSupport.truncate(record.getGeneratedContent(), 80)),
                        record.getStatus(),
                        firstTime(record.getUpdateTime(), record.getCreateTime())
                ))
                .toList();
    }

    private LocalDateTime firstTime(LocalDateTime... values) {
        if (values == null) {
            return null;
        }
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
