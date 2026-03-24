package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.entity.SystemConfig;

import java.util.List;

public interface SystemConfigService extends IService<SystemConfig> {
    List<SystemConfig> listMergedConfigs();
    List<SystemConfig> saveConfigs(List<SystemConfig> configs);
    String getConfigValue(String configKey);
}
