package com.storyweaver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.entity.Causality;
import com.storyweaver.repository.CausalityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CausalityService extends ServiceImpl<CausalityMapper, Causality> {
    
    public List<Causality> getProjectCausalities(Long projectId) {
        return baseMapper.findByProjectId(projectId);
    }
    
    public List<Causality> getCausesByEntity(Long projectId, String entityId) {
        return baseMapper.findCausesByEntity(projectId, entityId);
    }
    
    public List<Causality> getEffectsByEntity(Long projectId, String entityId) {
        return baseMapper.findEffectsByEntity(projectId, entityId);
    }
    
    public List<Causality> getRelatedCausalities(Long projectId, String entityId) {
        return baseMapper.findRelatedCausalities(projectId, entityId);
    }
    
    public List<Causality> searchCausalities(Long projectId, String keyword) {
        return baseMapper.searchByKeyword(projectId, keyword);
    }
    
    @Transactional
    public boolean createCausality(Causality causality, Long userId) {
        causality.setCreateTime(LocalDateTime.now());
        causality.setUpdateTime(LocalDateTime.now());
        causality.setCreateBy(userId);
        causality.setUpdateBy(userId);
        causality.setDeleted(0);
        causality.setStatus(1);
        
        return save(causality);
    }
    
    @Transactional
    public boolean updateCausality(Causality causality, Long userId) {
        causality.setUpdateTime(LocalDateTime.now());
        causality.setUpdateBy(userId);
        
        return updateById(causality);
    }
    
    @Transactional
    public boolean deleteCausality(Long causalityId) {
        Causality causality = getById(causalityId);
        if (causality == null) {
            return false;
        }
        
        causality.setDeleted(1);
        return updateById(causality);
    }
    
    public Causality getCausalityById(Long causalityId) {
        LambdaQueryWrapper<Causality> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Causality::getId, causalityId)
                   .eq(Causality::getDeleted, 0);
        return getOne(queryWrapper);
    }
    
    public boolean checkCausalityExists(Long projectId, String causeEntityId, String effectEntityId, String relationship) {
        LambdaQueryWrapper<Causality> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Causality::getProjectId, projectId)
                   .eq(Causality::getCauseEntityId, causeEntityId)
                   .eq(Causality::getEffectEntityId, effectEntityId)
                   .eq(Causality::getRelationship, relationship)
                   .eq(Causality::getDeleted, 0);
        return count(queryWrapper) > 0;
    }
    
    public List<Causality> analyzeCausalChain(Long projectId, String startEntityId, int maxDepth) {
        // This is a simplified implementation
        // In a real system, you would implement graph traversal algorithms
        return baseMapper.findRelatedCausalities(projectId, startEntityId);
    }
}