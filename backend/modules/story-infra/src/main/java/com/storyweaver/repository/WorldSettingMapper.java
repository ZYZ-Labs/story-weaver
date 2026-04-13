package com.storyweaver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storyweaver.domain.entity.WorldSetting;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorldSettingMapper extends BaseMapper<WorldSetting> {
}