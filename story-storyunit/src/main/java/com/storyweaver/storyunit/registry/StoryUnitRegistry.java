package com.storyweaver.storyunit.registry;

import com.storyweaver.storyunit.assembler.StoryUnitAssembler;
import com.storyweaver.storyunit.model.StoryUnitType;

import java.util.Collection;
import java.util.Optional;

public interface StoryUnitRegistry {

    Collection<StoryUnitAssembler<?>> assemblers();

    Optional<StoryUnitAssembler<?>> findByUnitType(StoryUnitType unitType);

    Optional<StoryUnitAssembler<?>> findBySourceType(Class<?> sourceType);

    default boolean supports(StoryUnitType unitType) {
        return findByUnitType(unitType).isPresent();
    }
}
