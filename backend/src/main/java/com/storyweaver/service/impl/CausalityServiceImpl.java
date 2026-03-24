package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.entity.Causality;
import com.storyweaver.repository.CausalityMapper;
import com.storyweaver.service.CausalityService;
import com.storyweaver.service.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CausalityServiceImpl extends ServiceImpl<CausalityMapper, Causality> implements CausalityService {

    private final ProjectService projectService;

    public CausalityServiceImpl(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public List<Causality> getProjectCausalities(Long projectId, Long userId) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return List.of();
        }
        return baseMapper.findByProjectId(projectId);
    }

    @Override
    public Causality getCausality(Long id, Long userId) {
        Causality causality = getById(id);
        if (causality == null || !projectService.hasProjectAccess(causality.getProjectId(), userId)) {
            return null;
        }
        return causality;
    }

    @Override
    @Transactional
    public Causality createCausality(Long projectId, Long userId, Causality causality) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return null;
        }
        causality.setId(null);
        causality.setProjectId(projectId);
        causality.setCreateBy(userId);
        causality.setUpdateBy(userId);
        causality.setCreateTime(LocalDateTime.now());
        causality.setUpdateTime(LocalDateTime.now());
        if (causality.getStatus() == null) {
            causality.setStatus(1);
        }
        if (causality.getStrength() == null) {
            causality.setStrength(50);
        }
        save(causality);
        return causality;
    }

    @Override
    @Transactional
    public boolean updateCausality(Long id, Long userId, Causality causality) {
        Causality existing = getCausality(id, userId);
        if (existing == null) {
            return false;
        }
        existing.setName(causality.getName());
        existing.setDescription(causality.getDescription());
        existing.setCauseType(causality.getCauseType());
        existing.setEffectType(causality.getEffectType());
        existing.setCauseEntityId(causality.getCauseEntityId());
        existing.setEffectEntityId(causality.getEffectEntityId());
        existing.setCauseEntityType(causality.getCauseEntityType());
        existing.setEffectEntityType(causality.getEffectEntityType());
        existing.setRelationship(causality.getRelationship());
        existing.setStrength(causality.getStrength());
        existing.setConditions(causality.getConditions());
        existing.setTags(causality.getTags());
        existing.setStatus(causality.getStatus());
        existing.setUpdateBy(userId);
        existing.setUpdateTime(LocalDateTime.now());
        return updateById(existing);
    }

    @Override
    @Transactional
    public boolean deleteCausality(Long id, Long userId) {
        Causality existing = getCausality(id, userId);
        if (existing == null) {
            return false;
        }
        return removeById(id);
    }
}
