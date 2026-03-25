package com.storyweaver.domain.dto;

import lombok.Data;

@Data
public class CharacterRequestDTO {
    private Long existingCharacterId;

    private String name;

    private String description;

    private String attributes;

    private String projectRole;
}
