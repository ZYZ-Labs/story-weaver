package com.storyweaver.item.web.request;

import lombok.Data;

@Data
public class ItemGenerationRequest {

    private String category;

    private Integer count;

    private String prompt;

    private String constraints;

    private Long selectedProviderId;

    private String selectedModel;
}
