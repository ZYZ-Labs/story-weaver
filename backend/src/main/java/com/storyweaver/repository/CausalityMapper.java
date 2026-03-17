package com.storyweaver.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storyweaver.domain.entity.Causality;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CausalityMapper extends BaseMapper<Causality> {
    
    @Select("SELECT * FROM causality WHERE project_id = #{projectId} AND deleted = 0 ORDER BY create_time DESC")
    List<Causality> findByProjectId(@Param("projectId") Long projectId);
    
    @Select("SELECT * FROM causality WHERE project_id = #{projectId} AND cause_entity_id = #{entityId} AND deleted = 0")
    List<Causality> findCausesByEntity(@Param("projectId") Long projectId, @Param("entityId") String entityId);
    
    @Select("SELECT * FROM causality WHERE project_id = #{projectId} AND effect_entity_id = #{entityId} AND deleted = 0")
    List<Causality> findEffectsByEntity(@Param("projectId") Long projectId, @Param("entityId") String entityId);
    
    @Select("SELECT * FROM causality WHERE project_id = #{projectId} AND (cause_entity_id = #{entityId} OR effect_entity_id = #{entityId}) AND deleted = 0")
    List<Causality> findRelatedCausalities(@Param("projectId") Long projectId, @Param("entityId") String entityId);
    
    @Select("SELECT * FROM causality WHERE project_id = #{projectId} AND relationship LIKE CONCAT('%', #{keyword}, '%') AND deleted = 0")
    List<Causality> searchByKeyword(@Param("projectId") Long projectId, @Param("keyword") String keyword);
}