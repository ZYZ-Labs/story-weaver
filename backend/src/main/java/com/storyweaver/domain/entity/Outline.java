package com.storyweaver.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("chapter_outline")
public class Outline {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String outlineType;

    private Long parentOutlineId;

    private Long rootOutlineId;

    private Long chapterId;

    private Long generatedChapterId;

    private String title;

    private String summary;

    private String content;

    private String stageGoal;

    private String keyConflict;

    private String turningPoints;

    private String expectedEnding;

    private String focusCharacterIds;

    private String relatedPlotIds;

    private String relatedCausalityIds;

    private String relatedWorldSettingIdsJson;

    private Integer status;

    private Integer orderNum;

    @TableField(exist = false)
    private String chapterTitle;

    @TableField(exist = false)
    private List<Outline> children;

    @TableField(exist = false)
    private List<Long> focusCharacterIdList;

    @TableField(exist = false)
    private List<String> focusCharacterNames;

    @TableField(exist = false)
    private List<Long> relatedPlotIdList;

    @TableField(exist = false)
    private List<String> relatedPlotTitles;

    @TableField(exist = false)
    private List<Long> relatedCausalityIdList;

    @TableField(exist = false)
    private List<String> relatedCausalityNames;

    @TableField(exist = false)
    private List<Long> relatedWorldSettingIdList;

    @TableField(exist = false)
    private List<String> relatedWorldSettingNames;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
