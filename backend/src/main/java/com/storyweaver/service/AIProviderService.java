package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.domain.vo.ProviderDiscoveryVO;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface AIProviderService extends IService<AIProvider> {
    List<AIProvider> listProviders();
    AIProvider createProvider(AIProvider provider);
    AIProvider updateProvider(Long id, AIProvider provider);
    boolean deleteProvider(Long id);
    boolean testProvider(Long id);
    ProviderDiscoveryVO discoverModels(AIProvider provider);
    String generateText(AIProvider provider, String modelName, String systemPrompt, String userPrompt, Double temperature, Integer maxTokens);
    void streamText(
            AIProvider provider,
            String modelName,
            String systemPrompt,
            String userPrompt,
            Double temperature,
            Integer maxTokens,
            Consumer<String> onChunk);

    ToolExecutionResult generateTextWithTools(
            AIProvider provider,
            String modelName,
            String systemPrompt,
            String userPrompt,
            List<ToolDefinition> tools,
            Integer maxTokens,
            int maxToolCalls,
            Function<ToolCallRequest, String> toolExecutor);

    record ToolDefinition(String name, String description, String inputSchemaJson) {
    }

    record ToolCallRequest(String id, String name, String argumentsJson) {
    }

    record ToolCallTrace(String id, String name, String argumentsJson, String resultJson) {
    }

    record ToolExecutionResult(String finalText, List<ToolCallTrace> toolCalls) {
    }
}
