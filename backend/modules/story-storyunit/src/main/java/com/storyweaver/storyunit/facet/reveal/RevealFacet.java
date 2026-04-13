package com.storyweaver.storyunit.facet.reveal;

import com.storyweaver.storyunit.facet.StoryFacet;
import com.storyweaver.storyunit.model.FacetType;

import java.util.List;

public interface RevealFacet extends StoryFacet {

    @Override
    default FacetType facetType() {
        return FacetType.REVEAL;
    }

    List<String> systemKnown();

    List<String> authorKnown();

    List<String> readerKnown();

    List<String> unrevealed();
}
