package com.storyweaver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storyweaver.domain.entity.Chapter;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChapterMapper extends BaseMapper<Chapter> {
}