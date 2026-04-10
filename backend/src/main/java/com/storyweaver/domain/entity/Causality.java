package com.storyweaver.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("causality")
public class Causality {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long projectId;
    
    private String name;
    
    private String description;
    
    private String causeType;
    
    private String effectType;
    
    private String causeEntityId;
    
    private String effectEntityId;
    
    private String causeEntityType;
    
    private String effectEntityType;
    
    private String relationship;

    private String causalType;

    private String triggerMode;

    private String payoffStatus;

    private String upstreamCauseIdsJson;

    private String downstreamEffectIdsJson;
    
    private Integer strength;
    
    private String conditions;
    
    private String tags;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    private Long createBy;
    
    private Long updateBy;
    
    @TableLogic
    private Integer deleted;
}
