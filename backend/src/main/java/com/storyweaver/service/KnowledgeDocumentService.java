package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.entity.KnowledgeDocument;

import java.util.List;

public interface KnowledgeDocumentService extends IService<KnowledgeDocument> {
    List<KnowledgeDocument> getProjectDocuments(Long projectId, Long userId);
    KnowledgeDocument getDocument(Long id, Long userId);
    KnowledgeDocument createDocument(Long projectId, Long userId, KnowledgeDocument document);
    boolean updateDocument(Long id, Long userId, KnowledgeDocument document);
    boolean deleteDocument(Long id, Long userId);
    List<KnowledgeDocument> queryDocuments(Long projectId, Long userId, String queryText);
}
