package com.storyweaver.ai.director.application;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class DirectorModuleRegistry {

    private final Map<String, ModuleDefinition> definitions;

    public DirectorModuleRegistry() {
        this.definitions = new LinkedHashMap<>();
        register(new ModuleDefinition(
                "chapter_snapshot",
                "getChapterSnapshot",
                1.0,
                1,
                List.of("title", "orderNum", "wordCount", "requiredCharacterNames", "currentContentSummary"),
                Set.of("opening", "setup", "advancement", "turning", "convergence", "polish"),
                true
        ));
        register(new ModuleDefinition(
                "outline",
                "getOutlineContext",
                0.92,
                1,
                List.of("title", "summary", "stageGoal", "keyConflict", "turningPoints", "expectedEnding"),
                Set.of("opening", "setup", "advancement", "turning", "convergence"),
                false
        ));
        register(new ModuleDefinition(
                "plot",
                "getPlotCandidates",
                0.88,
                3,
                List.of("title", "description", "conflicts", "resolutions"),
                Set.of("advancement", "turning", "convergence"),
                false
        ));
        register(new ModuleDefinition(
                "causality",
                "getCausalityCandidates",
                0.82,
                3,
                List.of("name", "relationship", "description", "conditions", "strength"),
                Set.of("advancement", "turning", "convergence"),
                false
        ));
        register(new ModuleDefinition(
                "world_setting",
                "getWorldSettingFacts",
                0.72,
                4,
                List.of("name", "category", "description"),
                Set.of("opening", "setup", "advancement"),
                false
        ));
        register(new ModuleDefinition(
                "required_characters",
                "getRequiredCharacterState",
                0.86,
                4,
                List.of("name", "description", "attributes", "projectRole"),
                Set.of("opening", "setup", "advancement", "turning", "convergence"),
                false
        ));
        register(new ModuleDefinition(
                "character_inventory",
                "getRequiredCharacterInventory",
                0.58,
                4,
                List.of("characterName", "items"),
                Set.of("opening", "setup", "advancement", "turning"),
                false
        ));
        register(new ModuleDefinition(
                "knowledge",
                "searchKnowledgeDocuments",
                0.46,
                3,
                List.of("title", "summary", "sourceType"),
                Set.of("opening", "setup", "advancement", "turning", "convergence"),
                false
        ));
        register(new ModuleDefinition(
                "chat_background",
                "getChatBackgroundSummary",
                0.84,
                1,
                List.of("worldFacts", "characterConstraints", "plotGuidance", "writingPreferences", "hardConstraints"),
                Set.of("opening", "setup", "advancement", "turning", "convergence", "polish"),
                false
        ));
    }

    public ModuleDefinition require(String moduleName) {
        ModuleDefinition definition = definitions.get(moduleName);
        if (definition == null) {
            throw new IllegalArgumentException("未注册的总导模块: " + moduleName);
        }
        return definition;
    }

    public List<ModuleDefinition> listAll() {
        return List.copyOf(definitions.values());
    }

    private void register(ModuleDefinition definition) {
        definitions.put(definition.moduleName(), definition);
    }

    public record ModuleDefinition(
            String moduleName,
            String toolName,
            double defaultWeight,
            int maxItems,
            List<String> availableFields,
            Set<String> supportedStages,
            boolean required) {
    }
}
