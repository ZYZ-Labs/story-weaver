package com.storyweaver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storyweaver.domain.entity.AIDirectorDecision;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AIDirectorDecisionMapper extends BaseMapper<AIDirectorDecision> {

    @Select("""
            SELECT *
            FROM ai_director_decision
            WHERE chapter_id = #{chapterId}
              AND user_id = #{userId}
              AND deleted = 0
            ORDER BY create_time DESC
            LIMIT 1
            """)
    AIDirectorDecision findLatestByChapterIdAndUserId(
            @Param("chapterId") Long chapterId,
            @Param("userId") Long userId);
}
