package com.storyweaver.controller;

import com.storyweaver.domain.dto.SummaryWorkflowApplyRequestDTO;
import com.storyweaver.domain.dto.SummaryWorkflowChatMessageDTO;
import com.storyweaver.domain.dto.SummaryWorkflowChatTurnRequestDTO;
import com.storyweaver.domain.dto.SummaryWorkflowPreviewRequestDTO;
import com.storyweaver.domain.dto.SummaryWorkflowProposalRequestDTO;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.storyunit.summary.workflow.StorySummaryApplyService;
import com.storyweaver.storyunit.summary.workflow.StorySummaryConversationService;
import com.storyweaver.storyunit.summary.workflow.StorySummaryPreviewService;
import com.storyweaver.storyunit.summary.workflow.StorySummaryProposalService;
import com.storyweaver.storyunit.summary.workflow.StructuredPatchProposal;
import com.storyweaver.storyunit.summary.workflow.SummaryApplyCommand;
import com.storyweaver.storyunit.summary.workflow.SummaryApplyResult;
import com.storyweaver.storyunit.summary.workflow.SummaryChangePreview;
import com.storyweaver.storyunit.summary.workflow.SummaryInputDraft;
import com.storyweaver.storyunit.summary.workflow.SummaryInputIntent;
import com.storyweaver.storyunit.summary.workflow.SummaryWorkflowChatMessage;
import com.storyweaver.storyunit.summary.workflow.SummaryWorkflowChatTurnRequest;
import com.storyweaver.storyunit.summary.workflow.SummaryWorkflowChatTurnResult;
import com.storyweaver.storyunit.workflow.exception.SummaryProposalNotFoundException;
import com.storyweaver.storyunit.workflow.support.SummaryProposalStore;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
public class SummaryWorkflowController {

    private final StorySummaryProposalService storySummaryProposalService;
    private final StorySummaryConversationService storySummaryConversationService;
    private final StorySummaryPreviewService storySummaryPreviewService;
    private final StorySummaryApplyService storySummaryApplyService;
    private final SummaryProposalStore summaryProposalStore;

    public SummaryWorkflowController(
            StorySummaryProposalService storySummaryProposalService,
            StorySummaryConversationService storySummaryConversationService,
            StorySummaryPreviewService storySummaryPreviewService,
            StorySummaryApplyService storySummaryApplyService,
            SummaryProposalStore summaryProposalStore) {
        this.storySummaryProposalService = storySummaryProposalService;
        this.storySummaryConversationService = storySummaryConversationService;
        this.storySummaryPreviewService = storySummaryPreviewService;
        this.storySummaryApplyService = storySummaryApplyService;
        this.summaryProposalStore = summaryProposalStore;
    }

