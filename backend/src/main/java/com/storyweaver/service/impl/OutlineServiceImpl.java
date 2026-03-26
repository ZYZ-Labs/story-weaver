package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.dto.OutlineRequestDTO;
import com.storyweaver.domain.entity.Causality;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.Outline;
import com.storyweaver.domain.entity.Plot;
import com.storyweaver.domain.entity.ProjectCharacterLink;
import com.storyweaver.repository.CausalityMapper;
import com.storyweaver.repository.ChapterMapper;
import com.storyweaver.repository.CharacterMapper;
import com.storyweaver.repository.OutlineMapper;
import com.storyweaver.repository.PlotMapper;
import com.storyweaver.repository.ProjectCharacterMapper;
import com.storyweaver.service.OutlineService;
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
public class OutlineServiceImpl extends ServiceImpl<OutlineMapper, Outline> implements OutlineService {

    private final ProjectService projectService;
    private final ChapterMapper chapterMapper;
    private final CharacterMapper characterMapper;
    private final ProjectCharacterMapper projectCharacterMapper;
    private final PlotMapper plotMapper;
    private final CausalityMapper causalityMapper;

    public OutlineServiceImpl(
            ProjectService projectService,
            ChapterMapper chapterMapper,
            CharacterMapper characterMapper,
            ProjectCharacterMapper projectCharacterMapper,
            PlotMapper plotMapper,
            CausalityMapper causalityMapper) {
        this.projectService = projectService;
        this.chapterMapper = chapterMapper;
        this.characterMapper = characterMapper;
        this.projectCharacterMapper = projectCharacterMapper;
        this.plotMapper = plotMapper;
        this.causalityMapper = causalityMapper;
    }

