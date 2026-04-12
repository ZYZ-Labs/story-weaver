package com.storyweaver.storyunit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.storyweaver.domain.entity.ChapterCharacterLink;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.ProjectCharacterLink;
import com.storyweaver.repository.ChapterCharacterMapper;
import com.storyweaver.repository.CharacterMapper;
import com.storyweaver.repository.ProjectCharacterMapper;
import com.storyweaver.storyunit.assembler.CharacterStoryUnitAssembler;
import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryUnit;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.projection.CharacterProjectionSource;
import com.storyweaver.storyunit.service.ProjectedStoryUnit;
import com.storyweaver.storyunit.service.TypedStoryUnitProjectionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class CharacterStoryUnitProjectionService implements TypedStoryUnitProjectionService {

    private final CharacterMapper characterMapper;
    private final ProjectCharacterMapper projectCharacterMapper;
    private final ChapterCharacterMapper chapterCharacterMapper;
    private final CharacterStoryUnitAssembler assembler;

    public CharacterStoryUnitProjectionService(
            CharacterMapper characterMapper,
            ProjectCharacterMapper projectCharacterMapper,
            ChapterCharacterMapper chapterCharacterMapper,
            CharacterStoryUnitAssembler assembler) {
        this.characterMapper = characterMapper;
        this.projectCharacterMapper = projectCharacterMapper;
        this.chapterCharacterMapper = chapterCharacterMapper;
        this.assembler = assembler;
    }

    @Override
    public StoryUnitType unitType() {
        return StoryUnitType.CHARACTER;
    }

    @Override
    public Optional<ProjectedStoryUnit> projectByUnitId(String unitId) {
        Long id = ProjectionSupport.parseUnitId(unitId);
        if (id == null) {
            return Optional.empty();
        }
        Character character = characterMapper.selectById(id);
        if (character == null || Integer.valueOf(1).equals(character.getDeleted())) {
            return Optional.empty();
        }
        return Optional.of(project(loadSource(character)));
    }

    @Override
    public List<ProjectedStoryUnit> listByProjectId(Long projectId) {
        if (projectId == null) {
            return List.of();
        }
        List<ProjectCharacterLink> scopedLinks = projectCharacterMapper.selectList(new LambdaQueryWrapper<ProjectCharacterLink>()
                .eq(ProjectCharacterLink::getProjectId, projectId)
                .orderByAsc(ProjectCharacterLink::getId));
        if (scopedLinks.isEmpty()) {
            return List.of();
        }

        List<Long> characterIds = scopedLinks.stream()
                .map(ProjectCharacterLink::getCharacterId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (characterIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Character> characters = characterMapper.selectBatchIds(characterIds).stream()
                .filter(character -> character != null && !Integer.valueOf(1).equals(character.getDeleted()))
                .collect(LinkedHashMap::new, (map, character) -> map.put(character.getId(), character), Map::putAll);

        List<ProjectedStoryUnit> result = new ArrayList<>();
        for (Long characterId : characterIds) {
            Character character = characters.get(characterId);
            if (character != null) {
                result.add(project(loadSource(character)));
            }
        }
        return List.copyOf(result);
    }

    private CharacterProjectionSource loadSource(Character character) {
        List<ProjectCharacterLink> projectLinks = projectCharacterMapper.selectList(new LambdaQueryWrapper<ProjectCharacterLink>()
                .eq(ProjectCharacterLink::getCharacterId, character.getId())
                .orderByAsc(ProjectCharacterLink::getId));
        List<ChapterCharacterLink> chapterLinks = chapterCharacterMapper.selectList(new LambdaQueryWrapper<ChapterCharacterLink>()
                .eq(ChapterCharacterLink::getCharacterId, character.getId())
                .orderByAsc(ChapterCharacterLink::getId));
        return new CharacterProjectionSource(character, projectLinks, chapterLinks);
    }

    private ProjectedStoryUnit project(CharacterProjectionSource source) {
        StoryUnit unit = assembler.assembleUnit(source);
        Map<FacetType, StoryFacet> facets = assembler.assembleFacets(source, unit);
        return new ProjectedStoryUnit(ProjectionSupport.withFacetRefs(unit, facets), facets);
    }
}
