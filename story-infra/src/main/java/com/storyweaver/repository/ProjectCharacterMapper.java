package com.storyweaver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storyweaver.domain.entity.ProjectCharacterLink;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectCharacterMapper extends BaseMapper<ProjectCharacterLink> {
}
