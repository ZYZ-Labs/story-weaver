package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.dto.WorldSettingDTO;
import com.storyweaver.domain.entity.WorldSetting;
import com.storyweaver.domain.vo.WorldSettingVO;

import java.util.List;

public interface WorldSettingService extends IService<WorldSetting> {
    
    List<WorldSettingVO> getWorldSettingsByProjectId(Long projectId);
    
    WorldSettingVO getWorldSettingById(Long id);
    
    WorldSettingVO createWorldSetting(WorldSettingDTO worldSettingDTO);
    
    WorldSettingVO updateWorldSetting(Long id, WorldSettingDTO worldSettingDTO);
    
    void deleteWorldSetting(Long id);
}