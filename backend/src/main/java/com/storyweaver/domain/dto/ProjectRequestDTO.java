package com.storyweaver.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProjectRequestDTO {

    private String name;

    private String description;

    private String genre;

    private String tags;

    private List<Long> worldSettingIds;

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

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public List<Long> getWorldSettingIds() {
        return worldSettingIds;
    }

    public void setWorldSettingIds(List<Long> worldSettingIds) {
        this.worldSettingIds = worldSettingIds;
    }
}
