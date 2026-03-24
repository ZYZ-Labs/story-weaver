package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.dto.ProjectRequestDTO;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.repository.ProjectMapper;
import com.storyweaver.service.ProjectService;
import com.storyweaver.service.WorldSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final WorldSettingService worldSettingService;

    public ProjectServiceImpl(WorldSettingService worldSettingService) {
        this.worldSettingService = worldSettingService;
    }

    @Override
    public List<Project> getUserProjects(Long userId) {
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("deleted", 0)
                .orderByDesc("update_time");
        return list(queryWrapper);
    }

    @Override
    @Transactional
    public Project createProject(Long userId, ProjectRequestDTO requestDTO) {
        Project project = new Project();
        project.setName(requestDTO.getName().trim());
        project.setDescription(trimOrNull(requestDTO.getDescription()));
        project.setGenre(trimOrNull(requestDTO.getGenre()));
        project.setTags(trimOrNull(requestDTO.getTags()));
        project.setUserId(userId);
        project.setStatus(1);

        save(project);
        worldSettingService.syncProjectAssociations(project.getId(), userId, requestDTO.getWorldSettingIds());
        return project;
    }

    @Override
    @Transactional
    public boolean updateProject(Long projectId, Long userId, ProjectRequestDTO requestDTO) {
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", projectId)
                .eq("user_id", userId)
                .eq("deleted", 0);

        Project existing = getOne(queryWrapper);
        if (existing == null) {
            return false;
        }

        if (StringUtils.hasText(requestDTO.getName())) {
            existing.setName(requestDTO.getName().trim());
        }
        existing.setDescription(trimOrNull(requestDTO.getDescription()));
        existing.setGenre(trimOrNull(requestDTO.getGenre()));
        existing.setTags(trimOrNull(requestDTO.getTags()));

        boolean updated = updateById(existing);
        if (updated) {
            worldSettingService.syncProjectAssociations(projectId, userId, requestDTO.getWorldSettingIds());
        }
        return updated;
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

    @Override
    public boolean hasProjectAccess(Long projectId, Long userId) {
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", projectId)
                .eq("user_id", userId)
                .eq("deleted", 0);
        return count(queryWrapper) > 0;
    }

    private String trimOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
