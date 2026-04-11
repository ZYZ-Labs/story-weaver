package com.storyweaver.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("plot")
public class Plot {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long projectId;
    
    private Long chapterId;
    
    private String title;
    
    private String description;
    
    private String content;
    
    private Integer plotType;

    private String storyBeatType;

    private String storyFunction;
    
    private Integer sequence;
    
    private String characters;
    
    private String locations;
    
    private String timeline;
    
    private String conflicts;
    
    private String resolutions;

    private String eventResult;

    private Long prevBeatId;

    private Long nextBeatId;

    private Integer outlinePriority;
    
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
