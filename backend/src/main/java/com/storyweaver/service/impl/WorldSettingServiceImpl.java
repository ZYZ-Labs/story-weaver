package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.dto.WorldSettingDTO;
import com.storyweaver.domain.entity.WorldSetting;
import com.storyweaver.domain.vo.WorldSettingVO;
import com.storyweaver.repository.WorldSettingMapper;
import com.storyweaver.service.WorldSettingService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorldSettingServiceImpl extends ServiceImpl<WorldSettingMapper, WorldSetting> implements WorldSettingService {
    
    @Override
    public List<WorldSettingVO> getWorldSettingsByProjectId(Long projectId) {
        LambdaQueryWrapper<WorldSetting> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorldSetting::getProjectId, projectId)
                   .eq(WorldSetting::getDeleted, 0)
                   .orderByDesc(WorldSetting::getUpdateTime);
        
        List<WorldSetting> worldSettings = this.list(queryWrapper);
        
        return worldSettings.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }
    
    @Override
    public WorldSettingVO getWorldSettingById(Long id) {
        WorldSetting worldSetting = this.getById(id);
        if (worldSetting == null || worldSetting.getDeleted() == 1) {
            return null;
        }
        return convertToVO(worldSetting);
    }
    
    @Override
    public WorldSettingVO createWorldSetting(WorldSettingDTO worldSettingDTO) {
        WorldSetting worldSetting = new WorldSetting();
        BeanUtils.copyProperties(worldSettingDTO, worldSetting);
        
        this.save(worldSetting);
        return convertToVO(worldSetting);
    }
    
    @Override
    public WorldSettingVO updateWorldSetting(Long id, WorldSettingDTO worldSettingDTO) {
        WorldSetting worldSetting = this.getById(id);
        if (worldSetting == null || worldSetting.getDeleted() == 1) {
            return null;
        }
        
        BeanUtils.copyProperties(worldSettingDTO, worldSetting);
        worldSetting.setId(id);
        
        this.updateById(worldSetting);
        return convertToVO(worldSetting);
    }
    
    @Override
    public void deleteWorldSetting(Long id) {
        WorldSetting worldSetting = this.getById(id);
        if (worldSetting != null) {
            worldSetting.setDeleted(1);
            this.updateById(worldSetting);
        }
    }
    
    private WorldSettingVO convertToVO(WorldSetting worldSetting) {
        WorldSettingVO vo = new WorldSettingVO();
        BeanUtils.copyProperties(worldSetting, vo);
        return vo;
    }
}