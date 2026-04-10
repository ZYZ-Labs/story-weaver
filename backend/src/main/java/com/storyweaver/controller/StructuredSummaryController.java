package com.storyweaver.controller;

import com.storyweaver.domain.dto.StructuredSummaryApplyRequestDTO;
import com.storyweaver.domain.dto.SummarySuggestionRequestDTO;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.story.generation.ConversationSummaryService;
import com.storyweaver.story.generation.StructuredSummaryApplyResult;
import com.storyweaver.story.generation.StructuredSummaryApplyService;
import com.storyweaver.story.generation.SummarySuggestionPack;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class StructuredSummaryController {

    private final ConversationSummaryService conversationSummaryService;
    private final StructuredSummaryApplyService structuredSummaryApplyService;

    public StructuredSummaryController(
            ConversationSummaryService conversationSummaryService,
            StructuredSummaryApplyService structuredSummaryApplyService) {
        this.conversationSummaryService = conversationSummaryService;
        this.structuredSummaryApplyService = structuredSummaryApplyService;
    }

    @PostMapping("/api/projects/{projectId}/story-brief/suggest")
    public ResponseEntity<Map<String, Object>> suggestProjectBrief(
            @PathVariable Long projectId,
            @RequestBody SummarySuggestionRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        SummarySuggestionPack pack = conversationSummaryService.suggestProjectBrief(
                userId,
                projectId,
                composeInput(requestDTO)
        );
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "建议摘要已生成",
                "data", pack
        ));
    }

    @PostMapping("/api/projects/{projectId}/characters/suggest")
    public ResponseEntity<Map<String, Object>> suggestCharacterCard(
            @PathVariable Long projectId,
            @RequestBody SummarySuggestionRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        SummarySuggestionPack pack = conversationSummaryService.suggestCharacterCard(
                userId,
                projectId,
                requestDTO == null ? null : requestDTO.getCharacterId(),
                composeInput(requestDTO)
        );
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "建议摘要已生成",
                "data", pack
        ));
    }

    @PostMapping("/api/projects/{projectId}/chapters/{chapterId}/brief/suggest")
    public ResponseEntity<Map<String, Object>> suggestChapterBrief(
            @PathVariable Long projectId,
            @PathVariable Long chapterId,
            @RequestBody SummarySuggestionRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        SummarySuggestionPack pack = conversationSummaryService.suggestChapterBrief(
                userId,
                projectId,
                chapterId,
                composeInput(requestDTO)
        );
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "建议摘要已生成",
                "data", pack
        ));
    }

    @PostMapping("/api/structured-summaries/apply")
    public ResponseEntity<Map<String, Object>> applyStructuredSummary(
            @RequestBody StructuredSummaryApplyRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        StructuredSummaryApplyResult result = structuredSummaryApplyService.apply(userId, requestDTO);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "结构化摘要已应用",
                "data", result
        ));
    }

    private String composeInput(SummarySuggestionRequestDTO requestDTO) {
        if (requestDTO == null) {
            return null;
        }
        String inputText = trimToNull(requestDTO.getInputText());
        String currentContext = trimToNull(requestDTO.getCurrentContext());
        if (!StringUtils.hasText(currentContext)) {
            return inputText;
        }
        if (!StringUtils.hasText(inputText)) {
            return currentContext;
        }
        return currentContext + "\n\n[本次补充]\n" + inputText;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
