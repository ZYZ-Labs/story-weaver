package com.storyweaver.storyunit.mapping;

import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryUnitType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record StoryUnitMappingMatrix(
        StoryUnitType unitType,
        List<String> sourceKeys,
        List<StoryFieldMapping> fieldMappings) {

    public StoryUnitMappingMatrix {
        unitType = Objects.requireNonNull(unitType, "unitType must not be null");
        sourceKeys = sourceKeys == null ? List.of() : List.copyOf(sourceKeys);
        fieldMappings = fieldMappings == null ? List.of() : List.copyOf(fieldMappings);
    }

    public List<StoryFieldMapping> mappingsFor(FacetType facetType) {
        Objects.requireNonNull(facetType, "facetType must not be null");
        return fieldMappings.stream()
                .filter(mapping -> mapping.facetType() == facetType)
                .toList();
    }

    public Set<FacetType> coveredFacetTypes() {
        Set<FacetType> covered = new LinkedHashSet<>();
        for (StoryFieldMapping mapping : fieldMappings) {
            covered.add(mapping.facetType());
        }
        return Set.copyOf(covered);
    }
}
