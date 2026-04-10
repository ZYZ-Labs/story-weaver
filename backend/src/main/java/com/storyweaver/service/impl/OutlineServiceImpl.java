package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.dto.OutlineRequestDTO;
import com.storyweaver.domain.entity.Causality;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.Outline;
import com.storyweaver.domain.entity.OutlineCausalityLink;
import com.storyweaver.domain.entity.OutlineCharacterFocusLink;
import com.storyweaver.domain.entity.OutlinePlotLink;
import com.storyweaver.domain.entity.OutlineWorldSettingLink;
import com.storyweaver.domain.entity.Plot;
import com.storyweaver.domain.entity.ProjectCharacterLink;
import com.storyweaver.domain.entity.ProjectWorldSettingLink;
import com.storyweaver.domain.entity.WorldSetting;
import com.storyweaver.repository.CausalityMapper;
import com.storyweaver.repository.ChapterMapper;
import com.storyweaver.repository.CharacterMapper;
import com.storyweaver.repository.OutlineCausalityMapper;
import com.storyweaver.repository.OutlineCharacterFocusMapper;
import com.storyweaver.repository.OutlineMapper;
import com.storyweaver.repository.OutlinePlotMapper;
import com.storyweaver.repository.OutlineWorldSettingMapper;
import com.storyweaver.repository.PlotMapper;
import com.storyweaver.repository.ProjectCharacterMapper;
import com.storyweaver.repository.ProjectWorldSettingMapper;
import com.storyweaver.repository.WorldSettingMapper;
import com.storyweaver.service.OutlineService;
import com.storyweaver.service.ProjectService;
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
public class OutlineServiceImpl extends ServiceImpl<OutlineMapper, Outline> implements OutlineService {

    private static final Set<String> SUPPORTED_OUTLINE_TYPES = Set.of("global", "volume", "chapter");

    private final ProjectService projectService;
    private final ChapterMapper chapterMapper;
    private final CharacterMapper characterMapper;
    private final ProjectCharacterMapper projectCharacterMapper;
    private final PlotMapper plotMapper;
    private final CausalityMapper causalityMapper;
    private final ProjectWorldSettingMapper projectWorldSettingMapper;
    private final WorldSettingMapper worldSettingMapper;
    private final OutlineWorldSettingMapper outlineWorldSettingMapper;
    private final OutlinePlotMapper outlinePlotMapper;
    private final OutlineCausalityMapper outlineCausalityMapper;
    private final OutlineCharacterFocusMapper outlineCharacterFocusMapper;

    public OutlineServiceImpl(
            ProjectService projectService,
            ChapterMapper chapterMapper,
            CharacterMapper characterMapper,
            ProjectCharacterMapper projectCharacterMapper,
            PlotMapper plotMapper,
            CausalityMapper causalityMapper,
            ProjectWorldSettingMapper projectWorldSettingMapper,
            WorldSettingMapper worldSettingMapper,
            OutlineWorldSettingMapper outlineWorldSettingMapper,
            OutlinePlotMapper outlinePlotMapper,
            OutlineCausalityMapper outlineCausalityMapper,
            OutlineCharacterFocusMapper outlineCharacterFocusMapper) {
        this.projectService = projectService;
        this.chapterMapper = chapterMapper;
        this.characterMapper = characterMapper;
        this.projectCharacterMapper = projectCharacterMapper;
        this.plotMapper = plotMapper;
        this.causalityMapper = causalityMapper;
        this.projectWorldSettingMapper = projectWorldSettingMapper;
        this.worldSettingMapper = worldSettingMapper;
        this.outlineWorldSettingMapper = outlineWorldSettingMapper;
        this.outlinePlotMapper = outlinePlotMapper;
        this.outlineCausalityMapper = outlineCausalityMapper;
        this.outlineCharacterFocusMapper = outlineCharacterFocusMapper;
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

        validateRelations(projectId, null, requestDTO);

        Outline outline = new Outline();
        applyRequest(outline, projectId, requestDTO);
        save(outline);

        if (outline.getRootOutlineId() == null) {
            outline.setRootOutlineId(outline.getId());
            updateById(outline);
        }

        syncOutlineRelations(outline, requestDTO);
        return getOutlineWithAuth(outline.getId(), userId);
    }

