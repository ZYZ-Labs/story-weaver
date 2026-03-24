package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.dto.WorldSettingDTO;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.entity.ProjectWorldSettingLink;
import com.storyweaver.domain.entity.WorldSetting;
import com.storyweaver.domain.vo.WorldSettingVO;
import com.storyweaver.repository.ProjectMapper;
import com.storyweaver.repository.ProjectWorldSettingMapper;
import com.storyweaver.repository.WorldSettingMapper;
import com.storyweaver.service.WorldSettingService;
import org.springframework.beans.BeanUtils;
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
public class WorldSettingServiceImpl extends ServiceImpl<WorldSettingMapper, WorldSetting> implements WorldSettingService {

    private final ProjectWorldSettingMapper projectWorldSettingMapper;
    private final ProjectMapper projectMapper;

    public WorldSettingServiceImpl(
            ProjectWorldSettingMapper projectWorldSettingMapper,
            ProjectMapper projectMapper) {
        this.projectWorldSettingMapper = projectWorldSettingMapper;
        this.projectMapper = projectMapper;
    }

    @Override
    public List<WorldSettingVO> getWorldSettingsByProjectId(Long projectId) {
        List<Long> worldSettingIds = listWorldSettingIdsByProject(projectId);
        if (worldSettingIds.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<WorldSetting> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(WorldSetting::getId, worldSettingIds)
                .eq(WorldSetting::getDeleted, 0)
                .orderByDesc(WorldSetting::getUpdateTime);

        List<WorldSetting> worldSettings = this.list(queryWrapper);
        Map<Long, Integer> associationCountMap = loadAssociationCountMap(worldSettingIds);

        return worldSettings.stream()
                .map(item -> convertToVO(item, associationCountMap))
                .collect(Collectors.toList());
    }

    @Override
    public List<WorldSettingVO> listLibraryWorldSettings(Long userId) {
        LambdaQueryWrapper<WorldSetting> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorldSetting::getOwnerUserId, userId)
                .eq(WorldSetting::getDeleted, 0)
                .orderByDesc(WorldSetting::getUpdateTime);

        List<WorldSetting> worldSettings = this.list(queryWrapper);
        if (worldSettings.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> associationCountMap = loadAssociationCountMap(
                worldSettings.stream().map(WorldSetting::getId).toList()
        );

        return worldSettings.stream()
                .map(item -> convertToVO(item, associationCountMap))
                .collect(Collectors.toList());
    }

    @Override
    public WorldSettingVO getWorldSettingById(Long id, Long userId) {
        WorldSetting worldSetting = getActiveWorldSetting(id);
        if (worldSetting == null || !hasAccess(id, userId)) {
            return null;
        }
        return convertToVO(worldSetting, loadAssociationCountMap(List.of(id)));
    }

    @Override
    @Transactional
    public WorldSettingVO createWorldSetting(WorldSettingDTO worldSettingDTO, Long userId) {
        WorldSetting worldSetting = new WorldSetting();
        BeanUtils.copyProperties(worldSettingDTO, worldSetting);
        worldSetting.setOwnerUserId(userId);
        syncLegacyFields(worldSetting);

        this.save(worldSetting);
        ensureAssociation(worldSettingDTO.getProjectId(), worldSetting.getId());
        return convertToVO(worldSetting, Map.of(worldSetting.getId(), 1));
    }

    @Override
    @Transactional
    public WorldSettingVO updateWorldSetting(Long id, WorldSettingDTO worldSettingDTO, Long userId) {
        WorldSetting worldSetting = getActiveWorldSetting(id);
        if (worldSetting == null || !ownsWorldSetting(worldSetting, userId)) {
            return null;
        }

        worldSetting.setName(worldSettingDTO.getName());
        worldSetting.setDescription(worldSettingDTO.getDescription());
        worldSetting.setCategory(worldSettingDTO.getCategory());
        syncLegacyFields(worldSetting);

        this.updateById(worldSetting);
        return convertToVO(worldSetting, loadAssociationCountMap(List.of(id)));
    }

    @Override
    @Transactional
    public void deleteWorldSetting(Long id, Long userId) {
        WorldSetting worldSetting = getActiveWorldSetting(id);
        if (worldSetting == null || !ownsWorldSetting(worldSetting, userId)) {
            return;
        }

        QueryWrapper<ProjectWorldSettingLink> deleteLinks = new QueryWrapper<>();
        deleteLinks.eq("world_setting_id", id);
        projectWorldSettingMapper.delete(deleteLinks);
        this.removeById(id);
    }

    @Override
    @Transactional
    public boolean attachWorldSettingToProject(Long worldSettingId, Long projectId, Long userId) {
        WorldSetting worldSetting = getActiveWorldSetting(worldSettingId);
        if (worldSetting == null || !ownsWorldSetting(worldSetting, userId)) {
            return false;
        }
        ensureAssociation(projectId, worldSettingId);
        return true;
    }

    @Override
    @Transactional
    public boolean detachWorldSettingFromProject(Long worldSettingId, Long projectId, Long userId) {
        WorldSetting worldSetting = getActiveWorldSetting(worldSettingId);
        if (worldSetting == null || !ownsWorldSetting(worldSetting, userId)) {
            return false;
        }

        QueryWrapper<ProjectWorldSettingLink> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("project_id", projectId).eq("world_setting_id", worldSettingId);
        return projectWorldSettingMapper.delete(deleteWrapper) > 0;
    }

    @Override
    @Transactional
    public void syncProjectAssociations(Long projectId, Long userId, List<Long> worldSettingIds) {
        Set<Long> targetIds = normalizeIds(worldSettingIds);
        if (!targetIds.isEmpty()) {
            long accessibleCount = this.count(new LambdaQueryWrapper<WorldSetting>()
                    .in(WorldSetting::getId, targetIds)
                    .eq(WorldSetting::getOwnerUserId, userId)
                    .eq(WorldSetting::getDeleted, 0));
            if (accessibleCount != targetIds.size()) {
                throw new IllegalArgumentException("存在无权关联的世界观模型");
            }
        }

        Set<Long> currentIds = new LinkedHashSet<>(listWorldSettingIdsByProject(projectId));
        for (Long worldSettingId : targetIds) {
            if (!currentIds.contains(worldSettingId)) {
                ensureAssociation(projectId, worldSettingId);
            }
        }

        Set<Long> removableIds = new LinkedHashSet<>(currentIds);
        removableIds.removeAll(targetIds);
        if (!removableIds.isEmpty()) {
            QueryWrapper<ProjectWorldSettingLink> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("project_id", projectId).in("world_setting_id", removableIds);
            projectWorldSettingMapper.delete(deleteWrapper);
        }
    }

    @Override
    public boolean hasAccess(Long worldSettingId, Long userId) {
        WorldSetting worldSetting = getActiveWorldSetting(worldSettingId);
        return worldSetting != null && ownsWorldSetting(worldSetting, userId);
    }

    private WorldSetting getActiveWorldSetting(Long id) {
        WorldSetting worldSetting = this.getById(id);
        if (worldSetting == null || Integer.valueOf(1).equals(worldSetting.getDeleted())) {
            return null;
        }
        return worldSetting;
    }

    private boolean ownsWorldSetting(WorldSetting worldSetting, Long userId) {
        if (worldSetting == null) {
            return false;
        }
        if (Objects.equals(worldSetting.getOwnerUserId(), userId)) {
            return true;
        }
        if (worldSetting.getProjectId() == null) {
            return false;
        }
        Project project = projectMapper.selectById(worldSetting.getProjectId());
        return project != null
                && !Integer.valueOf(1).equals(project.getDeleted())
                && Objects.equals(project.getUserId(), userId);
    }

    private List<Long> listWorldSettingIdsByProject(Long projectId) {
        QueryWrapper<ProjectWorldSettingLink> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id", projectId).orderByDesc("update_time").orderByDesc("id");
        List<ProjectWorldSettingLink> links = projectWorldSettingMapper.selectList(queryWrapper);
        if (links == null || links.isEmpty()) {
            return List.of();
        }

        List<Long> ids = new ArrayList<>();
        for (ProjectWorldSettingLink link : links) {
            if (link.getWorldSettingId() != null) {
                ids.add(link.getWorldSettingId());
            }
        }
        return ids;
    }

    private Map<Long, Integer> loadAssociationCountMap(List<Long> worldSettingIds) {
        if (worldSettingIds == null || worldSettingIds.isEmpty()) {
            return Collections.emptyMap();
        }

        QueryWrapper<ProjectWorldSettingLink> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("world_setting_id", worldSettingIds);
        List<ProjectWorldSettingLink> links = projectWorldSettingMapper.selectList(queryWrapper);

        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (Long worldSettingId : worldSettingIds) {
            counts.put(worldSettingId, 0);
        }
        for (ProjectWorldSettingLink link : links) {
            Long worldSettingId = link.getWorldSettingId();
            if (worldSettingId != null) {
                counts.put(worldSettingId, counts.getOrDefault(worldSettingId, 0) + 1);
            }
        }
        return counts;
    }

    private WorldSettingVO convertToVO(WorldSetting worldSetting, Map<Long, Integer> associationCountMap) {
        WorldSettingVO vo = new WorldSettingVO();
        BeanUtils.copyProperties(worldSetting, vo);
        vo.setAssociationCount(associationCountMap.getOrDefault(worldSetting.getId(), 0));
        return vo;
    }

    private void ensureAssociation(Long projectId, Long worldSettingId) {
        QueryWrapper<ProjectWorldSettingLink> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id", projectId).eq("world_setting_id", worldSettingId);
        ProjectWorldSettingLink existing = projectWorldSettingMapper.selectOne(queryWrapper);
        if (existing != null) {
            return;
        }

        ProjectWorldSettingLink link = new ProjectWorldSettingLink();
        link.setProjectId(projectId);
        link.setWorldSettingId(worldSettingId);
        projectWorldSettingMapper.insert(link);
    }

    private Set<Long> normalizeIds(List<Long> worldSettingIds) {
        if (worldSettingIds == null || worldSettingIds.isEmpty()) {
            return Collections.emptySet();
        }

        return worldSettingIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void syncLegacyFields(WorldSetting worldSetting) {
        worldSetting.setTitle(worldSetting.getName());
        worldSetting.setContent(worldSetting.getDescription());
        if (!StringUtils.hasText(worldSetting.getCategory())) {
            worldSetting.setCategory("世界规则");
        }
        if (worldSetting.getOrderNum() == null) {
            worldSetting.setOrderNum(0);
        }
    }
}
