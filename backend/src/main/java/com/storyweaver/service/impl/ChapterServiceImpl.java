package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.dto.ChapterRequestDTO;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.ChapterCharacterLink;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.repository.ChapterCharacterMapper;
import com.storyweaver.repository.ChapterMapper;
import com.storyweaver.repository.CharacterMapper;
import com.storyweaver.repository.ProjectCharacterMapper;
import com.storyweaver.service.ChapterService;
import com.storyweaver.service.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChapterServiceImpl extends ServiceImpl<ChapterMapper, Chapter> implements ChapterService {

    private final ProjectService projectService;
    private final ChapterCharacterMapper chapterCharacterMapper;
    private final ProjectCharacterMapper projectCharacterMapper;
    private final CharacterMapper characterMapper;

    public ChapterServiceImpl(
            ProjectService projectService,
            ChapterCharacterMapper chapterCharacterMapper,
            ProjectCharacterMapper projectCharacterMapper,
            CharacterMapper characterMapper) {
        this.projectService = projectService;
        this.chapterCharacterMapper = chapterCharacterMapper;
        this.projectCharacterMapper = projectCharacterMapper;
        this.characterMapper = characterMapper;
    }

    @Override
    public List<Chapter> getProjectChapters(Long projectId, Long userId) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return List.of();
        }

        List<Chapter> chapters = list(new QueryWrapper<Chapter>()
                .eq("project_id", projectId)
                .eq("deleted", 0)
                .orderByAsc("order_num")
                .orderByAsc("create_time"));
        attachRequiredCharacters(chapters);
        return chapters;
    }

    @Override
    @Transactional
    public Chapter createChapter(Long projectId, Long userId, ChapterRequestDTO requestDTO) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return null;
        }

        Chapter chapter = new Chapter();
        chapter.setProjectId(projectId);
        chapter.setTitle(requestDTO.getTitle().trim());
        chapter.setContent(requestDTO.getContent());
        chapter.setOrderNum(requestDTO.getOrderNum() != null ? requestDTO.getOrderNum() : 0);
        chapter.setStatus(requestDTO.getStatus() != null ? requestDTO.getStatus() : 0);
        chapter.setWordCount(requestDTO.getContent() == null ? 0 : requestDTO.getContent().length());
        save(chapter);
        syncRequiredCharacters(chapter, requestDTO.getRequiredCharacterIds());
        return getChapterWithAuth(chapter.getId(), userId);
    }

    @Override
    @Transactional
    public boolean updateChapter(Long projectId, Long chapterId, Long userId, ChapterRequestDTO requestDTO) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return false;
        }

        Chapter existing = getChapterWithAuth(chapterId, userId);
        if (existing == null || !Objects.equals(existing.getProjectId(), projectId)) {
            return false;
        }

        if (StringUtils.hasText(requestDTO.getTitle())) {
            existing.setTitle(requestDTO.getTitle().trim());
        }
        existing.setContent(requestDTO.getContent());
        existing.setOrderNum(requestDTO.getOrderNum() != null ? requestDTO.getOrderNum() : existing.getOrderNum());
        existing.setStatus(requestDTO.getStatus() != null ? requestDTO.getStatus() : existing.getStatus());
        existing.setWordCount(requestDTO.getContent() == null ? 0 : requestDTO.getContent().length());
        updateById(existing);
        syncRequiredCharacters(existing, requestDTO.getRequiredCharacterIds());
        return true;
    }

    @Override
    @Transactional
    public boolean deleteChapter(Long chapterId, Long userId) {
        Chapter chapter = getChapterWithAuth(chapterId, userId);
        if (chapter == null) {
            return false;
        }

        chapterCharacterMapper.delete(new QueryWrapper<ChapterCharacterLink>().eq("chapter_id", chapterId));
        return removeById(chapterId);
    }

    @Override
    public Chapter getChapterWithAuth(Long chapterId, Long userId) {
        Chapter chapter = getById(chapterId);
        if (chapter == null || Integer.valueOf(1).equals(chapter.getDeleted())) {
            return null;
        }
        if (!projectService.hasProjectAccess(chapter.getProjectId(), userId)) {
            return null;
        }

        attachRequiredCharacters(List.of(chapter));
        return chapter;
    }

    @Override
    public List<String> getRequiredCharacterNames(Long chapterId) {
        List<ChapterCharacterLink> links = chapterCharacterMapper.selectList(new QueryWrapper<ChapterCharacterLink>()
                .eq("chapter_id", chapterId)
                .eq("required_flag", 1)
                .orderByAsc("id"));
        if (links.isEmpty()) {
            return List.of();
        }

        Map<Long, Character> characterMap = characterMapper.selectBatchIds(
                links.stream().map(ChapterCharacterLink::getCharacterId).toList()
        ).stream()
                .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                .collect(Collectors.toMap(Character::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<String> names = new ArrayList<>();
        for (ChapterCharacterLink link : links) {
            Character character = characterMap.get(link.getCharacterId());
            if (character != null && StringUtils.hasText(character.getName())) {
                names.add(character.getName());
            }
        }
        return names;
    }

    private void attachRequiredCharacters(List<Chapter> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            return;
        }

        List<Long> chapterIds = chapters.stream()
                .map(Chapter::getId)
                .filter(Objects::nonNull)
                .toList();
        if (chapterIds.isEmpty()) {
            return;
        }

        List<ChapterCharacterLink> links = chapterCharacterMapper.selectList(new QueryWrapper<ChapterCharacterLink>()
                .in("chapter_id", chapterIds)
                .eq("required_flag", 1)
                .orderByAsc("id"));

        Set<Long> characterIds = links.stream()
                .map(ChapterCharacterLink::getCharacterId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, Character> characterMap = characterIds.isEmpty()
                ? Map.of()
                : characterMapper.selectBatchIds(characterIds).stream()
                        .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                        .collect(Collectors.toMap(Character::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        Map<Long, List<Long>> requiredIdsByChapter = new LinkedHashMap<>();
        Map<Long, List<String>> requiredNamesByChapter = new LinkedHashMap<>();
        for (ChapterCharacterLink link : links) {
            Character character = characterMap.get(link.getCharacterId());
            if (character == null) {
                continue;
            }
            requiredIdsByChapter.computeIfAbsent(link.getChapterId(), key -> new ArrayList<>()).add(character.getId());
            requiredNamesByChapter.computeIfAbsent(link.getChapterId(), key -> new ArrayList<>()).add(character.getName());
        }

        for (Chapter chapter : chapters) {
            chapter.setRequiredCharacterIds(requiredIdsByChapter.getOrDefault(chapter.getId(), List.of()));
            chapter.setRequiredCharacterNames(requiredNamesByChapter.getOrDefault(chapter.getId(), List.of()));
        }
    }

    private void syncRequiredCharacters(Chapter chapter, List<Long> requiredCharacterIds) {
        Set<Long> targetIds = normalizeIds(requiredCharacterIds);
        if (!targetIds.isEmpty()) {
            long projectCharacterCount = projectCharacterMapper.selectCount(new QueryWrapper<com.storyweaver.domain.entity.ProjectCharacterLink>()
                    .eq("project_id", chapter.getProjectId())
                    .in("character_id", targetIds));
            if (projectCharacterCount != targetIds.size()) {
                throw new IllegalArgumentException("本章必出人物必须先关联到当前项目");
            }
        }

        chapterCharacterMapper.delete(new QueryWrapper<ChapterCharacterLink>().eq("chapter_id", chapter.getId()));
        for (Long characterId : targetIds) {
            ChapterCharacterLink link = new ChapterCharacterLink();
            link.setChapterId(chapter.getId());
            link.setCharacterId(characterId);
            link.setRequiredFlag(1);
            chapterCharacterMapper.insert(link);
        }
    }

    private Set<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
