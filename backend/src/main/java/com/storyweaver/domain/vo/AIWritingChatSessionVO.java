package com.storyweaver.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class AIWritingChatSessionVO {

    private Long sessionId;

    private Long projectId;

    private Long chapterId;

    private Integer activeSegmentNo;

    private Integer activeWindowChars;

    private Integer maxWindowChars;

    private String compressedSummary;

    private List<AIWritingChatMessageVO> messages;
}
