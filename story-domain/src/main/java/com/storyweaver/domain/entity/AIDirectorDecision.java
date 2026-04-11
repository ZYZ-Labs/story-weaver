package com.storyweaver.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_director_decision")
public class AIDirectorDecision {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long chapterId;

    private Long userId;

    private String sourceType;

    private String entryPoint;

    private String stage;

    private String writingMode;

    private Integer targetWordCount;

    private String selectedModulesJson;

    private String moduleWeightsJson;

    private String requiredFactsJson;

    private String prohibitedMovesJson;

    private String decisionPackJson;

    private String toolTraceJson;

    private Long selectedProviderId;

    private String selectedModel;

    private String status;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
