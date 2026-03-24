package com.storyweaver.domain.dto;

import lombok.Data;

@Data
public class CharacterAttributeSuggestionRequestDTO {

    private String name;

    private String description;

    private String extraRequirements;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExtraRequirements() {
        return extraRequirements;
    }

    public void setExtraRequirements(String extraRequirements) {
        this.extraRequirements = extraRequirements;
    }
}
