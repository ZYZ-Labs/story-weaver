package com.storyweaver.controller;

import com.storyweaver.domain.entity.Character;
import com.storyweaver.service.CharacterService;
import com.storyweaver.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/characters")
public class CharacterController {
    @Autowired
    private CharacterService characterService;
    
    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProjectCharacters(
            @PathVariable Long projectId,
            HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        List<Character> characters = characterService.getProjectCharacters(projectId, userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", characters);
        
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCharacter(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> requestBody,
            HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        String name = requestBody.get("name");
        String description = requestBody.get("description");
        String attributes = requestBody.get("attributes");
        
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "code", 400,
                "message", "角色名称不能为空"
            ));
        }
        
        Character character = characterService.createCharacter(projectId, userId, name, description, attributes);
        if (character == null) {
            return ResponseEntity.status(404).body(Map.of(
                "code", 404,
                "message", "项目不存在或无权访问"
            ));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "创建成功");
        result.put("data", character);
        
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{characterId}")
    public ResponseEntity<Map<String, Object>> updateCharacter(
            @PathVariable Long projectId,
            @PathVariable Long characterId,
            @RequestBody Character character,
            HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        boolean success = characterService.updateCharacter(characterId, userId, character);
        if (!success) {
            return ResponseEntity.status(404).body(Map.of(
                "code", 404,
                "message", "角色不存在或无权访问"
            ));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功");
        
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{characterId}")
    public ResponseEntity<Map<String, Object>> deleteCharacter(
            @PathVariable Long projectId,
            @PathVariable Long characterId,
            HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        boolean success = characterService.deleteCharacter(characterId, userId);
        if (!success) {
            return ResponseEntity.status(404).body(Map.of(
                "code", 404,
                "message", "角色不存在或无权访问"
            ));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除成功");
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{characterId}")
    public ResponseEntity<Map<String, Object>> getCharacter(
            @PathVariable Long projectId,
            @PathVariable Long characterId,
            HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        Character character = characterService.getCharacterWithAuth(characterId, userId);
        if (character == null) {
            return ResponseEntity.status(404).body(Map.of(
                "code", 404,
                "message", "角色不存在或无权访问"
            ));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", character);
        
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