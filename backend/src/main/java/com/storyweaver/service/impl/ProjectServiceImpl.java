package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.repository.ProjectMapper;
import com.storyweaver.service.ProjectService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {
    @Override
    public List<Project> getUserProjects(Long userId) {
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("deleted", 0)
                   .orderByDesc("update_time");
        return list(queryWrapper);
    }

    @Override
    public Project createProject(Long userId, String name, String description, String genre, String tags) {
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setGenre(genre);
        project.setTags(tags);
        project.setUserId(userId);
        project.setStatus(1);
        
        save(project);
        return project;
    }

    @Override
    public boolean updateProject(Long projectId, Long userId, Project project) {
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", projectId)
                   .eq("user_id", userId)
                   .eq("deleted", 0);
        
        Project existing = getOne(queryWrapper);
        if (existing == null) {
            return false;
        }
        
        existing.setName(project.getName());
        existing.setDescription(project.getDescription());
        existing.setGenre(project.getGenre());
        existing.setTags(project.getTags());
        existing.setCoverImage(project.getCoverImage());
        
        return updateById(existing);
    }

    @Override
    public boolean deleteProject(Long projectId, Long userId) {
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", projectId)
                   .eq("user_id", userId)
                   .eq("deleted", 0);
        
        Project project = getOne(queryWrapper);
        if (project == null) {
            return false;
        }
        
        return removeById(projectId);
    }
}