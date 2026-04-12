package com.storyweaver.storyunit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.storyweaver.domain.entity.OutlineWorldSettingLink;
import com.storyweaver.domain.entity.ProjectWorldSettingLink;
import com.storyweaver.domain.entity.WorldSetting;
import com.storyweaver.repository.OutlineWorldSettingMapper;
import com.storyweaver.repository.ProjectWorldSettingMapper;
import com.storyweaver.repository.WorldSettingMapper;
import com.storyweaver.storyunit.assembler.WorldSettingStoryUnitAssembler;
import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryUnit;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.projection.WorldSettingProjectionSource;
import com.storyweaver.storyunit.service.ProjectedStoryUnit;
import com.storyweaver.storyunit.service.TypedStoryUnitProjectionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WorldSettingStoryUnitProjectionService implements TypedStoryUnitProjectionService {

    private final WorldSettingMapper worldSettingMapper;
    private final ProjectWorldSettingMapper projectWorldSettingMapper;
    private final OutlineWorldSettingMapper outlineWorldSettingMapper;
    private final WorldSettingStoryUnitAssembler assembler;

    public WorldSettingStoryUnitProjectionService(
            WorldSettingMapper worldSettingMapper,
            ProjectWorldSettingMapper projectWorldSettingMapper,
            OutlineWorldSettingMapper outlineWorldSettingMapper,
            WorldSettingStoryUnitAssembler assembler) {
        this.worldSettingMapper = worldSettingMapper;
        this.projectWorldSettingMapper = projectWorldSettingMapper;
        this.outlineWorldSettingMapper = outlineWorldSettingMapper;
        this.assembler = assembler;
    }

    @Override
    public StoryUnitType unitType() {
        return StoryUnitType.WORLD_SETTING;
    }

    @Override
    public Optional<ProjectedStoryUnit> projectByUnitId(String unitId) {
        Long id = ProjectionSupport.parseUnitId(unitId);
        if (id == null) {
            return Optional.empty();
        }
        WorldSetting worldSetting = worldSettingMapper.selectById(id);
        if (worldSetting == null || Integer.valueOf(1).equals(worldSetting.getDeleted())) {
            return Optional.empty();
        }
        return Optional.of(project(loadSource(worldSetting)));
    }

    @Override
    public List<ProjectedStoryUnit> listByProjectId(Long projectId) {
        if (projectId == null) {
            return List.of();
        }
        List<ProjectWorldSettingLink> scopedLinks = projectWorldSettingMapper.selectList(new LambdaQueryWrapper<ProjectWorldSettingLink>()
                .eq(ProjectWorldSettingLink::getProjectId, projectId)
                .orderByAsc(ProjectWorldSettingLink::getId));
        if (scopedLinks.isEmpty()) {
            return List.of();
        }

        List<Long> worldSettingIds = scopedLinks.stream()
                .map(ProjectWorldSettingLink::getWorldSettingId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (worldSettingIds.isEmpty()) {
            return List.of();
        }

        Map<Long, WorldSetting> worldSettings = worldSettingMapper.selectBatchIds(worldSettingIds).stream()
                .filter(worldSetting -> worldSetting != null && !Integer.valueOf(1).equals(worldSetting.getDeleted()))
                .collect(LinkedHashMap::new, (map, worldSetting) -> map.put(worldSetting.getId(), worldSetting), Map::putAll);

        List<ProjectedStoryUnit> result = new ArrayList<>();
        for (Long worldSettingId : worldSettingIds) {
            WorldSetting worldSetting = worldSettings.get(worldSettingId);
            if (worldSetting != null) {
                result.add(project(loadSource(worldSetting)));
            }
        }
        return List.copyOf(result);
    }

    private WorldSettingProjectionSource loadSource(WorldSetting worldSetting) {
        List<ProjectWorldSettingLink> projectLinks = projectWorldSettingMapper.selectList(new LambdaQueryWrapper<ProjectWorldSettingLink>()
                .eq(ProjectWorldSettingLink::getWorldSettingId, worldSetting.getId())
                .orderByAsc(ProjectWorldSettingLink::getId));
        List<OutlineWorldSettingLink> outlineLinks = outlineWorldSettingMapper.selectList(new LambdaQueryWrapper<OutlineWorldSettingLink>()
                .eq(OutlineWorldSettingLink::getWorldSettingId, worldSetting.getId())
                .orderByAsc(OutlineWorldSettingLink::getId));
        return new WorldSettingProjectionSource(worldSetting, projectLinks, outlineLinks);
    }

    private ProjectedStoryUnit project(WorldSettingProjectionSource source) {
        StoryUnit unit = assembler.assembleUnit(source);
        Map<FacetType, StoryFacet> facets = assembler.assembleFacets(source, unit);
        return new ProjectedStoryUnit(ProjectionSupport.withFacetRefs(unit, facets), facets);
    }
}
