package com.storyweaver.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChapterAnchorUpdateRequestDTO {

    private Long outlineId;

    private Long mainPovCharacterId;

    private List<Long> requiredCharacterIds;

    private List<Long> storyBeatIds;
}