    @Override
    @Transactional
    public boolean updateOutline(Long projectId, Long outlineId, Long userId, OutlineRequestDTO requestDTO) {
        Outline existing = getOutlineWithAuth(outlineId, userId);
        if (existing == null || !Objects.equals(existing.getProjectId(), projectId)) {
            return false;
        }

        validateRelations(projectId, outlineId, requestDTO);
        applyRequest(existing, projectId, requestDTO);
        if (existing.getRootOutlineId() == null) {
            existing.setRootOutlineId(existing.getId());
        }
        boolean updated = updateById(existing);
        if (updated) {
            syncOutlineRelations(existing, requestDTO);
        }
        return updated;
    }

    @Override
    @Transactional
    public boolean deleteOutline(Long outlineId, Long userId) {
        Outline outline = getOutlineWithAuth(outlineId, userId);
        if (outline == null) {
            return false;
        }
        deleteOutlineRelations(outlineId);
        return removeById(outlineId);
    }

    private void applyRequest(Outline outline, Long projectId, OutlineRequestDTO requestDTO) {
        Long chapterId = resolveRequestedChapterId(requestDTO);
        String outlineType = resolveOutlineType(requestDTO.getOutlineType(), chapterId);

        outline.setProjectId(projectId);
        outline.setOutlineType(outlineType);
        outline.setParentOutlineId(requestDTO.getParentOutlineId());
        outline.setRootOutlineId(resolveRootOutlineId(projectId, outline.getId(), requestDTO.getParentOutlineId()));
        outline.setChapterId(chapterId);
        outline.setGeneratedChapterId(chapterId);
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
        outline.setRelatedWorldSettingIdsJson(toJsonArray(requestDTO.getRelatedWorldSettingIds()));
        outline.setStatus(requestDTO.getStatus() != null ? requestDTO.getStatus() : 0);
        outline.setOrderNum(resolveOrderNum(projectId, chapterId, requestDTO));
    }

    private Integer resolveOrderNum(Long projectId, Long chapterId, OutlineRequestDTO requestDTO) {
        if (requestDTO.getOrderNum() != null) {
            return requestDTO.getOrderNum();
        }
        if (chapterId != null) {
            Chapter chapter = chapterMapper.selectById(chapterId);
            if (chapter != null && !Integer.valueOf(1).equals(chapter.getDeleted()) && chapter.getOrderNum() != null) {
                return chapter.getOrderNum();
            }
        }
        long existingCount = count(new LambdaQueryWrapper<Outline>()
                .eq(Outline::getProjectId, projectId)
                .eq(Outline::getDeleted, 0));
        return (int) existingCount + 1;
    }

