package com.storyweaver.storyunit.facet.reveal;

import java.util.List;

public record ReaderRevealState(
        Long projectId,
        Long chapterId,
        List<String> systemKnown,
        List<String> authorKnown,
        List<String> readerKnown,
        List<String> unrevealed,
        String summary) implements RevealFacet {

    public ReaderRevealState {
        systemKnown = systemKnown == null ? List.of() : List.copyOf(systemKnown);
        authorKnown = authorKnown == null ? List.of() : List.copyOf(authorKnown);
        readerKnown = readerKnown == null ? List.of() : List.copyOf(readerKnown);
        unrevealed = unrevealed == null ? List.of() : List.copyOf(unrevealed);
        summary = summary == null ? "" : summary.trim();
    }
}
