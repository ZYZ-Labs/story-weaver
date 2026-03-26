package com.storyweaver.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class OutlineRequestDTO {

    private Long chapterId;

    private String title;

    private String summary;

    private String content;

    private String stageGoal;

    private String keyConflict;

    private String turningPoints;

    private String expectedEnding;

    private List<Long> focusCharacterIds;

    private List<Long> relatedPlotIds;

    private List<Long> relatedCausalityIds;

    private Integer status;

    private Integer orderNum;
}
