package com.storyweaver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storyweaver.domain.entity.AIWritingChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AIWritingChatMessageMapper extends BaseMapper<AIWritingChatMessage> {
}
