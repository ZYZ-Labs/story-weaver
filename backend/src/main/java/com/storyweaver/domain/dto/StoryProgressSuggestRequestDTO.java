package com.storyweaver.domain.dto;

import lombok.Data;

@Data
public class StoryProgressSuggestRequestDTO {

    private String contextText;

    private Long targetOutlineId;

    private Long targetChapterId;
}
