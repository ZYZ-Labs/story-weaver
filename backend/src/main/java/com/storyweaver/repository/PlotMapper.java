package com.storyweaver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storyweaver.domain.entity.Plot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PlotMapper extends BaseMapper<Plot> {
    
    @Select("SELECT * FROM plot WHERE project_id = #{projectId} AND deleted = 0 ORDER BY sequence ASC")
    List<Plot> findByProjectId(@Param("projectId") Long projectId);
    
    @Select("SELECT * FROM plot WHERE chapter_id = #{chapterId} AND deleted = 0 ORDER BY sequence ASC")
    List<Plot> findByChapterId(@Param("chapterId") Long chapterId);
    
    @Select("SELECT * FROM plot WHERE project_id = #{projectId} AND plot_type = #{plotType} AND deleted = 0 ORDER BY sequence ASC")
    List<Plot> findByPlotType(@Param("projectId") Long projectId, @Param("plotType") Integer plotType);
    
    @Select("SELECT * FROM plot WHERE project_id = #{projectId} AND (title LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%')) AND deleted = 0")
    List<Plot> searchByKeyword(@Param("projectId") Long projectId, @Param("keyword") String keyword);
    
    @Select("SELECT MAX(sequence) FROM plot WHERE chapter_id = #{chapterId} AND deleted = 0")
    Integer getMaxSequenceByChapterId(@Param("chapterId") Long chapterId);
    
    @Select("SELECT * FROM plot WHERE project_id = #{projectId} AND characters LIKE CONCAT('%', #{characterName}, '%') AND deleted = 0")
    List<Plot> findByCharacter(@Param("projectId") Long projectId, @Param("characterName") String characterName);
}