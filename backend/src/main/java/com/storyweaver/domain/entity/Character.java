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

    private String identity;

    private String coreGoal;

    private String growthArc;

    private Long firstAppearanceChapterId;

    private String activeStage;

    private Integer isRetired;

    private String attributes;

    private String advancedProfileJson;

    @TableField(exist = false)
    private String projectRole;

    @TableField(exist = false)
    private String roleType;

    @TableField(exist = false)
    private List<Long> projectIds;

    @TableField(exist = false)
    private List<String> projectNames;

    @TableField(exist = false)
    private Integer inventoryItemCount;

    @TableField(exist = false)
    private Integer equippedItemCount;

    @TableField(exist = false)
    private Integer rareItemCount;

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

    public String getIdentity() { return identity; }
    public void setIdentity(String identity) { this.identity = identity; }

    public String getCoreGoal() { return coreGoal; }
    public void setCoreGoal(String coreGoal) { this.coreGoal = coreGoal; }

    public String getGrowthArc() { return growthArc; }
    public void setGrowthArc(String growthArc) { this.growthArc = growthArc; }

    public Long getFirstAppearanceChapterId() { return firstAppearanceChapterId; }
    public void setFirstAppearanceChapterId(Long firstAppearanceChapterId) { this.firstAppearanceChapterId = firstAppearanceChapterId; }

    public String getActiveStage() { return activeStage; }
    public void setActiveStage(String activeStage) { this.activeStage = activeStage; }

    public Integer getIsRetired() { return isRetired; }
    public void setIsRetired(Integer isRetired) { this.isRetired = isRetired; }

    public String getAttributes() { return attributes; }
    public void setAttributes(String attributes) { this.attributes = attributes; }

    public String getAdvancedProfileJson() { return advancedProfileJson; }
    public void setAdvancedProfileJson(String advancedProfileJson) { this.advancedProfileJson = advancedProfileJson; }

    public String getProjectRole() { return projectRole; }
    public void setProjectRole(String projectRole) { this.projectRole = projectRole; }

    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }

    public List<Long> getProjectIds() { return projectIds; }
    public void setProjectIds(List<Long> projectIds) { this.projectIds = projectIds; }

    public List<String> getProjectNames() { return projectNames; }
    public void setProjectNames(List<String> projectNames) { this.projectNames = projectNames; }

    public Integer getInventoryItemCount() { return inventoryItemCount; }
    public void setInventoryItemCount(Integer inventoryItemCount) { this.inventoryItemCount = inventoryItemCount; }

    public Integer getEquippedItemCount() { return equippedItemCount; }
    public void setEquippedItemCount(Integer equippedItemCount) { this.equippedItemCount = equippedItemCount; }

    public Integer getRareItemCount() { return rareItemCount; }
    public void setRareItemCount(Integer rareItemCount) { this.rareItemCount = rareItemCount; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
