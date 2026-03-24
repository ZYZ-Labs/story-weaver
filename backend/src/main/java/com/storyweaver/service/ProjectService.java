package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.dto.ProjectRequestDTO;
import com.storyweaver.domain.entity.Project;

import java.util.List;

public interface ProjectService extends IService<Project> {
    List<Project> getUserProjects(Long userId);

    Project createProject(Long userId, ProjectRequestDTO requestDTO);

    boolean updateProject(Long projectId, Long userId, ProjectRequestDTO requestDTO);

    boolean deleteProject(Long projectId, Long userId);

    boolean hasProjectAccess(Long projectId, Long userId);
}
