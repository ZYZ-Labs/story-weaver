package com.storyweaver.storyunit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.ChapterCharacterLink;
import com.storyweaver.domain.entity.ChapterPlotLink;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.Outline;
import com.storyweaver.domain.entity.Plot;
import com.storyweaver.repository.ChapterCharacterMapper;
import com.storyweaver.repository.ChapterMapper;
import com.storyweaver.repository.ChapterPlotMapper;
import com.storyweaver.repository.CharacterMapper;
import com.storyweaver.repository.OutlineMapper;
import com.storyweaver.repository.PlotMapper;
import com.storyweaver.storyunit.assembler.ChapterStoryUnitAssembler;
import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryUnit;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.projection.ChapterProjectionSource;
import com.storyweaver.storyunit.service.ProjectedStoryUnit;
import com.storyweaver.storyunit.service.TypedStoryUnitProjectionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ChapterStoryUnitProjectionService implements TypedStoryUnitProjectionService {

    private final ChapterMapper chapterMapper;
    private final ChapterCharacterMapper chapterCharacterMapper;
    private final ChapterPlotMapper chapterPlotMapper;
    private final CharacterMapper characterMapper;
    private final OutlineMapper outlineMapper;
    private final PlotMapper plotMapper;
    private final ChapterStoryUnitAssembler assembler;

    public ChapterStoryUnitProjectionService(
            ChapterMapper chapterMapper,
            ChapterCharacterMapper chapterCharacterMapper,
            ChapterPlotMapper chapterPlotMapper,
            CharacterMapper characterMapper,
            OutlineMapper outlineMapper,
            PlotMapper plotMapper,
            ChapterStoryUnitAssembler assembler) {
        this.chapterMapper = chapterMapper;
        this.chapterCharacterMapper = chapterCharacterMapper;
        this.chapterPlotMapper = chapterPlotMapper;
        this.characterMapper = characterMapper;
        this.outlineMapper = outlineMapper;
        this.plotMapper = plotMapper;
        this.assembler = assembler;
    }

    @Override
    public StoryUnitType unitType() {
        return StoryUnitType.CHAPTER;
    }

    @Override
    public Optional<ProjectedStoryUnit> projectByUnitId(String unitId) {
        Long id = ProjectionSupport.parseUnitId(unitId);
        if (id == null) {
            return Optional.empty();
        }
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null || Integer.valueOf(1).equals(chapter.getDeleted())) {
            return Optional.empty();
        }
        return Optional.of(project(loadSource(chapter)));
    }

    @Override
    public List<ProjectedStoryUnit> listByProjectId(Long projectId) {
        if (projectId == null) {
            return List.of();
        }
        List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, projectId)
                .eq(Chapter::getDeleted, 0)
                .orderByAsc(Chapter::getOrderNum)
                .orderByAsc(Chapter::getCreateTime));
        if (chapters.isEmpty()) {
            return List.of();
        }
        List<ProjectedStoryUnit> result = new ArrayList<>();
        for (Chapter chapter : chapters) {
            result.add(project(loadSource(chapter)));
        }
        return List.copyOf(result);
    }

    private ChapterProjectionSource loadSource(Chapter chapter) {
        Outline outline = chapter.getOutlineId() == null ? null : outlineMapper.selectById(chapter.getOutlineId());
        Character mainPovCharacter = chapter.getMainPovCharacterId() == null ? null : characterMapper.selectById(chapter.getMainPovCharacterId());

        List<ChapterCharacterLink> chapterCharacterLinks = chapterCharacterMapper.selectList(new LambdaQueryWrapper<ChapterCharacterLink>()
                .eq(ChapterCharacterLink::getChapterId, chapter.getId())
                .eq(ChapterCharacterLink::getRequiredFlag, 1)
                .orderByAsc(ChapterCharacterLink::getId));
        List<Long> characterIds = chapterCharacterLinks.stream()
                .map(ChapterCharacterLink::getCharacterId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, Character> charactersById = characterIds.isEmpty()
                ? Map.of()
                : characterMapper.selectBatchIds(characterIds).stream()
                .filter(character -> character != null && !Integer.valueOf(1).equals(character.getDeleted()))
                .collect(LinkedHashMap::new, (map, character) -> map.put(character.getId(), character), Map::putAll);
        List<Character> requiredCharacters = characterIds.stream()
                .map(charactersById::get)
                .filter(character -> character != null)
                .toList();

        List<ChapterPlotLink> chapterPlotLinks = chapterPlotMapper.selectList(new LambdaQueryWrapper<ChapterPlotLink>()
                .eq(ChapterPlotLink::getChapterId, chapter.getId())
                .orderByAsc(ChapterPlotLink::getSortOrder)
                .orderByAsc(ChapterPlotLink::getId));
        List<Long> plotIds = chapterPlotLinks.stream()
                .map(ChapterPlotLink::getPlotId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, Plot> plotsById = plotIds.isEmpty()
                ? Map.of()
                : plotMapper.selectBatchIds(plotIds).stream()
                .filter(plot -> plot != null && !Integer.valueOf(1).equals(plot.getDeleted()))
                .collect(LinkedHashMap::new, (map, plot) -> map.put(plot.getId(), plot), Map::putAll);
        List<Plot> plots = plotIds.stream()
                .map(plotsById::get)
                .filter(plot -> plot != null)
                .toList();

        return new ChapterProjectionSource(chapter, outline, mainPovCharacter, requiredCharacters, plots);
    }

    private ProjectedStoryUnit project(ChapterProjectionSource source) {
        StoryUnit unit = assembler.assembleUnit(source);
        Map<FacetType, StoryFacet> facets = assembler.assembleFacets(source, unit);
        return new ProjectedStoryUnit(ProjectionSupport.withFacetRefs(unit, facets), facets);
    }
}
