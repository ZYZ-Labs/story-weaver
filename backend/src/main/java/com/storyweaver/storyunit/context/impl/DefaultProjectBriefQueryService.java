package com.storyweaver.storyunit.context.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.entity.ProjectWorldSettingLink;
import com.storyweaver.domain.entity.WorldSetting;
import com.storyweaver.repository.ProjectMapper;
import com.storyweaver.repository.ProjectWorldSettingMapper;
import com.storyweaver.repository.WorldSettingMapper;
import com.storyweaver.storyunit.context.ProjectBriefQueryService;
import com.storyweaver.storyunit.context.ProjectBriefView;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DefaultProjectBriefQueryService implements ProjectBriefQueryService {

    private final ProjectMapper projectMapper;
    private final ProjectWorldSettingMapper projectWorldSettingMapper;
    private final WorldSettingMapper worldSettingMapper;

    public DefaultProjectBriefQueryService(
            ProjectMapper projectMapper,
            ProjectWorldSettingMapper projectWorldSettingMapper,
            WorldSettingMapper worldSettingMapper) {
        this.projectMapper = projectMapper;
        this.projectWorldSettingMapper = projectWorldSettingMapper;
        this.worldSettingMapper = worldSettingMapper;
    }

    @Override
    public Optional<ProjectBriefView> getProjectBrief(Long projectId) {
        if (projectId == null) {
            return Optional.empty();
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null || Integer.valueOf(1).equals(project.getDeleted())) {
            return Optional.empty();
        }

        List<String> worldSettingNames = loadWorldSettingNames(projectId);
        String logline = ContextViewSupport.firstNonBlank(
                project.getDescription(),
                ContextViewSupport.joinNonBlank(" / ", project.getGenre(), project.getTags())
        );
        String summary = buildSummary(project, worldSettingNames);
        return Optional.of(new ProjectBriefView(project.getId(), project.getName(), logline, summary));
    }

    private List<String> loadWorldSettingNames(Long projectId) {
        List<ProjectWorldSettingLink> links = projectWorldSettingMapper.selectList(new LambdaQueryWrapper<ProjectWorldSettingLink>()
                .eq(ProjectWorldSettingLink::getProjectId, projectId)
                .orderByDesc(ProjectWorldSettingLink::getUpdateTime)
                .orderByDesc(ProjectWorldSettingLink::getId));
        if (links.isEmpty()) {
            return List.of();
        }

        List<Long> ids = links.stream()
                .map(ProjectWorldSettingLink::getWorldSettingId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Long, WorldSetting> worldSettings = worldSettingMapper.selectBatchIds(ids).stream()
                .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), Map::putAll);

        return ContextViewSupport.sanitizeDistinct(ids.stream()
                .map(worldSettings::get)
                .filter(item -> item != null)
                .map(item -> ContextViewSupport.firstNonBlank(item.getName(), item.getTitle()))
                .toList());
    }

    private String buildSummary(Project project, List<String> worldSettingNames) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(project.getDescription())) {
            builder.append(project.getDescription().trim());
        }

        String metadata = ContextViewSupport.joinNonBlank("；",
                StringUtils.hasText(project.getGenre()) ? "题材：" + project.getGenre().trim() : "",
                StringUtils.hasText(project.getTags()) ? "标签：" + project.getTags().trim() : ""
        );
        if (StringUtils.hasText(metadata)) {
            appendSegment(builder, metadata);
        }
        if (!worldSettingNames.isEmpty()) {
            appendSegment(builder, "关联世界观：" + String.join("、", worldSettingNames));
        }
        return builder.toString();
    }

    private void appendSegment(StringBuilder builder, String segment) {
        if (!StringUtils.hasText(segment)) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(" ");
        }
        builder.append(segment.trim());
    }
}
