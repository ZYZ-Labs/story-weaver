package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.entity.KnowledgeDocument;
import com.storyweaver.repository.KnowledgeDocumentMapper;
import com.storyweaver.service.KnowledgeDocumentService;
import com.storyweaver.service.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument> implements KnowledgeDocumentService {

    private final ProjectService projectService;

    public KnowledgeDocumentServiceImpl(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public List<KnowledgeDocument> getProjectDocuments(Long projectId, Long userId) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return List.of();
        }
        QueryWrapper<KnowledgeDocument> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id", projectId).eq("deleted", 0).orderByDesc("update_time");
        return list(queryWrapper);
    }

    @Override
    public KnowledgeDocument getDocument(Long id, Long userId) {
        KnowledgeDocument document = getById(id);
        if (document == null || !projectService.hasProjectAccess(document.getProjectId(), userId)) {
            return null;
        }
        return document;
    }

    @Override
    @Transactional
    public KnowledgeDocument createDocument(Long projectId, Long userId, KnowledgeDocument document) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return null;
        }
        document.setId(null);
        document.setProjectId(projectId);
        if (document.getStatus() == null) {
            document.setStatus("ready");
        }
        save(document);
        return document;
    }

    @Override
    @Transactional
    public boolean updateDocument(Long id, Long userId, KnowledgeDocument document) {
        KnowledgeDocument existing = getDocument(id, userId);
        if (existing == null) {
            return false;
        }
        existing.setSourceType(document.getSourceType());
        existing.setSourceRefId(document.getSourceRefId());
        existing.setTitle(document.getTitle());
        existing.setContentText(document.getContentText());
        existing.setSummary(document.getSummary());
        existing.setStatus(document.getStatus());
        return updateById(existing);
    }

    @Override
    @Transactional
    public boolean deleteDocument(Long id, Long userId) {
        KnowledgeDocument existing = getDocument(id, userId);
        if (existing == null) {
            return false;
        }
        return removeById(id);
    }

    @Override
    public List<KnowledgeDocument> queryDocuments(Long projectId, Long userId, String queryText) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return List.of();
        }
        QueryWrapper<KnowledgeDocument> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id", projectId)
                .eq("deleted", 0)
                .and(wrapper -> wrapper.like("title", queryText)
                        .or().like("summary", queryText)
                        .or().like("content_text", queryText))
                .last("LIMIT 10");
        return list(queryWrapper);
    }
}
