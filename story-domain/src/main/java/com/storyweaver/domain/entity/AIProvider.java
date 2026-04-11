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
@TableName("ai_provider")
public class AIProvider {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String providerType;
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private String embeddingModel;
    private Double temperature;
    private Double topP;
    private Integer maxTokens;
    private Integer timeoutSeconds;
    private Integer enabled;
    private Integer isDefault;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
