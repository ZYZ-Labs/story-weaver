package com.storyweaver.storyunit.projection;

import com.storyweaver.domain.entity.ChapterCharacterLink;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.ProjectCharacterLink;

import java.util.List;
import java.util.Objects;

public record CharacterProjectionSource(
        Character character,
        List<ProjectCharacterLink> projectLinks,
        List<ChapterCharacterLink> chapterLinks) {

    public CharacterProjectionSource {
        character = Objects.requireNonNull(character, "character must not be null");
        projectLinks = projectLinks == null ? List.of() : List.copyOf(projectLinks);
        chapterLinks = chapterLinks == null ? List.of() : List.copyOf(chapterLinks);
    }
}
