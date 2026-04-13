package com.storyweaver.domain.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class StructuredSummaryApplyRequestDTO {

    private String scope;

    private Long projectId;

    private Long targetId;

    private Map<String, Object> structuredFields = new LinkedHashMap<>();

    private String canonSummaryText;
}
