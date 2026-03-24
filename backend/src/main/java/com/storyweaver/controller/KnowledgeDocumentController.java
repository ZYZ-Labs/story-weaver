package com.storyweaver.controller;

import com.storyweaver.domain.entity.KnowledgeDocument;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.service.KnowledgeDocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService knowledgeDocumentService;

    public KnowledgeDocumentController(KnowledgeDocumentService knowledgeDocumentService) {
        this.knowledgeDocumentService = knowledgeDocumentService;
    }

    @GetMapping("/api/projects/{projectId}/knowledge/documents")
    public ResponseEntity<Map<String, Object>> getProjectDocuments(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(Map.of("code", 200, "message", "获取成功", "data", knowledgeDocumentService.getProjectDocuments(projectId, userId)));
    }

    @PostMapping("/api/projects/{projectId}/knowledge/documents")
    public ResponseEntity<Map<String, Object>> createDocument(
            @PathVariable Long projectId,
            @RequestBody KnowledgeDocument document,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        KnowledgeDocument created = knowledgeDocumentService.createDocument(projectId, userId, document);
        if (created == null) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "项目不存在或无权访问"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "创建成功", "data", created));
    }

    @GetMapping("/api/knowledge/documents/{id}")
    public ResponseEntity<Map<String, Object>> getDocument(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        KnowledgeDocument document = knowledgeDocumentService.getDocument(id, userId);
        if (document == null) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "知识文档不存在或无权访问"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "获取成功", "data", document));
    }

    @PutMapping("/api/knowledge/documents/{id}")
    public ResponseEntity<Map<String, Object>> updateDocument(
            @PathVariable Long id,
            @RequestBody KnowledgeDocument document,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        if (!knowledgeDocumentService.updateDocument(id, userId, document)) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "知识文档不存在或无权访问"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功"));
    }

    @DeleteMapping("/api/knowledge/documents/{id}")
    public ResponseEntity<Map<String, Object>> deleteDocument(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        if (!knowledgeDocumentService.deleteDocument(id, userId)) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "知识文档不存在或无权访问"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }

    @PostMapping("/api/projects/{projectId}/rag/query")
    public ResponseEntity<Map<String, Object>> queryDocuments(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        String queryText = payload.getOrDefault("query", "");
        List<KnowledgeDocument> results = knowledgeDocumentService.queryDocuments(projectId, userId, queryText);
        return ResponseEntity.ok(Map.of("code", 200, "message", "检索成功", "data", results));
    }

    @PostMapping("/api/projects/{projectId}/rag/reindex")
    public ResponseEntity<Map<String, Object>> reindexDocuments(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        List<KnowledgeDocument> documents = knowledgeDocumentService.getProjectDocuments(projectId, userId);
        return ResponseEntity.ok(Map.of("code", 200, "message", "重建索引任务已模拟完成", "data", Map.of("documentCount", documents.size(), "status", "completed")));
    }
}
