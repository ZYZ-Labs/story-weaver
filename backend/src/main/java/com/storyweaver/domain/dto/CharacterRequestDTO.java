package com.storyweaver.domain.dto;

import lombok.Data;

@Data
public class CharacterRequestDTO {
    private Long existingCharacterId;

    private String name;

    private String description;

    private String identity;

    private String coreGoal;

    private String growthArc;

    private Long firstAppearanceChapterId;

    private String activeStage;

    private Boolean isRetired;

    private String attributes;

    private String advancedProfileJson;

    private String projectRole;

    private String roleType;
}
