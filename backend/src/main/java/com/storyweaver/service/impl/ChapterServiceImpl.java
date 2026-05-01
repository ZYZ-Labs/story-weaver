package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.dto.ChapterRequestDTO;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.ChapterCharacterLink;
import com.storyweaver.domain.entity.ChapterPlotLink;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.Outline;
import com.storyweaver.domain.entity.Plot;
import com.storyweaver.domain.entity.ProjectCharacterLink;
import com.storyweaver.repository.ChapterCharacterMapper;
import com.storyweaver.repository.ChapterMapper;
import com.storyweaver.repository.ChapterPlotMapper;
import com.storyweaver.repository.CharacterMapper;
import com.storyweaver.repository.OutlineMapper;
import com.storyweaver.repository.PlotMapper;
import com.storyweaver.repository.ProjectCharacterMapper;
import com.storyweaver.service.ChapterService;
import com.storyweaver.service.ProjectService;
import com.storyweaver.story.generation.orchestration.ChapterNarrativeRuntimeMode;
import com.storyweaver.story.generation.orchestration.ChapterNarrativeRuntimeModeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChapterServiceImpl extends ServiceImpl<ChapterMapper, Chapter> implements ChapterService {

    private static final int WORDS_PER_MINUTE = 400;

    private final ProjectService projectService;
    private final ChapterCharacterMapper chapterCharacterMapper;
    private final ProjectCharacterMapper projectCharacterMapper;
    private final CharacterMapper characterMapper;
    private final OutlineMapper outlineMapper;
    private final PlotMapper plotMapper;
    private final ChapterPlotMapper chapterPlotMapper;
    private final ChapterNarrativeRuntimeModeService chapterNarrativeRuntimeModeService;

    public ChapterServiceImpl(
            ProjectService projectService,
            ChapterCharacterMapper chapterCharacterMapper,
            ProjectCharacterMapper projectCharacterMapper,
            CharacterMapper characterMapper,
            OutlineMapper outlineMapper,
            PlotMapper plotMapper,
            ChapterPlotMapper chapterPlotMapper,
            ChapterNarrativeRuntimeModeService chapterNarrativeRuntimeModeService) {
        this.projectService = projectService;
        this.chapterCharacterMapper = chapterCharacterMapper;
        this.projectCharacterMapper = projectCharacterMapper;
        this.characterMapper = characterMapper;
        this.outlineMapper = outlineMapper;
        this.plotMapper = plotMapper;
        this.chapterPlotMapper = chapterPlotMapper;
        this.chapterNarrativeRuntimeModeService = chapterNarrativeRuntimeModeService;
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
        attachWorkbenchMetadata(chapters);
        return chapters;
    }

    @Override
    @Transactional
    public Chapter createChapter(Long projectId, Long userId, ChapterRequestDTO requestDTO) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return null;
        }

        validateChapterReferences(projectId, null, requestDTO);

        Chapter chapter = new Chapter();
        chapter.setProjectId(projectId);
        applyRequest(chapter, requestDTO);
        save(chapter);

        syncRequiredCharacters(chapter, requestDTO.getRequiredCharacterIds());
        syncStoryBeats(chapter, requestDTO.getStoryBeatIds());
        syncOutlineBinding(chapter, null);
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

        validateChapterReferences(projectId, chapterId, requestDTO);

        Long previousOutlineId = existing.getOutlineId();
        applyRequest(existing, requestDTO);
        updateById(existing);

        syncRequiredCharacters(existing, requestDTO.getRequiredCharacterIds());
        syncStoryBeats(existing, requestDTO.getStoryBeatIds());
        syncOutlineBinding(existing, previousOutlineId);
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
        chapterPlotMapper.delete(new LambdaQueryWrapper<ChapterPlotLink>().eq(ChapterPlotLink::getChapterId, chapterId));
        clearOutlineBinding(chapterId, chapter.getOutlineId());
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
        attachWorkbenchMetadata(List.of(chapter));
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

    private void applyRequest(Chapter chapter, ChapterRequestDTO requestDTO) {
        if (StringUtils.hasText(requestDTO.getTitle())) {
            chapter.setTitle(requestDTO.getTitle().trim());
        }
        chapter.setSummary(trimToNull(requestDTO.getSummary()));
        chapter.setContent(requestDTO.getContent());
        chapter.setOrderNum(requestDTO.getOrderNum() != null ? requestDTO.getOrderNum() : chapter.getOrderNum());

        String chapterStatus = resolveChapterStatus(requestDTO.getChapterStatus(), requestDTO.getStatus(), chapter.getChapterStatus());
        chapter.setChapterStatus(chapterStatus);
        chapter.setStatus(resolveLegacyStatus(requestDTO.getStatus(), chapterStatus, chapter.getStatus()));
        chapter.setWordCount(requestDTO.getContent() == null ? 0 : requestDTO.getContent().length());
        chapter.setOutlineId(requestDTO.getOutlineId());
        chapter.setPrevChapterId(requestDTO.getPrevChapterId());
        chapter.setNextChapterId(requestDTO.getNextChapterId());
        chapter.setMainPovCharacterId(requestDTO.getMainPovCharacterId());
    }

    private void validateChapterReferences(Long projectId, Long chapterId, ChapterRequestDTO requestDTO) {
        if (requestDTO.getOutlineId() != null) {
            Outline outline = outlineMapper.selectById(requestDTO.getOutlineId());
            if (outline == null
                    || Integer.valueOf(1).equals(outline.getDeleted())
                    || !Objects.equals(outline.getProjectId(), projectId)) {
                throw new IllegalArgumentException("关联大纲不存在或不属于当前项目");
            }
        }

        Set<Long> projectCharacterIds = new LinkedHashSet<>();
        if (requestDTO.getRequiredCharacterIds() != null) {
            projectCharacterIds.addAll(requestDTO.getRequiredCharacterIds().stream()
                    .filter(Objects::nonNull)
                    .toList());
        }
        if (requestDTO.getMainPovCharacterId() != null) {
            projectCharacterIds.add(requestDTO.getMainPovCharacterId());
        }
        if (!projectCharacterIds.isEmpty()) {
            long linkedCharacterCount = projectCharacterMapper.selectCount(new LambdaQueryWrapper<ProjectCharacterLink>()
                    .eq(ProjectCharacterLink::getProjectId, projectId)
                    .in(ProjectCharacterLink::getCharacterId, projectCharacterIds));
            if (linkedCharacterCount != projectCharacterIds.size()) {
                throw new IllegalArgumentException("章节人物或 POV 人物必须先关联到当前项目");
            }
        }

        Set<Long> storyBeatIds = normalizeIds(requestDTO.getStoryBeatIds());
        if (!storyBeatIds.isEmpty()) {
            long plotCount = plotMapper.selectCount(new LambdaQueryWrapper<Plot>()
                    .eq(Plot::getProjectId, projectId)
                    .eq(Plot::getDeleted, 0)
                    .in(Plot::getId, storyBeatIds));
            if (plotCount != storyBeatIds.size()) {
                throw new IllegalArgumentException("存在不属于当前项目的剧情节点");
            }
        }

        validateNeighborChapter(projectId, chapterId, requestDTO.getPrevChapterId(), "上一章节");
        validateNeighborChapter(projectId, chapterId, requestDTO.getNextChapterId(), "下一章节");
    }

    private void validateNeighborChapter(Long projectId, Long currentChapterId, Long targetChapterId, String label) {
        if (targetChapterId == null) {
            return;
        }
        if (Objects.equals(currentChapterId, targetChapterId)) {
            throw new IllegalArgumentException(label + "不能是当前章节本身");
        }
        Chapter chapter = getById(targetChapterId);
        if (chapter == null
                || Integer.valueOf(1).equals(chapter.getDeleted())
                || !Objects.equals(chapter.getProjectId(), projectId)) {
            throw new IllegalArgumentException(label + "不存在或不属于当前项目");
        }
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
                ? Collections.emptyMap()
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

    private void attachWorkbenchMetadata(List<Chapter> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            return;
        }

        Map<Long, ChapterNarrativeRuntimeMode> runtimeModes = chapterNarrativeRuntimeModeService.getModes(chapters);

        List<Long> chapterIds = chapters.stream()
                .map(Chapter::getId)
                .filter(Objects::nonNull)
                .toList();
        if (chapterIds.isEmpty()) {
            return;
        }

        Set<Long> outlineIds = chapters.stream()
                .map(Chapter::getOutlineId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> povCharacterIds = chapters.stream()
                .map(Chapter::getMainPovCharacterId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, Outline> outlineMap = outlineIds.isEmpty()
                ? Collections.emptyMap()
                : outlineMapper.selectBatchIds(outlineIds).stream()
                        .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                        .collect(Collectors.toMap(Outline::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        Map<Long, Outline> outlineByChapterId = outlineMapper.selectList(new LambdaQueryWrapper<Outline>()
                        .in(Outline::getChapterId, chapterIds)
                        .eq(Outline::getDeleted, 0))
                .stream()
                .collect(Collectors.toMap(
                        Outline::getChapterId,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<ChapterPlotLink> chapterPlotLinks = chapterPlotMapper.selectList(new LambdaQueryWrapper<ChapterPlotLink>()
                .in(ChapterPlotLink::getChapterId, chapterIds)
                .orderByAsc(ChapterPlotLink::getSortOrder)
                .orderByAsc(ChapterPlotLink::getId));

        Set<Long> plotIds = chapterPlotLinks.stream()
                .map(ChapterPlotLink::getPlotId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Plot> fallbackPlots = plotMapper.selectList(new LambdaQueryWrapper<Plot>()
                .in(Plot::getChapterId, chapterIds)
                .eq(Plot::getDeleted, 0)
                .orderByAsc(Plot::getSequence)
                .orderByAsc(Plot::getId));
        plotIds.addAll(fallbackPlots.stream()
                .map(Plot::getId)
                .filter(Objects::nonNull)
                .toList());

        Map<Long, Plot> plotMap = plotIds.isEmpty()
                ? Collections.emptyMap()
                : plotMapper.selectBatchIds(plotIds).stream()
                        .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                        .collect(Collectors.toMap(Plot::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        Map<Long, List<Long>> storyBeatIdsByChapter = new LinkedHashMap<>();
        Map<Long, List<String>> storyBeatTitlesByChapter = new LinkedHashMap<>();
        for (ChapterPlotLink link : chapterPlotLinks) {
            Plot plot = plotMap.get(link.getPlotId());
            if (plot == null) {
                continue;
            }
            storyBeatIdsByChapter.computeIfAbsent(link.getChapterId(), key -> new ArrayList<>()).add(plot.getId());
            storyBeatTitlesByChapter.computeIfAbsent(link.getChapterId(), key -> new ArrayList<>()).add(resolvePlotTitle(plot));
        }
        for (Plot plot : fallbackPlots) {
            if (storyBeatIdsByChapter.containsKey(plot.getChapterId())) {
                continue;
            }
            storyBeatIdsByChapter.computeIfAbsent(plot.getChapterId(), key -> new ArrayList<>()).add(plot.getId());
            storyBeatTitlesByChapter.computeIfAbsent(plot.getChapterId(), key -> new ArrayList<>()).add(resolvePlotTitle(plot));
        }

        Map<Long, String> povCharacterNameMap = povCharacterIds.isEmpty()
                ? Collections.emptyMap()
                : characterMapper.selectBatchIds(povCharacterIds).stream()
                        .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                        .collect(Collectors.toMap(Character::getId, Character::getName, (left, right) -> left, LinkedHashMap::new));

        for (Chapter chapter : chapters) {
            Outline explicitOutline = chapter.getOutlineId() == null ? null : outlineMap.get(chapter.getOutlineId());
            Outline fallbackOutline = outlineByChapterId.get(chapter.getId());
            Outline resolvedOutline = explicitOutline != null ? explicitOutline : fallbackOutline;

            if (chapter.getOutlineId() == null && resolvedOutline != null) {
                chapter.setOutlineId(resolvedOutline.getId());
            }
            chapter.setOutlineTitle(resolvedOutline == null ? null : resolvedOutline.getTitle());
            chapter.setStoryBeatIds(storyBeatIdsByChapter.getOrDefault(chapter.getId(), List.of()));
            chapter.setStoryBeatTitles(storyBeatTitlesByChapter.getOrDefault(chapter.getId(), List.of()));
            chapter.setMainPovCharacterName(
                    chapter.getMainPovCharacterId() == null ? null : povCharacterNameMap.get(chapter.getMainPovCharacterId())
            );
            chapter.setReadingTimeMinutes(resolveReadingTimeMinutes(chapter.getWordCount()));
            chapter.setNarrativeRuntimeMode(runtimeModes.getOrDefault(chapter.getId(), ChapterNarrativeRuntimeMode.SCENE).apiValue());
            if (!StringUtils.hasText(chapter.getChapterStatus())) {
                chapter.setChapterStatus(resolveChapterStatus(null, chapter.getStatus(), null));
            }
        }
    }

    private void syncRequiredCharacters(Chapter chapter, List<Long> requiredCharacterIds) {
        Set<Long> targetIds = normalizeIds(requiredCharacterIds);
        if (!targetIds.isEmpty()) {
            long projectCharacterCount = projectCharacterMapper.selectCount(new QueryWrapper<ProjectCharacterLink>()
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

    private void syncStoryBeats(Chapter chapter, List<Long> storyBeatIds) {
        List<ChapterPlotLink> existingLinks = chapterPlotMapper.selectList(new LambdaQueryWrapper<ChapterPlotLink>()
                .eq(ChapterPlotLink::getChapterId, chapter.getId())
                .orderByAsc(ChapterPlotLink::getSortOrder)
                .orderByAsc(ChapterPlotLink::getId));
        Set<Long> previousPlotIds = existingLinks.stream()
                .map(ChapterPlotLink::getPlotId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> targetIds = normalizeIds(storyBeatIds);
        chapterPlotMapper.delete(new LambdaQueryWrapper<ChapterPlotLink>()
                .eq(ChapterPlotLink::getChapterId, chapter.getId()));

        int sortOrder = 0;
        for (Long plotId : targetIds) {
            ChapterPlotLink link = new ChapterPlotLink();
            link.setChapterId(chapter.getId());
            link.setPlotId(plotId);
            link.setRelationType("primary");
            link.setSortOrder(sortOrder++);
            chapterPlotMapper.insert(link);

            Plot plot = plotMapper.selectById(plotId);
            if (plot != null && !Integer.valueOf(1).equals(plot.getDeleted())) {
                plot.setChapterId(chapter.getId());
                plotMapper.updateById(plot);
            }
        }

        previousPlotIds.removeAll(targetIds);
        for (Long removedPlotId : previousPlotIds) {
            Plot plot = plotMapper.selectById(removedPlotId);
            if (plot != null && Objects.equals(plot.getChapterId(), chapter.getId())) {
                plot.setChapterId(null);
                plotMapper.updateById(plot);
            }
        }
    }

    private void syncOutlineBinding(Chapter chapter, Long previousOutlineId) {
        if (previousOutlineId != null && !Objects.equals(previousOutlineId, chapter.getOutlineId())) {
            clearOutlineBinding(chapter.getId(), previousOutlineId);
        }
        if (chapter.getOutlineId() == null) {
            return;
        }

        Outline outline = outlineMapper.selectById(chapter.getOutlineId());
        if (outline == null || Integer.valueOf(1).equals(outline.getDeleted())) {
            return;
        }
        outline.setChapterId(chapter.getId());
        outline.setGeneratedChapterId(chapter.getId());
        if (!StringUtils.hasText(outline.getOutlineType())) {
            outline.setOutlineType("chapter");
        }
        if (outline.getRootOutlineId() == null) {
            outline.setRootOutlineId(outline.getId());
        }
        outlineMapper.updateById(outline);
    }

    private void clearOutlineBinding(Long chapterId, Long outlineId) {
        if (outlineId == null) {
            return;
        }
        Outline outline = outlineMapper.selectById(outlineId);
        if (outline == null || Integer.valueOf(1).equals(outline.getDeleted())) {
            return;
        }
        if (Objects.equals(outline.getChapterId(), chapterId)) {
            outline.setChapterId(null);
        }
        if (Objects.equals(outline.getGeneratedChapterId(), chapterId)) {
            outline.setGeneratedChapterId(null);
        }
        outlineMapper.updateById(outline);
    }

    private Set<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String resolveChapterStatus(String requestedStatus, Integer legacyStatus, String currentStatus) {
        if (StringUtils.hasText(requestedStatus)) {
            return requestedStatus.trim();
        }
        if (legacyStatus == null) {
            return StringUtils.hasText(currentStatus) ? currentStatus.trim() : "draft";
        }
        return switch (legacyStatus) {
            case 1 -> "review";
            case 2 -> "published";
            default -> "draft";
        };
    }

    private Integer resolveLegacyStatus(Integer requestedStatus, String chapterStatus, Integer currentStatus) {
        if (requestedStatus != null) {
            return requestedStatus;
        }
        if (!StringUtils.hasText(chapterStatus)) {
            return currentStatus == null ? 0 : currentStatus;
        }
        return switch (chapterStatus.trim()) {
            case "review", "polishing" -> 1;
            case "published", "final", "archived" -> 2;
            default -> 0;
        };
    }

    private String resolvePlotTitle(Plot plot) {
        if (plot == null) {
            return null;
        }
        return StringUtils.hasText(plot.getTitle()) ? plot.getTitle().trim() : "剧情 #" + plot.getId();
    }

    private Integer resolveReadingTimeMinutes(Integer wordCount) {
        if (wordCount == null || wordCount <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(wordCount / (double) WORDS_PER_MINUTE));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
