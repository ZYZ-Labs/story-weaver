package com.storyweaver.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AIWritingChatMessageVO {

    private Long id;

    private String role;

    private String content;

    private Integer segmentNo;

    private Boolean pinnedToBackground;

    private Boolean compressed;

    private LocalDateTime createTime;
}