    @Override
    public List<Outline> getProjectOutlines(Long projectId, Long userId) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return List.of();
        }
        List<Outline> outlines = baseMapper.findByProjectId(projectId);
        attachReadableRelations(outlines);
        return outlines;
    }

    @Override
    public Outline getOutlineWithAuth(Long outlineId, Long userId) {
        Outline outline = getById(outlineId);
        if (outline == null || Integer.valueOf(1).equals(outline.getDeleted())) {
            return null;
        }
        if (!projectService.hasProjectAccess(outline.getProjectId(), userId)) {
            return null;
        }
        attachReadableRelations(List.of(outline));
        return outline;
    }

    @Override
    @Transactional
    public Outline createOutline(Long projectId, Long userId, OutlineRequestDTO requestDTO) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return null;
        }

        validateRelations(projectId, requestDTO);

        Outline outline = new Outline();
        applyRequest(outline, projectId, requestDTO);
        save(outline);
        return getOutlineWithAuth(outline.getId(), userId);
    }

    @Override
    @Transactional
    public boolean updateOutline(Long projectId, Long outlineId, Long userId, OutlineRequestDTO requestDTO) {
        Outline existing = getOutlineWithAuth(outlineId, userId);
        if (existing == null || !Objects.equals(existing.getProjectId(), projectId)) {
            return false;
        }

        validateRelations(projectId, requestDTO);
        applyRequest(existing, projectId, requestDTO);
        return updateById(existing);
    }

    @Override
    @Transactional
    public boolean deleteOutline(Long outlineId, Long userId) {
        Outline outline = getOutlineWithAuth(outlineId, userId);
        if (outline == null) {
            return false;
        }
        return removeById(outlineId);
    }

    private void applyRequest(Outline outline, Long projectId, OutlineRequestDTO requestDTO) {
        outline.setProjectId(projectId);
        outline.setChapterId(requestDTO.getChapterId());
        outline.setTitle(trimToNull(requestDTO.getTitle()));
        outline.setSummary(trimToNull(requestDTO.getSummary()));
        outline.setContent(trimToNull(requestDTO.getContent()));
        outline.setStageGoal(trimToNull(requestDTO.getStageGoal()));
        outline.setKeyConflict(trimToNull(requestDTO.getKeyConflict()));
        outline.setTurningPoints(trimToNull(requestDTO.getTurningPoints()));
        outline.setExpectedEnding(trimToNull(requestDTO.getExpectedEnding()));
        outline.setFocusCharacterIds(joinIds(requestDTO.getFocusCharacterIds()));
        outline.setRelatedPlotIds(joinIds(requestDTO.getRelatedPlotIds()));
        outline.setRelatedCausalityIds(joinIds(requestDTO.getRelatedCausalityIds()));
        outline.setStatus(requestDTO.getStatus() != null ? requestDTO.getStatus() : 0);
        outline.setOrderNum(resolveOrderNum(projectId, requestDTO));
    }

    private Integer resolveOrderNum(Long projectId, OutlineRequestDTO requestDTO) {
        if (requestDTO.getOrderNum() != null) {
            return requestDTO.getOrderNum();
        }
        if (requestDTO.getChapterId() != null) {
            Chapter chapter = chapterMapper.selectById(requestDTO.getChapterId());
            if (chapter != null && !Integer.valueOf(1).equals(chapter.getDeleted()) && chapter.getOrderNum() != null) {
                return chapter.getOrderNum();
            }
        }
        long existingCount = count(new LambdaQueryWrapper<Outline>()
                .eq(Outline::getProjectId, projectId)
                .eq(Outline::getDeleted, 0));
        return (int) existingCount + 1;
    }

    private void validateRelations(Long projectId, OutlineRequestDTO requestDTO) {
        if (!StringUtils.hasText(requestDTO.getTitle()) && !StringUtils.hasText(requestDTO.getSummary())) {
            throw new IllegalArgumentException("大纲至少需要标题或摘要");
        }

        if (requestDTO.getChapterId() != null) {
            Chapter chapter = chapterMapper.selectById(requestDTO.getChapterId());
            if (chapter == null
                    || Integer.valueOf(1).equals(chapter.getDeleted())
                    || !Objects.equals(chapter.getProjectId(), projectId)) {
                throw new IllegalArgumentException("关联章节不存在或不属于当前项目");
            }
        }

        Set<Long> focusCharacterIds = normalizeIds(requestDTO.getFocusCharacterIds());
        if (!focusCharacterIds.isEmpty()) {
            long linkedCharacterCount = projectCharacterMapper.selectCount(new LambdaQueryWrapper<ProjectCharacterLink>()
                    .eq(ProjectCharacterLink::getProjectId, projectId)
                    .in(ProjectCharacterLink::getCharacterId, focusCharacterIds));
            if (linkedCharacterCount != focusCharacterIds.size()) {
                throw new IllegalArgumentException("存在未关联到当前项目的人物，无法绑定到大纲");
            }
        }

        Set<Long> plotIds = normalizeIds(requestDTO.getRelatedPlotIds());
        if (!plotIds.isEmpty()) {
            long plotCount = plotMapper.selectCount(new LambdaQueryWrapper<Plot>()
                    .eq(Plot::getProjectId, projectId)
                    .eq(Plot::getDeleted, 0)
                    .in(Plot::getId, plotIds));
            if (plotCount != plotIds.size()) {
                throw new IllegalArgumentException("存在不属于当前项目的剧情节点");
            }
        }

        Set<Long> causalityIds = normalizeIds(requestDTO.getRelatedCausalityIds());
        if (!causalityIds.isEmpty()) {
            long causalityCount = causalityMapper.selectCount(new LambdaQueryWrapper<Causality>()
                    .eq(Causality::getProjectId, projectId)
                    .eq(Causality::getDeleted, 0)
                    .in(Causality::getId, causalityIds));
            if (causalityCount != causalityIds.size()) {
                throw new IllegalArgumentException("存在不属于当前项目的因果关系");
            }
        }
    }

    private void attachReadableRelations(List<Outline> outlines) {
        if (outlines == null || outlines.isEmpty()) {
            return;
        }

        Set<Long> chapterIds = new LinkedHashSet<>();
        Set<Long> focusCharacterIds = new LinkedHashSet<>();
        Set<Long> plotIds = new LinkedHashSet<>();
        Set<Long> causalityIds = new LinkedHashSet<>();

        for (Outline outline : outlines) {
            if (outline.getChapterId() != null) {
                chapterIds.add(outline.getChapterId());
            }
            focusCharacterIds.addAll(parseIds(outline.getFocusCharacterIds()));
            plotIds.addAll(parseIds(outline.getRelatedPlotIds()));
            causalityIds.addAll(parseIds(outline.getRelatedCausalityIds()));
        }

        Map<Long, String> chapterTitleMap = chapterIds.isEmpty()
                ? Map.of()
                : chapterMapper.selectBatchIds(chapterIds).stream()
                        .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                        .collect(Collectors.toMap(Chapter::getId, Chapter::getTitle, (left, right) -> left, LinkedHashMap::new));

        Map<Long, String> characterNameMap = focusCharacterIds.isEmpty()
                ? Map.of()
                : characterMapper.selectBatchIds(focusCharacterIds).stream()
                        .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                        .collect(Collectors.toMap(Character::getId, Character::getName, (left, right) -> left, LinkedHashMap::new));

        Map<Long, String> plotTitleMap = plotIds.isEmpty()
                ? Map.of()
                : plotMapper.selectBatchIds(plotIds).stream()
                        .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                        .collect(Collectors.toMap(Plot::getId, Plot::getTitle, (left, right) -> left, LinkedHashMap::new));

        Map<Long, String> causalityNameMap = causalityIds.isEmpty()
                ? Map.of()
                : causalityMapper.selectBatchIds(causalityIds).stream()
                        .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                        .collect(Collectors.toMap(
                                Causality::getId,
                                item -> StringUtils.hasText(item.getName()) ? item.getName() : item.getRelationship(),
                                (left, right) -> left,
                                LinkedHashMap::new
                        ));

        for (Outline outline : outlines) {
            List<Long> focusIds = parseIds(outline.getFocusCharacterIds());
            List<Long> relatedPlotIds = parseIds(outline.getRelatedPlotIds());
            List<Long> relatedCausalityIds = parseIds(outline.getRelatedCausalityIds());

            outline.setChapterTitle(chapterTitleMap.get(outline.getChapterId()));
            outline.setFocusCharacterIdList(focusIds);
            outline.setFocusCharacterNames(resolveNames(focusIds, characterNameMap));
            outline.setRelatedPlotIdList(relatedPlotIds);
            outline.setRelatedPlotTitles(resolveNames(relatedPlotIds, plotTitleMap));
            outline.setRelatedCausalityIdList(relatedCausalityIds);
            outline.setRelatedCausalityNames(resolveNames(relatedCausalityIds, causalityNameMap));
        }
    }

    private List<Long> parseIds(String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String token : csv.split(",")) {
            if (!StringUtils.hasText(token)) {
                continue;
            }
            try {
                ids.add(Long.parseLong(token.trim()));
            } catch (NumberFormatException ignored) {
                // Ignore malformed ids so old data does not break page rendering.
            }
        }
        return ids;
    }

    private String joinIds(List<Long> ids) {
        return normalizeIds(ids).stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private Set<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> resolveNames(List<Long> ids, Map<Long, String> nameMap) {
        return ids.stream()
                .map(nameMap::get)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
