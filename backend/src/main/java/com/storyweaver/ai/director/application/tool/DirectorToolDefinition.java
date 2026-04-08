package com.storyweaver.ai.director.application.tool;

import com.storyweaver.service.AIProviderService;

public record DirectorToolDefinition(
        String name,
        String description,
        String inputSchemaJson) {

    public AIProviderService.ToolDefinition toProviderDefinition() {
        return new AIProviderService.ToolDefinition(name, description, inputSchemaJson);
    }
}
