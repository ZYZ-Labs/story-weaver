package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.entity.Project;

import java.util.List;

public interface ProjectService extends IService<Project> {
    List<Project> getUserProjects(Long userId);
    
    Project createProject(Long userId, String name, String description, String genre, String tags);
    
    boolean updateProject(Long projectId, Long userId, Project project);
    
    boolean deleteProject(Long projectId, Long userId);
}