package com.storyweaver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storyweaver.domain.entity.Outline;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OutlineMapper extends BaseMapper<Outline> {

    @Select("""
            SELECT * FROM chapter_outline
            WHERE project_id = #{projectId}
              AND deleted = 0
            ORDER BY order_num ASC, id ASC
            """)
    List<Outline> findByProjectId(@Param("projectId") Long projectId);

    @Select("""
            SELECT * FROM chapter_outline
            WHERE chapter_id = #{chapterId}
              AND deleted = 0
            ORDER BY order_num ASC, id ASC
            """)
    List<Outline> findByChapterId(@Param("chapterId") Long chapterId);
}
