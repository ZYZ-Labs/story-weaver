package com.storyweaver.storyunit.registry;

import com.storyweaver.storyunit.assembler.StoryUnitAssembler;
import com.storyweaver.storyunit.model.StoryUnitType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DefaultStoryUnitRegistry implements StoryUnitRegistry {

    private final List<StoryUnitAssembler<?>> assemblers;

    private final Map<StoryUnitType, StoryUnitAssembler<?>> assemblersByUnitType;

    private final Map<Class<?>, StoryUnitAssembler<?>> assemblersBySourceType;

    public DefaultStoryUnitRegistry(Collection<? extends StoryUnitAssembler<?>> assemblers) {
        Objects.requireNonNull(assemblers, "assemblers must not be null");

        Map<StoryUnitType, StoryUnitAssembler<?>> unitTypeMap = new LinkedHashMap<>();
        Map<Class<?>, StoryUnitAssembler<?>> sourceTypeMap = new LinkedHashMap<>();
        for (StoryUnitAssembler<?> assembler : assemblers) {
            StoryUnitAssembler<?> current = Objects.requireNonNull(assembler, "assembler must not be null");
            registerUnique(unitTypeMap, current.unitType(), current, "unitType");
            registerUnique(sourceTypeMap, current.sourceType(), current, "sourceType");
        }

        this.assemblers = List.copyOf(assemblers);
        this.assemblersByUnitType = Map.copyOf(unitTypeMap);
        this.assemblersBySourceType = Map.copyOf(sourceTypeMap);
    }

    @Override
    public Collection<StoryUnitAssembler<?>> assemblers() {
        return assemblers;
    }

    @Override
    public Optional<StoryUnitAssembler<?>> findByUnitType(StoryUnitType unitType) {
        return Optional.ofNullable(assemblersByUnitType.get(unitType));
    }

    @Override
    public Optional<StoryUnitAssembler<?>> findBySourceType(Class<?> sourceType) {
        return Optional.ofNullable(assemblersBySourceType.get(sourceType));
    }

    private <K> void registerUnique(Map<K, StoryUnitAssembler<?>> registry, K key, StoryUnitAssembler<?> assembler, String keyType) {
        StoryUnitAssembler<?> previous = registry.putIfAbsent(Objects.requireNonNull(key, keyType + " must not be null"), assembler);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate assembler " + keyType + ": " + key);
        }
    }
}
