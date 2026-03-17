package com.storyweaver.domain.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class WorldSettingDTO {
    
    @NotNull(message = "项目ID不能为空")
    private Long projectId;
    
    @NotBlank(message = "设定名称不能为空")
    private String name;
    
    private String description;
    
    private String category;
    
    // Manual getters and setters for IDE compatibility
    public Long getProjectId() {
        return projectId;
    }
    
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
}