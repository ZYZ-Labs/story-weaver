package com.storyweaver.controller;

import com.storyweaver.domain.dto.AIWritingRequestDTO;
import com.storyweaver.domain.vo.AIWritingRollbackResponseVO;
import com.storyweaver.domain.vo.AIWritingResponseVO;
import com.storyweaver.domain.vo.AIWritingStreamEventVO;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.service.AIWritingService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/ai-writing")
public class AIWritingController {

    private final AIWritingService aiWritingService;

    public AIWritingController(AIWritingService aiWritingService) {
        this.aiWritingService = aiWritingService;
    }

    @PostMapping("/generate")
    public ResponseEntity<AIWritingResponseVO> generateContent(
            @Validated @RequestBody AIWritingRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {

        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        AIWritingResponseVO response = aiWritingService.generateContent(userId, requestDTO);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/generate-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> generateContentStream(
            @Validated @RequestBody AIWritingRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);

        SseEmitter emitter = new SseEmitter(0L);
        Thread.startVirtualThread(() -> {
            try {
                aiWritingService.streamContent(userId, requestDTO, event -> sendStreamEvent(emitter, event));
            } catch (Exception exception) {
                finishStreamWithError(emitter, AIWritingStreamEventVO.error(resolveMessage(exception)));
            } finally {
                completeEmitterQuietly(emitter);
            }
        });

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
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

        return ResponseEntity.ok(aiWritingService.getRecordsByChapterId(chapterId));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<AIWritingResponseVO>> getRecordsByProjectId(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {

        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }
        SecurityUtils.getCurrentUserId(authentication);

        return ResponseEntity.ok(aiWritingService.getRecordsByProjectId(projectId));
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
    public ResponseEntity<AIWritingResponseVO> rejectGeneratedContent(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {

        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }
        SecurityUtils.getCurrentUserId(authentication);

        AIWritingResponseVO record = aiWritingService.rejectGeneratedContent(id);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(record);
    }

    @PostMapping("/chapter/{chapterId}/rollback-latest-scene")
    public ResponseEntity<AIWritingRollbackResponseVO> rollbackLatestAcceptedScene(
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {

        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }
        SecurityUtils.getCurrentUserId(authentication);

        return ResponseEntity.ok(aiWritingService.rollbackLatestAcceptedScene(chapterId));
    }

    @PostMapping("/chapter/{chapterId}/rollback-all-scenes")
    public ResponseEntity<AIWritingRollbackResponseVO> rollbackAllAcceptedScenes(
            @PathVariable Long chapterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {

        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }
        SecurityUtils.getCurrentUserId(authentication);

        return ResponseEntity.ok(aiWritingService.rollbackAllAcceptedScenes(chapterId));
    }

    private void sendStreamEvent(SseEmitter emitter, AIWritingStreamEventVO event) {
        try {
            emitter.send(SseEmitter.event().name(event.getType()).data(event));
        } catch (IOException | IllegalStateException exception) {
            throw new IllegalStateException("流式连接已中断", exception);
        }
    }

    private void finishStreamWithError(SseEmitter emitter, AIWritingStreamEventVO event) {
        try {
            emitter.send(SseEmitter.event().name(event.getType()).data(event));
        } catch (IOException | IllegalStateException ignored) {
        }
    }

    private void completeEmitterQuietly(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
        }
    }

    private String resolveMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "AI 流式生成失败，请稍后重试" : message;
    }
}
