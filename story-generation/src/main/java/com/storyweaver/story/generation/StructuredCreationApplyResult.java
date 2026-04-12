package com.storyweaver.story.generation;

import lombok.Data;

@Data
public class StructuredCreationApplyResult {

    private String entityType;

    private Long createdId;

    private Object created;
}
