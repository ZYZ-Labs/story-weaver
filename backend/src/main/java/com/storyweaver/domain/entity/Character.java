package com.storyweaver.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("`character`")
public class Character {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private Long ownerUserId;

    private String name;

    private String description;

    private String attributes;

    @TableField(exist = false)
    private String projectRole;

    @TableField(exist = false)
    private List<Long> projectIds;

    @TableField(exist = false)
    private List<String> projectNames;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    // Manual getters and setters for IDE compatibility
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAttributes() { return attributes; }
    public void setAttributes(String attributes) { this.attributes = attributes; }

    public String getProjectRole() { return projectRole; }
    public void setProjectRole(String projectRole) { this.projectRole = projectRole; }

    public List<Long> getProjectIds() { return projectIds; }
    public void setProjectIds(List<Long> projectIds) { this.projectIds = projectIds; }

    public List<String> getProjectNames() { return projectNames; }
    public void setProjectNames(List<String> projectNames) { this.projectNames = projectNames; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
