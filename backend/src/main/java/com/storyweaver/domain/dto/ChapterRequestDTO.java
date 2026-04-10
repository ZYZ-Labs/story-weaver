package com.storyweaver.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChapterRequestDTO {
    private String title;

    private String summary;

    private String content;

    private Integer orderNum;

    private Integer status;

    private String chapterStatus;

    private Long outlineId;

    private List<Long> storyBeatIds;

    private Long prevChapterId;

    private Long nextChapterId;

    private Long mainPovCharacterId;

    private List<Long> requiredCharacterIds;
}
