package com.storyweaver.controller;

import com.storyweaver.domain.dto.WorldSettingDTO;
import com.storyweaver.domain.vo.WorldSettingVO;
import com.storyweaver.service.WorldSettingService;
import com.storyweaver.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/world-settings")
public class WorldSettingController {
    
    @Autowired
    private WorldSettingService worldSettingService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<WorldSettingVO>> getWorldSettingsByProjectId(
            @PathVariable Long projectId,
            HttpServletRequest request) {
        
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        List<WorldSettingVO> worldSettings = worldSettingService.getWorldSettingsByProjectId(projectId);
        return ResponseEntity.ok(worldSettings);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<WorldSettingVO> getWorldSettingById(
            @PathVariable Long id,
            HttpServletRequest request) {
        
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        WorldSettingVO worldSetting = worldSettingService.getWorldSettingById(id);
        if (worldSetting == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(worldSetting);
    }
    
    @PostMapping
    public ResponseEntity<WorldSettingVO> createWorldSetting(
            @Validated @RequestBody WorldSettingDTO worldSettingDTO,
            HttpServletRequest request) {
        
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        WorldSettingVO createdWorldSetting = worldSettingService.createWorldSetting(worldSettingDTO);
        return ResponseEntity.ok(createdWorldSetting);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<WorldSettingVO> updateWorldSetting(
            @PathVariable Long id,
            @Validated @RequestBody WorldSettingDTO worldSettingDTO,
            HttpServletRequest request) {
        
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        WorldSettingVO updatedWorldSetting = worldSettingService.updateWorldSetting(id, worldSettingDTO);
        if (updatedWorldSetting == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(updatedWorldSetting);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorldSetting(
            @PathVariable Long id,
            HttpServletRequest request) {
        
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        worldSettingService.deleteWorldSetting(id);
        return ResponseEntity.ok().build();
    }
    
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}