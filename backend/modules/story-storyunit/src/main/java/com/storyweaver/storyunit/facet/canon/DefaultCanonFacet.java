package com.storyweaver.storyunit.facet.canon;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DefaultCanonFacet(
        Map<String, Object> canonicalFields,
        List<String> canonicalTags) implements CanonFacet {

    public DefaultCanonFacet {
        canonicalFields = canonicalFields == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(canonicalFields));
        canonicalTags = canonicalTags == null ? List.of() : List.copyOf(canonicalTags);
    }
}
