package com.storyweaver.controller;

import com.storyweaver.domain.dto.AIWritingRequestDTO;
import com.storyweaver.domain.vo.AIWritingResponseVO;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.service.AIWritingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-writing")
public class AIWritingController {

    @Autowired
    private AIWritingService aiWritingService;

    @PostMapping("/generate")
    public ResponseEntity<AIWritingResponseVO> generateContent(
            @Validated @RequestBody AIWritingRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {

        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }
        SecurityUtils.getCurrentUserId(authentication);

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
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {

        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }
        SecurityUtils.getCurrentUserId(authentication);

        List<AIWritingResponseVO> records = aiWritingService.getRecordsByChapterId(chapterId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AIWritingResponseVO> getRecordById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {

        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }
        SecurityUtils.getCurrentUserId(authentication);

        AIWritingResponseVO record = aiWritingService.getRecordById(id);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(record);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<AIWritingResponseVO> acceptGeneratedContent(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {

        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }
        SecurityUtils.getCurrentUserId(authentication);

        AIWritingResponseVO record = aiWritingService.acceptGeneratedContent(id);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(record);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectGeneratedContent(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {

        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }
        SecurityUtils.getCurrentUserId(authentication);

        aiWritingService.rejectGeneratedContent(id);
        return ResponseEntity.ok().build();
    }
}