    private void validateRelations(Long projectId, Long selfOutlineId, OutlineRequestDTO requestDTO) {
        if (!StringUtils.hasText(requestDTO.getTitle()) && !StringUtils.hasText(requestDTO.getSummary())) {
            throw new IllegalArgumentException("大纲至少需要标题或摘要");
        }

        Long chapterId = resolveRequestedChapterId(requestDTO);
        String outlineType = resolveOutlineType(requestDTO.getOutlineType(), chapterId);
        if (!SUPPORTED_OUTLINE_TYPES.contains(outlineType)) {
            throw new IllegalArgumentException("不支持的大纲类型");
        }

        if (chapterId != null) {
            Chapter chapter = chapterMapper.selectById(chapterId);
            if (chapter == null
                    || Integer.valueOf(1).equals(chapter.getDeleted())
                    || !Objects.equals(chapter.getProjectId(), projectId)) {
                throw new IllegalArgumentException("关联章节不存在或不属于当前项目");
            }
        }

        if (requestDTO.getParentOutlineId() != null) {
            Outline parent = getById(requestDTO.getParentOutlineId());
            if (parent == null
                    || Integer.valueOf(1).equals(parent.getDeleted())
                    || !Objects.equals(parent.getProjectId(), projectId)) {
                throw new IllegalArgumentException("父级大纲不存在或不属于当前项目");
            }
            if (selfOutlineId != null && Objects.equals(selfOutlineId, parent.getId())) {
                throw new IllegalArgumentException("父级大纲不能是当前大纲本身");
            }
            if (!isAllowedParentChild(resolveOutlineType(parent.getOutlineType(), parent.getGeneratedChapterId()), outlineType)) {
                throw new IllegalArgumentException("当前大纲类型不能挂到该父级下");
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

        Set<Long> worldSettingIds = normalizeIds(requestDTO.getRelatedWorldSettingIds());
        if (!worldSettingIds.isEmpty()) {
            long linkedWorldSettingCount = projectWorldSettingMapper.selectCount(new LambdaQueryWrapper<ProjectWorldSettingLink>()
                    .eq(ProjectWorldSettingLink::getProjectId, projectId)
                    .in(ProjectWorldSettingLink::getWorldSettingId, worldSettingIds));
            if (linkedWorldSettingCount != worldSettingIds.size()) {
                throw new IllegalArgumentException("存在未关联到当前项目的世界观设定");
            }
        }
    }

    private void syncOutlineRelations(Outline outline, OutlineRequestDTO requestDTO) {
        Long outlineId = outline.getId();
        syncOutlineCharacterFocus(outlineId, normalizeIds(requestDTO.getFocusCharacterIds()));
        syncOutlinePlots(outlineId, normalizeIds(requestDTO.getRelatedPlotIds()));
        syncOutlineCausalities(outlineId, normalizeIds(requestDTO.getRelatedCausalityIds()));
        syncOutlineWorldSettings(outlineId, normalizeIds(requestDTO.getRelatedWorldSettingIds()));
    }

    private void syncOutlineCharacterFocus(Long outlineId, Set<Long> characterIds) {
        outlineCharacterFocusMapper.delete(new LambdaQueryWrapper<OutlineCharacterFocusLink>()
                .eq(OutlineCharacterFocusLink::getOutlineId, outlineId));
        for (Long characterId : characterIds) {
            OutlineCharacterFocusLink link = new OutlineCharacterFocusLink();
            link.setOutlineId(outlineId);
            link.setCharacterId(characterId);
            outlineCharacterFocusMapper.insert(link);
        }
    }

    private void syncOutlinePlots(Long outlineId, Set<Long> plotIds) {
        outlinePlotMapper.delete(new LambdaQueryWrapper<OutlinePlotLink>()
                .eq(OutlinePlotLink::getOutlineId, outlineId));
        for (Long plotId : plotIds) {
            OutlinePlotLink link = new OutlinePlotLink();
            link.setOutlineId(outlineId);
            link.setPlotId(plotId);
            outlinePlotMapper.insert(link);
        }
    }

    private void syncOutlineCausalities(Long outlineId, Set<Long> causalityIds) {
        outlineCausalityMapper.delete(new LambdaQueryWrapper<OutlineCausalityLink>()
                .eq(OutlineCausalityLink::getOutlineId, outlineId));
        for (Long causalityId : causalityIds) {
            OutlineCausalityLink link = new OutlineCausalityLink();
            link.setOutlineId(outlineId);
            link.setCausalityId(causalityId);
            outlineCausalityMapper.insert(link);
        }
    }

    private void syncOutlineWorldSettings(Long outlineId, Set<Long> worldSettingIds) {
        outlineWorldSettingMapper.delete(new LambdaQueryWrapper<OutlineWorldSettingLink>()
                .eq(OutlineWorldSettingLink::getOutlineId, outlineId));
        for (Long worldSettingId : worldSettingIds) {
            OutlineWorldSettingLink link = new OutlineWorldSettingLink();
            link.setOutlineId(outlineId);
            link.setWorldSettingId(worldSettingId);
            outlineWorldSettingMapper.insert(link);
        }
    }

    private void deleteOutlineRelations(Long outlineId) {
        outlineCharacterFocusMapper.delete(new LambdaQueryWrapper<OutlineCharacterFocusLink>()
                .eq(OutlineCharacterFocusLink::getOutlineId, outlineId));
        outlinePlotMapper.delete(new LambdaQueryWrapper<OutlinePlotLink>()
                .eq(OutlinePlotLink::getOutlineId, outlineId));
        outlineCausalityMapper.delete(new LambdaQueryWrapper<OutlineCausalityLink>()
                .eq(OutlineCausalityLink::getOutlineId, outlineId));
        outlineWorldSettingMapper.delete(new LambdaQueryWrapper<OutlineWorldSettingLink>()
                .eq(OutlineWorldSettingLink::getOutlineId, outlineId));
    }

    private void attachReadableRelations(List<Outline> outlines) {
        if (outlines == null || outlines.isEmpty()) {
            return;
        }

        List<Long> outlineIds = outlines.stream()
                .map(Outline::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, List<Long>> focusCharacterIdsByOutline = mapOutlineIds(outlineCharacterFocusMapper.selectList(
                new LambdaQueryWrapper<OutlineCharacterFocusLink>()
                        .in(OutlineCharacterFocusLink::getOutlineId, outlineIds)
                        .orderByAsc(OutlineCharacterFocusLink::getId)
        ), OutlineCharacterFocusLink::getOutlineId, OutlineCharacterFocusLink::getCharacterId);

        Map<Long, List<Long>> plotIdsByOutline = mapOutlineIds(outlinePlotMapper.selectList(
                new LambdaQueryWrapper<OutlinePlotLink>()
                        .in(OutlinePlotLink::getOutlineId, outlineIds)
                        .orderByAsc(OutlinePlotLink::getId)
        ), OutlinePlotLink::getOutlineId, OutlinePlotLink::getPlotId);

        Map<Long, List<Long>> causalityIdsByOutline = mapOutlineIds(outlineCausalityMapper.selectList(
                new LambdaQueryWrapper<OutlineCausalityLink>()
                        .in(OutlineCausalityLink::getOutlineId, outlineIds)
                        .orderByAsc(OutlineCausalityLink::getId)
        ), OutlineCausalityLink::getOutlineId, OutlineCausalityLink::getCausalityId);

        Map<Long, List<Long>> worldSettingIdsByOutline = mapOutlineIds(outlineWorldSettingMapper.selectList(
                new LambdaQueryWrapper<OutlineWorldSettingLink>()
                        .in(OutlineWorldSettingLink::getOutlineId, outlineIds)
                        .orderByAsc(OutlineWorldSettingLink::getId)
        ), OutlineWorldSettingLink::getOutlineId, OutlineWorldSettingLink::getWorldSettingId);

        Set<Long> chapterIds = new LinkedHashSet<>();
        Set<Long> focusCharacterIds = new LinkedHashSet<>();
        Set<Long> plotIds = new LinkedHashSet<>();
        Set<Long> causalityIds = new LinkedHashSet<>();
        Set<Long> worldSettingIds = new LinkedHashSet<>();

        for (Outline outline : outlines) {
            Long chapterId = resolveOutlineChapterId(outline);
            if (chapterId != null) {
                chapterIds.add(chapterId);
            }
            focusCharacterIds.addAll(resolveOutlineRelationIds(
                    focusCharacterIdsByOutline.get(outline.getId()),
                    parseIds(outline.getFocusCharacterIds())
            ));
            plotIds.addAll(resolveOutlineRelationIds(
                    plotIdsByOutline.get(outline.getId()),
                    parseIds(outline.getRelatedPlotIds())
            ));
            causalityIds.addAll(resolveOutlineRelationIds(
                    causalityIdsByOutline.get(outline.getId()),
                    parseIds(outline.getRelatedCausalityIds())
            ));
            worldSettingIds.addAll(resolveOutlineRelationIds(
                    worldSettingIdsByOutline.get(outline.getId()),
                    parseJsonIds(outline.getRelatedWorldSettingIdsJson())
            ));
        }

        Map<Long, String> chapterTitleMap = chapterIds.isEmpty()
                ? Collections.emptyMap()
                : chapterMapper.selectBatchIds(chapterIds).stream()
                        .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                        .collect(Collectors.toMap(Chapter::getId, Chapter::getTitle, (left, right) -> left, LinkedHashMap::new));

        Map<Long, String> characterNameMap = focusCharacterIds.isEmpty()
                ? Collections.emptyMap()
                : characterMapper.selectBatchIds(focusCharacterIds).stream()
                        .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                        .collect(Collectors.toMap(Character::getId, Character::getName, (left, right) -> left, LinkedHashMap::new));

        Map<Long, String> plotTitleMap = plotIds.isEmpty()
                ? Collections.emptyMap()
                : plotMapper.selectBatchIds(plotIds).stream()
                        .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                        .collect(Collectors.toMap(Plot::getId, Plot::getTitle, (left, right) -> left, LinkedHashMap::new));

        Map<Long, String> causalityNameMap = causalityIds.isEmpty()
                ? Collections.emptyMap()
                : causalityMapper.selectBatchIds(causalityIds).stream()
                        .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                        .collect(Collectors.toMap(
                                Causality::getId,
                                item -> StringUtils.hasText(item.getName()) ? item.getName() : item.getRelationship(),
                                (left, right) -> left,
                                LinkedHashMap::new
                        ));

        Map<Long, String> worldSettingNameMap = worldSettingIds.isEmpty()
                ? Collections.emptyMap()
                : worldSettingMapper.selectBatchIds(worldSettingIds).stream()
                        .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                        .collect(Collectors.toMap(
                                WorldSetting::getId,
                                item -> StringUtils.hasText(item.getName()) ? item.getName() : item.getTitle(),
                                (left, right) -> left,
                                LinkedHashMap::new
                        ));

        for (Outline outline : outlines) {
            Long chapterId = resolveOutlineChapterId(outline);
            List<Long> focusIds = resolveOutlineRelationIds(
                    focusCharacterIdsByOutline.get(outline.getId()),
                    parseIds(outline.getFocusCharacterIds())
            );
            List<Long> relatedPlotIds = resolveOutlineRelationIds(
                    plotIdsByOutline.get(outline.getId()),
                    parseIds(outline.getRelatedPlotIds())
            );
            List<Long> relatedCausalityIds = resolveOutlineRelationIds(
                    causalityIdsByOutline.get(outline.getId()),
                    parseIds(outline.getRelatedCausalityIds())
            );
            List<Long> relatedWorldSettingIds = resolveOutlineRelationIds(
                    worldSettingIdsByOutline.get(outline.getId()),
                    parseJsonIds(outline.getRelatedWorldSettingIdsJson())
            );

            if (outline.getGeneratedChapterId() == null) {
                outline.setGeneratedChapterId(chapterId);
            }
            if (outline.getChapterId() == null) {
                outline.setChapterId(chapterId);
            }
            if (outline.getRootOutlineId() == null) {
                outline.setRootOutlineId(outline.getId());
            }

            outline.setChapterTitle(chapterId == null ? null : chapterTitleMap.get(chapterId));
            outline.setFocusCharacterIdList(focusIds);
            outline.setFocusCharacterNames(resolveNames(focusIds, characterNameMap));
            outline.setRelatedPlotIdList(relatedPlotIds);
            outline.setRelatedPlotTitles(resolveNames(relatedPlotIds, plotTitleMap));
            outline.setRelatedCausalityIdList(relatedCausalityIds);
            outline.setRelatedCausalityNames(resolveNames(relatedCausalityIds, causalityNameMap));
            outline.setRelatedWorldSettingIdList(relatedWorldSettingIds);
            outline.setRelatedWorldSettingNames(resolveNames(relatedWorldSettingIds, worldSettingNameMap));
        }
    }

    private <T> Map<Long, List<Long>> mapOutlineIds(List<T> source, IdExtractor<T> outlineIdExtractor, IdExtractor<T> relationIdExtractor) {
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        for (T item : source) {
            Long outlineId = outlineIdExtractor.get(item);
            Long relationId = relationIdExtractor.get(item);
            if (outlineId == null || relationId == null) {
                continue;
            }
            result.computeIfAbsent(outlineId, key -> new ArrayList<>()).add(relationId);
        }
        return result;
    }

    private Long resolveRequestedChapterId(OutlineRequestDTO requestDTO) {
        return requestDTO.getGeneratedChapterId() != null ? requestDTO.getGeneratedChapterId() : requestDTO.getChapterId();
    }

    private Long resolveRootOutlineId(Long projectId, Long currentOutlineId, Long parentOutlineId) {
        if (parentOutlineId == null) {
            return currentOutlineId;
        }
        Outline parent = getById(parentOutlineId);
        if (parent == null || !Objects.equals(parent.getProjectId(), projectId)) {
            return currentOutlineId;
        }
        return parent.getRootOutlineId() != null ? parent.getRootOutlineId() : parent.getId();
    }

    private Long resolveOutlineChapterId(Outline outline) {
        if (outline == null) {
            return null;
        }
        return outline.getGeneratedChapterId() != null ? outline.getGeneratedChapterId() : outline.getChapterId();
    }

    private String resolveOutlineType(String requestedType, Long chapterId) {
        if (StringUtils.hasText(requestedType)) {
            return requestedType.trim().toLowerCase();
        }
        return chapterId != null ? "chapter" : "global";
    }

    private boolean isAllowedParentChild(String parentType, String childType) {
        if (!StringUtils.hasText(parentType) || !StringUtils.hasText(childType)) {
            return true;
        }
        if (Objects.equals(parentType, "chapter")) {
            return false;
        }
        if (Objects.equals(parentType, "volume")) {
            return Objects.equals(childType, "chapter");
        }
        return Objects.equals(parentType, "global");
    }

    private List<Long> resolveOutlineRelationIds(List<Long> preferredIds, List<Long> fallbackIds) {
        return preferredIds != null && !preferredIds.isEmpty() ? preferredIds : fallbackIds;
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

    private List<Long> parseJsonIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        String normalized = raw.replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .trim();
        return parseIds(normalized);
    }

    private String joinIds(List<Long> ids) {
        Set<Long> normalized = normalizeIds(ids);
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String toJsonArray(List<Long> ids) {
        Set<Long> normalized = normalizeIds(ids);
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
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
                .filter(Objects::nonNull)
                .map(nameMap::get)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    @FunctionalInterface
    private interface IdExtractor<T> {
        Long get(T source);
    }
}
