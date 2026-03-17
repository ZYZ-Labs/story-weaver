package com.storyweaver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storyweaver.domain.entity.AIWritingRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AIWritingRecordMapper extends BaseMapper<AIWritingRecord> {
}