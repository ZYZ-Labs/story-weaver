package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.dto.ProjectRequestDTO;
import com.storyweaver.domain.entity.AIWritingRecord;
import com.storyweaver.domain.entity.Causality;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.ChapterCharacterLink;
import com.storyweaver.domain.entity.KnowledgeDocument;
import com.storyweaver.domain.entity.Outline;
import com.storyweaver.domain.entity.Plot;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.entity.ProjectCharacterLink;
import com.storyweaver.domain.entity.ProjectWorldSettingLink;
import com.storyweaver.repository.AIWritingRecordMapper;
import com.storyweaver.repository.CausalityMapper;
import com.storyweaver.repository.ChapterMapper;
import com.storyweaver.repository.KnowledgeDocumentMapper;
import com.storyweaver.repository.OutlineMapper;
import com.storyweaver.repository.PlotMapper;
import com.storyweaver.domain.vo.WorldSettingVO;
import com.storyweaver.repository.ChapterCharacterMapper;
import com.storyweaver.repository.ProjectMapper;
import com.storyweaver.repository.ProjectCharacterMapper;
import com.storyweaver.repository.ProjectWorldSettingMapper;
import com.storyweaver.service.ProjectService;
import com.storyweaver.service.WorldSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService {

    private final WorldSettingService worldSettingService;
    private final ChapterMapper chapterMapper;
    private final PlotMapper plotMapper;
    private final OutlineMapper outlineMapper;
    private final CausalityMapper causalityMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final AIWritingRecordMapper aiWritingRecordMapper;
    private final ProjectWorldSettingMapper projectWorldSettingMapper;
    private final ProjectCharacterMapper projectCharacterMapper;
    private final ChapterCharacterMapper chapterCharacterMapper;

    public ProjectServiceImpl(
            WorldSettingService worldSettingService,
            ChapterMapper chapterMapper,
            PlotMapper plotMapper,
            OutlineMapper outlineMapper,
            CausalityMapper causalityMapper,
            KnowledgeDocumentMapper knowledgeDocumentMapper,
            AIWritingRecordMapper aiWritingRecordMapper,
            ProjectWorldSettingMapper projectWorldSettingMapper,
            ProjectCharacterMapper projectCharacterMapper,
            ChapterCharacterMapper chapterCharacterMapper) {
        this.worldSettingService = worldSettingService;
        this.chapterMapper = chapterMapper;
        this.plotMapper = plotMapper;
        this.outlineMapper = outlineMapper;
        this.causalityMapper = causalityMapper;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.aiWritingRecordMapper = aiWritingRecordMapper;
        this.projectWorldSettingMapper = projectWorldSettingMapper;
        this.projectCharacterMapper = projectCharacterMapper;
        this.chapterCharacterMapper = chapterCharacterMapper;
    }

    @Override
    public List<Project> getUserProjects(Long userId) {
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("deleted", 0)
                .orderByDesc("update_time");
        List<Project> projects = list(queryWrapper);
        attachWorldSettingSummaries(projects);
        return projects;
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
        attachWorldSettingSummaries(List.of(project));
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
    @Transactional
    public boolean deleteProject(Long projectId, Long userId) {
        QueryWrapper<Project> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", projectId)
                .eq("user_id", userId)
                .eq("deleted", 0);

        Project project = getOne(queryWrapper);
        if (project == null) {
            return false;
        }

        List<Long> chapterIds = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                        .eq(Chapter::getProjectId, projectId)
                        .eq(Chapter::getDeleted, 0))
                .stream()
                .map(Chapter::getId)
                .collect(Collectors.toList());

        if (!chapterIds.isEmpty()) {
            aiWritingRecordMapper.delete(new LambdaQueryWrapper<AIWritingRecord>()
                    .in(AIWritingRecord::getChapterId, chapterIds)
                    .eq(AIWritingRecord::getDeleted, 0));

            chapterCharacterMapper.delete(new LambdaQueryWrapper<ChapterCharacterLink>()
                    .in(ChapterCharacterLink::getChapterId, chapterIds));
        }

        chapterMapper.delete(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, projectId)
                .eq(Chapter::getDeleted, 0));

        plotMapper.delete(new LambdaQueryWrapper<Plot>()
                .eq(Plot::getProjectId, projectId)
                .eq(Plot::getDeleted, 0));

        outlineMapper.delete(new LambdaQueryWrapper<Outline>()
                .eq(Outline::getProjectId, projectId)
                .eq(Outline::getDeleted, 0));

        causalityMapper.delete(new LambdaQueryWrapper<Causality>()
                .eq(Causality::getProjectId, projectId)
                .eq(Causality::getDeleted, 0));

        knowledgeDocumentMapper.delete(new LambdaQueryWrapper<KnowledgeDocument>()
                .eq(KnowledgeDocument::getProjectId, projectId)
                .eq(KnowledgeDocument::getDeleted, 0));

        // Keep reusable world-setting models themselves, only remove the project associations.
        projectWorldSettingMapper.delete(new QueryWrapper<ProjectWorldSettingLink>()
                .eq("project_id", projectId));

        // Characters remain reusable in the library, but the project bindings should be removed.
        projectCharacterMapper.delete(new QueryWrapper<ProjectCharacterLink>()
                .eq("project_id", projectId));

        // Characters are intentionally retained for future reuse, so they are not cascade deleted here.
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

    private void attachWorldSettingSummaries(List<Project> projects) {
        for (Project project : projects) {
            List<WorldSettingVO> worldSettings = worldSettingService.getWorldSettingsByProjectId(project.getId());
            project.setWorldSettingIds(worldSettings.stream()
                    .map(WorldSettingVO::getId)
                    .collect(Collectors.toList()));
            project.setWorldSettingNames(worldSettings.stream()
                    .map(item -> StringUtils.hasText(item.getName()) ? item.getName() : item.getTitle())
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList()));
        }
    }
}