    @PostMapping("/api/summary-workflow/proposals")
    public ResponseEntity<Map<String, Object>> createProposal(
            @RequestBody SummaryWorkflowProposalRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        validateProposalRequest(requestDTO);
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        SummaryInputDraft inputDraft = new SummaryInputDraft(
                requestDTO.getTargetType(),
                requestDTO.getTargetSourceId(),
                requestDTO.getProjectId(),
                requestDTO.getSummaryText(),
                requestDTO.getIntent(),
                requestDTO.getOperatorMode()
        );
        StructuredPatchProposal proposal = storySummaryProposalService.propose(userId, inputDraft);
        SummaryChangePreview preview = storySummaryPreviewService.preview(userId, proposal);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "摘要提案已生成",
                "data", Map.of(
                        "proposal", proposal,
                        "preview", preview
                )
        ));
    }

    @PostMapping("/api/summary-workflow/chat-turns")
    public ResponseEntity<Map<String, Object>> replyChatTurn(
            @RequestBody SummaryWorkflowChatTurnRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        if (requestDTO == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (requestDTO.getTargetType() == null) {
            throw new IllegalArgumentException("targetType 不能为空");
        }
        if (requestDTO.getProjectId() == null) {
            throw new IllegalArgumentException("projectId 不能为空");
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        SummaryWorkflowChatTurnResult result = storySummaryConversationService.reply(
                userId,
                new SummaryWorkflowChatTurnRequest(
                        requestDTO.getTargetType(),
                        requestDTO.getTargetSourceId(),
                        requestDTO.getProjectId(),
                        requestDTO.getTitle(),
                        requestDTO.getExistingSummary(),
                        requestDTO.getCurrentDraftSummary(),
                        requestDTO.getIntent(),
                        requestDTO.getOperatorMode(),
                        requestDTO.getMessages() == null ? List.of() : requestDTO.getMessages().stream()
                                .map(this::toChatMessage)
                                .toList(),
                        requestDTO.getSelectedProviderId(),
                        requestDTO.getSelectedModel()
                )
        );
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "摘要对话已更新",
                "data", result
        ));
    }

    @PostMapping("/api/summary-workflow/previews")
    public ResponseEntity<Map<String, Object>> previewProposal(
            @RequestBody SummaryWorkflowPreviewRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        StructuredPatchProposal proposal = resolveProposal(requestDTO);
        SummaryChangePreview preview = storySummaryPreviewService.preview(userId, proposal);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "摘要预览已生成",
                "data", preview
        ));
    }

    @PostMapping("/api/summary-workflow/apply")
    public ResponseEntity<Map<String, Object>> applyProposal(
            @RequestBody SummaryWorkflowApplyRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        if (requestDTO == null || !StringUtils.hasText(requestDTO.getProposalId())) {
            throw new IllegalArgumentException("proposalId 不能为空");
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        StructuredPatchProposal proposal = summaryProposalStore.find(requestDTO.getProposalId())
                .orElseThrow(() -> new IllegalArgumentException("proposal 不存在或已过期"));
        SummaryApplyCommand command = new SummaryApplyCommand(
                requestDTO.getProposalId(),
                requestDTO.getTargetRef() == null ? proposal.targetRef() : requestDTO.getTargetRef(),
                !Boolean.FALSE.equals(requestDTO.getConfirmed()),
                userId
        );
        SummaryApplyResult result = storySummaryApplyService.apply(userId, command);
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "摘要变更已写回",
                "data", result
        ));
    }

    private void validateProposalRequest(SummaryWorkflowProposalRequestDTO requestDTO) {
        if (requestDTO == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (requestDTO.getTargetType() == null) {
            throw new IllegalArgumentException("targetType 不能为空");
        }
        if (requestDTO.getIntent() != SummaryInputIntent.CREATE && requestDTO.getTargetSourceId() == null) {
            throw new IllegalArgumentException("targetSourceId 不能为空");
        }
        if (requestDTO.getProjectId() == null) {
            throw new IllegalArgumentException("projectId 不能为空");
        }
        if (!StringUtils.hasText(requestDTO.getSummaryText())) {
            throw new IllegalArgumentException("summaryText 不能为空");
        }
    }

    private StructuredPatchProposal resolveProposal(SummaryWorkflowPreviewRequestDTO requestDTO) {
        if (requestDTO == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (requestDTO.getProposal() != null) {
            return requestDTO.getProposal();
        }
        if (!StringUtils.hasText(requestDTO.getProposalId())) {
            throw new IllegalArgumentException("proposal 或 proposalId 至少提供一个");
        }
        return summaryProposalStore.find(requestDTO.getProposalId())
                .orElseThrow(() -> new SummaryProposalNotFoundException("proposal 不存在、已过期或已被清理"));
    }

    private SummaryWorkflowChatMessage toChatMessage(SummaryWorkflowChatMessageDTO dto) {
        if (dto == null) {
            return new SummaryWorkflowChatMessage("user", "");
        }
        return new SummaryWorkflowChatMessage(dto.getRole(), dto.getContent());
    }
}
