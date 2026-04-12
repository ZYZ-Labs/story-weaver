package com.storyweaver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storyweaver.domain.entity.AIWritingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AIWritingRecordMapper extends BaseMapper<AIWritingRecord> {
    @Select("""
            SELECT r.*
            FROM ai_writing_record r
            INNER JOIN chapter c ON c.id = r.chapter_id
            WHERE c.project_id = #{projectId}
              AND c.deleted = 0
              AND r.deleted = 0
            ORDER BY r.create_time DESC
            """)
    List<AIWritingRecord> findByProjectId(@Param("projectId") Long projectId);
}
