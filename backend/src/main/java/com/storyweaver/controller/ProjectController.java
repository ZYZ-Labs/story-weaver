package com.storyweaver.controller;

import com.storyweaver.domain.entity.Project;
import com.storyweaver.service.ProjectService;
import com.storyweaver.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserProjects(HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        List<Project> projects = projectService.getUserProjects(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", projects);
        
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProject(
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        String name = requestBody.get("name");
        String description = requestBody.get("description");
        String genre = requestBody.get("genre");
        String tags = requestBody.get("tags");
        
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "code", 400,
                "message", "项目名称不能为空"
            ));
        }
        
        Project project = projectService.createProject(userId, name, description, genre, tags);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "创建成功");
        result.put("data", project);
        
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProject(
            @PathVariable Long id,
            @RequestBody Project project,
            HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        boolean success = projectService.updateProject(id, userId, project);
        if (!success) {
            return ResponseEntity.status(404).body(Map.of(
                "code", 404,
                "message", "项目不存在或无权访问"
            ));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功");
        
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteProject(
            @PathVariable Long id,
            HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        boolean success = projectService.deleteProject(id, userId);
        if (!success) {
            return ResponseEntity.status(404).body(Map.of(
                "code", 404,
                "message", "项目不存在或无权访问"
            ));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除成功");
        
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
