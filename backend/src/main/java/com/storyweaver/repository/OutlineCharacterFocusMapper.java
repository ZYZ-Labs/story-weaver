package com.storyweaver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storyweaver.domain.entity.OutlineCharacterFocusLink;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutlineCharacterFocusMapper extends BaseMapper<OutlineCharacterFocusLink> {
}
