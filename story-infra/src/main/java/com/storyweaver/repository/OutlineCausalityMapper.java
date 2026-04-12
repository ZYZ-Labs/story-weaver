package com.storyweaver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storyweaver.domain.entity.OutlineCausalityLink;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutlineCausalityMapper extends BaseMapper<OutlineCausalityLink> {
}
