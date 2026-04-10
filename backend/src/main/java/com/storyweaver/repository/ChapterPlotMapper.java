package com.storyweaver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storyweaver.domain.entity.ChapterPlotLink;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChapterPlotMapper extends BaseMapper<ChapterPlotLink> {
}
