package com.storyweaver.controller;

import com.storyweaver.domain.dto.AIWritingRequestDTO;
import com.storyweaver.domain.vo.AIWritingResponseVO;
import com.storyweaver.service.AIWritingService;
import com.storyweaver.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-writing")
public class AIWritingController {
    
    @Autowired
    private AIWritingService aiWritingService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/generate")
    public ResponseEntity<AIWritingResponseVO> generateContent(
            @Validated @RequestBody AIWritingRequestDTO requestDTO,
            HttpServletRequest request) {
        
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        try {
            AIWritingResponseVO response = aiWritingService.generateContent(requestDTO);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/chapter/{chapterId}")
    public ResponseEntity<List<AIWritingResponseVO>> getRecordsByChapterId(
            @PathVariable Long chapterId,
            HttpServletRequest request) {
        
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        List<AIWritingResponseVO> records = aiWritingService.getRecordsByChapterId(chapterId);
        return ResponseEntity.ok(records);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AIWritingResponseVO> getRecordById(
            @PathVariable Long id,
            HttpServletRequest request) {
        
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        AIWritingResponseVO record = aiWritingService.getRecordById(id);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(record);
    }
    
    @PostMapping("/{id}/accept")
    public ResponseEntity<AIWritingResponseVO> acceptGeneratedContent(
            @PathVariable Long id,
            HttpServletRequest request) {
        
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        AIWritingResponseVO record = aiWritingService.acceptGeneratedContent(id);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(record);
    }
    
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectGeneratedContent(
            @PathVariable Long id,
            HttpServletRequest request) {
        
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        
        aiWritingService.rejectGeneratedContent(id);
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