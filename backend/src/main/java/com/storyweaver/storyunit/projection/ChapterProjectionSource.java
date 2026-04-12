package com.storyweaver.storyunit.projection;

import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.Outline;
import com.storyweaver.domain.entity.Plot;

import java.util.List;
import java.util.Objects;

public record ChapterProjectionSource(
        Chapter chapter,
        Outline outline,
        Character mainPovCharacter,
        List<Character> requiredCharacters,
        List<Plot> plots) {

    public ChapterProjectionSource {
        chapter = Objects.requireNonNull(chapter, "chapter must not be null");
        requiredCharacters = requiredCharacters == null ? List.of() : List.copyOf(requiredCharacters);
        plots = plots == null ? List.of() : List.copyOf(plots);
    }
}
