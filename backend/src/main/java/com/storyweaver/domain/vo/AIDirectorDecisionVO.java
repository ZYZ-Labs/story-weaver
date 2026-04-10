package com.storyweaver.domain.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AIDirectorDecisionVO {

    private Long id;

    private Long projectId;

    private Long chapterId;

    private String sourceType;

    private String entryPoint;

    private String stage;

    private String writingMode;

    private Integer targetWordCount;

    private String decisionSummary;

    private JsonNode decisionPack;

    private JsonNode toolTrace;

    private Long selectedProviderId;

    private String selectedModel;

    private String status;

    private String mode;

    private String errorMessage;

    private String failureReason;

    private String selectedAnchorSummary;

    private Integer toolCallCount;

    private LocalDateTime createTime;
}
