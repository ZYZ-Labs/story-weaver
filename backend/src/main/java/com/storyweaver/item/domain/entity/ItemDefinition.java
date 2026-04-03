package com.storyweaver.item.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("item")
public class ItemDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long ownerUserId;

    private String name;

    private String description;

    private String category;

    private String rarity;

    private Integer stackable;

    private Integer maxStack;

    private Integer usable;

    private Integer equippable;

    private String slotType;

    @TableField("item_value")
    private Integer itemValue;

    private Integer weight;

    private String attributesJson;

    private String effectJson;

    private String tags;

    private String sourceType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
