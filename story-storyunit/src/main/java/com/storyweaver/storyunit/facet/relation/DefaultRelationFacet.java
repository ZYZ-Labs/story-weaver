package com.storyweaver.storyunit.facet.relation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DefaultRelationFacet(
        Map<String, List<String>> relationRefs) implements RelationFacet {

    public DefaultRelationFacet {
        if (relationRefs == null) {
            relationRefs = Map.of();
        } else {
            Map<String, List<String>> copied = new LinkedHashMap<>();
            relationRefs.forEach((key, value) ->
                    copied.put(key, value == null ? List.of() : List.copyOf(value)));
            relationRefs = Map.copyOf(copied);
        }
    }
}
