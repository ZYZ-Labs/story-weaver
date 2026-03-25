package com.storyweaver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storyweaver.domain.entity.ChapterCharacterLink;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChapterCharacterMapper extends BaseMapper<ChapterCharacterLink> {
}
