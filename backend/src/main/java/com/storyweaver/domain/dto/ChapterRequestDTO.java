package com.storyweaver.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChapterRequestDTO {
    private String title;

    private String content;

    private Integer orderNum;

    private Integer status;

    private List<Long> requiredCharacterIds;
}
