package com.storyweaver.controller;

import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.service.ChapterService;
import com.storyweaver.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/chapters")
public class ChapterController {
    @Autowired
    private ChapterService chapterService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProjectChapters(
            @PathVariable Long projectId,
            HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        List<Chapter> chapters = chapterService.getProjectChapters(projectId, userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", chapters);
        
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createChapter(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        String title = requestBody.get("title");
        String content = requestBody.get("content");
        Integer orderNum = requestBody.get("orderNum") != null ? 
                          Integer.parseInt(requestBody.get("orderNum")) : null;
        
        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "code", 400,
                "message", "章节标题不能为空"
            ));
        }
        
        Chapter chapter = chapterService.createChapter(projectId, userId, title, content, orderNum);
        if (chapter == null) {
            return ResponseEntity.status(404).body(Map.of(
                "code", 404,
                "message", "项目不存在或无权访问"
            ));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "创建成功");
        result.put("data", chapter);
        
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{chapterId}")
    public ResponseEntity<Map<String, Object>> updateChapter(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestBody Chapter chapter,
            HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        boolean success = chapterService.updateChapter(chapterId, userId, chapter);
        if (!success) {
            return ResponseEntity.status(404).body(Map.of(
                "code", 404,
                "message", "章节不存在或无权访问"
            ));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功");
        
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{chapterId}")
    public ResponseEntity<Map<String, Object>> deleteChapter(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        boolean success = chapterService.deleteChapter(chapterId, userId);
        if (!success) {
            return ResponseEntity.status(404).body(Map.of(
                "code", 404,
                "message", "章节不存在或无权访问"
            ));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除成功");
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{chapterId}")
    public ResponseEntity<Map<String, Object>> getChapter(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        Chapter chapter = chapterService.getChapterWithAuth(chapterId, userId);
        if (chapter == null) {
            return ResponseEntity.status(404).body(Map.of(
                "code", 404,
                "message", "章节不存在或无权访问"
            ));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", chapter);
        
        return ResponseEntity.ok(result);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}