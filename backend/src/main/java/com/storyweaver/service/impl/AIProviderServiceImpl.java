package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.domain.vo.ProviderDiscoveryVO;
import com.storyweaver.repository.AIProviderMapper;
import com.storyweaver.service.AIProviderService;
import com.storyweaver.service.SystemConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
public class AIProviderServiceImpl extends ServiceImpl<AIProviderMapper, AIProvider> implements AIProviderService {

    private static final int DEFAULT_PROVIDER_REQUEST_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_GLOBAL_AI_REQUEST_TIMEOUT_SECONDS = 3600;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final SystemConfigService systemConfigService;

    public AIProviderServiceImpl(ObjectMapper objectMapper, SystemConfigService systemConfigService) {
        this.objectMapper = objectMapper;
        this.systemConfigService = systemConfigService;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public List<AIProvider> listProviders() {
        QueryWrapper<AIProvider> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0).orderByDesc("is_default").orderByDesc("update_time");
        return list(queryWrapper);
    }

    @Override
    @Transactional
    public AIProvider createProvider(AIProvider provider) {
        if (provider.getEnabled() == null) {
            provider.setEnabled(1);
        }
        if (provider.getIsDefault() == null) {
            provider.setIsDefault(0);
        }
        save(provider);
        if (Integer.valueOf(1).equals(provider.getIsDefault())) {
            clearOtherDefaults(provider.getId());
        }
        return getById(provider.getId());
    }

    @Override
    @Transactional
    public AIProvider updateProvider(Long id, AIProvider provider) {
        AIProvider existing = getById(id);
        if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) {
            return null;
        }

        existing.setName(provider.getName());
        existing.setProviderType(provider.getProviderType());
        existing.setBaseUrl(provider.getBaseUrl());
        if ("ollama".equalsIgnoreCase(provider.getProviderType())) {
            existing.setApiKey("");
        } else if (StringUtils.hasText(provider.getApiKey())) {
            existing.setApiKey(provider.getApiKey().trim());
        }
        existing.setModelName(provider.getModelName());
        existing.setEmbeddingModel(provider.getEmbeddingModel());
        existing.setTemperature(provider.getTemperature());
        existing.setTopP(provider.getTopP());
        existing.setMaxTokens(provider.getMaxTokens());
        existing.setTimeoutSeconds(provider.getTimeoutSeconds());
        existing.setEnabled(provider.getEnabled());
        existing.setIsDefault(provider.getIsDefault());
        existing.setRemark(provider.getRemark());

        updateById(existing);
        if (Integer.valueOf(1).equals(existing.getIsDefault())) {
            clearOtherDefaults(existing.getId());
        }
        return getById(existing.getId());
    }

    @Override
    @Transactional
    public boolean deleteProvider(Long id) {
        return removeById(id);
    }

    @Override
    public boolean testProvider(Long id) {
        AIProvider provider = getById(id);
        if (provider == null || Integer.valueOf(1).equals(provider.getDeleted())) {
            return false;
        }

        if ("ollama".equalsIgnoreCase(provider.getProviderType())) {
            return discoverModels(provider).success();
        }

        return StringUtils.hasText(provider.getBaseUrl()) && StringUtils.hasText(provider.getModelName());
    }

    @Override
    public ProviderDiscoveryVO discoverModels(AIProvider provider) {
        if (provider == null || !StringUtils.hasText(provider.getProviderType())) {
            return new ProviderDiscoveryVO(false, "请先选择模型服务类型。", List.of(), List.of());
        }
        if (!StringUtils.hasText(provider.getBaseUrl())) {
            return new ProviderDiscoveryVO(false, "请先填写服务地址。", List.of(), List.of());
        }

        try {
            if ("ollama".equalsIgnoreCase(provider.getProviderType())) {
                return discoverOllamaModels(provider);
            }
            if ("openai-compatible".equalsIgnoreCase(provider.getProviderType())) {
                return discoverCompatibleModels(provider);
            }
            return new ProviderDiscoveryVO(false, "当前服务类型暂不支持自动获取模型列表。", List.of(), List.of());
        } catch (IllegalArgumentException exception) {
            return new ProviderDiscoveryVO(false, "服务地址格式不正确，请检查后重试。", List.of(), List.of());
        } catch (IOException exception) {
            return new ProviderDiscoveryVO(false, "连接失败，无法读取模型服务返回结果。", List.of(), List.of());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new ProviderDiscoveryVO(false, "获取模型列表时被中断，请稍后重试。", List.of(), List.of());
        } catch (Exception exception) {
            return new ProviderDiscoveryVO(false, "连接失败：" + exception.getMessage(), List.of(), List.of());
        }
    }

    @Override
    public String generateText(
            AIProvider provider,
            String modelName,
            String systemPrompt,
            String userPrompt,
            Double temperature,
            Integer maxTokens) {
        String resolvedModelName = validateGenerationRequest(provider, modelName);

        try {
            if ("ollama".equalsIgnoreCase(provider.getProviderType())) {
                if (preferCompatibleOllamaEndpoint(provider)) {
                    return requestCompatibleChat(provider, resolvedModelName, systemPrompt, userPrompt, temperature, maxTokens);
                }
                return requestOllamaChat(provider, resolvedModelName, systemPrompt, userPrompt, temperature, maxTokens);
            }
            return requestCompatibleChat(provider, resolvedModelName, systemPrompt, userPrompt, temperature, maxTokens);
        } catch (IOException exception) {
            throw translateTransportFailure(exception, provider);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("调用模型服务时被中断，请稍后重试");
        }
    }

    @Override
    public void streamText(
            AIProvider provider,
            String modelName,
            String systemPrompt,
            String userPrompt,
            Double temperature,
            Integer maxTokens,
            Consumer<String> onChunk) {
        String resolvedModelName = validateGenerationRequest(provider, modelName);

        try {
            if ("ollama".equalsIgnoreCase(provider.getProviderType())) {
                if (preferCompatibleOllamaEndpoint(provider)) {
                    requestCompatibleChatStream(provider, resolvedModelName, systemPrompt, userPrompt, temperature, maxTokens, onChunk);
                    return;
                }
                requestOllamaChatStream(provider, resolvedModelName, systemPrompt, userPrompt, temperature, maxTokens, onChunk);
                return;
            }
            requestCompatibleChatStream(provider, resolvedModelName, systemPrompt, userPrompt, temperature, maxTokens, onChunk);
        } catch (IOException exception) {
            throw translateTransportFailure(exception, provider);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("调用模型服务时被中断，请稍后重试");
        }
    }

    @Override
    public ToolExecutionResult generateTextWithTools(
            AIProvider provider,
            String modelName,
            String systemPrompt,
            String userPrompt,
            List<ToolDefinition> tools,
            Integer maxTokens,
            int maxToolCalls,
            Function<ToolCallRequest, String> toolExecutor) {
        String resolvedModelName = validateGenerationRequest(provider, modelName);
        if (tools == null || tools.isEmpty() || toolExecutor == null) {
            return new ToolExecutionResult(
                    generateText(provider, resolvedModelName, systemPrompt, userPrompt, null, maxTokens),
                    List.of()
            );
        }

        if ("ollama".equalsIgnoreCase(provider.getProviderType()) && !preferCompatibleOllamaEndpoint(provider)) {
            throw new IllegalStateException("当前 Provider 未启用兼容 /v1/chat/completions 接口，暂不支持 tool calling");
        }

        try {
            return requestCompatibleChatWithTools(
                    provider,
                    resolvedModelName,
                    systemPrompt,
                    userPrompt,
                    tools,
                    maxTokens,
                    maxToolCalls,
                    toolExecutor
            );
        } catch (IOException exception) {
            throw translateTransportFailure(exception, provider);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("调用模型服务时被中断，请稍后重试");
        }
    }

    private String validateGenerationRequest(AIProvider provider, String modelName) {
        if (provider == null || Integer.valueOf(1).equals(provider.getDeleted())) {
            throw new IllegalStateException("模型服务不存在或已被删除");
        }

        String resolvedModelName = StringUtils.hasText(modelName)
                ? modelName.trim()
                : StringUtils.trimWhitespace(provider.getModelName());
        if (!StringUtils.hasText(resolvedModelName)) {
            throw new IllegalStateException("当前模型服务尚未配置对话模型");
        }
        if (!StringUtils.hasText(provider.getBaseUrl())) {
            throw new IllegalStateException("当前模型服务尚未配置服务地址");
        }
        return resolvedModelName;
    }

    private String buildOllamaTagsUrl(String baseUrl) {
        String normalized = normalizeBaseUrl(baseUrl);
        if (normalized.endsWith("/v1")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        return normalized + "/api/tags";
    }

    private String buildOllamaChatUrl(String baseUrl) {
        String normalized = normalizeBaseUrl(baseUrl);
        if (normalized.endsWith("/v1")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        return normalized + "/api/chat";
    }

    private String buildCompatibleChatUrl(String baseUrl) {
        String normalized = normalizeBaseUrl(baseUrl);
        if (normalized.endsWith("/chat/completions")) {
            return normalized;
        }
        if (normalized.endsWith("/v1")) {
            return normalized + "/chat/completions";
        }
        return normalized + "/v1/chat/completions";
    }

    private String buildCompatibleModelsUrl(String baseUrl) {
        String normalized = normalizeBaseUrl(baseUrl);
        if (normalized.endsWith("/models")) {
            return normalized;
        }
        if (normalized.endsWith("/v1")) {
            return normalized + "/models";
        }
        return normalized + "/v1/models";
    }

    private String normalizeBaseUrl(String baseUrl) {
        return baseUrl.trim().replaceAll("/+$", "");
    }

    private int resolveTimeoutSeconds(AIProvider provider) {
        Integer timeout = provider.getTimeoutSeconds();
        return timeout != null && timeout > 0 ? timeout : 15;
    }

    private int resolveGenerationTimeoutSeconds(AIProvider provider) {
        return resolveRequestTimeoutSeconds(provider);
    }

    private int resolveStreamingTimeoutSeconds(AIProvider provider) {
        return resolveRequestTimeoutSeconds(provider);
    }

    int resolveRequestTimeoutSeconds(AIProvider provider) {
        int providerTimeoutSeconds = resolveProviderRequestTimeoutSeconds(provider);
        int configuredTimeoutSeconds = resolveConfiguredAiTimeoutSeconds();
        return configuredTimeoutSeconds > 0
                ? Math.min(providerTimeoutSeconds, configuredTimeoutSeconds)
                : providerTimeoutSeconds;
    }

    private int resolveProviderRequestTimeoutSeconds(AIProvider provider) {
        Integer timeout = provider == null ? null : provider.getTimeoutSeconds();
        return timeout != null && timeout > 0 ? timeout : DEFAULT_PROVIDER_REQUEST_TIMEOUT_SECONDS;
    }

    private int resolveConfiguredAiTimeoutSeconds() {
        String configuredValue = systemConfigService.getConfigValue("ai.request.timeout_seconds");
        if (!StringUtils.hasText(configuredValue)) {
            return DEFAULT_GLOBAL_AI_REQUEST_TIMEOUT_SECONDS;
        }
        try {
            int parsed = Integer.parseInt(configuredValue.trim());
            return parsed > 0 ? parsed : DEFAULT_GLOBAL_AI_REQUEST_TIMEOUT_SECONDS;
        } catch (NumberFormatException exception) {
            return DEFAULT_GLOBAL_AI_REQUEST_TIMEOUT_SECONDS;
        }
    }

    private IllegalStateException translateTransportFailure(IOException exception, AIProvider provider) {
        String providerLabel = provider != null && StringUtils.hasText(provider.getName())
                ? "模型服务「" + provider.getName().trim() + "」"
                : "模型服务";

        if (exception instanceof HttpTimeoutException || exception instanceof SocketTimeoutException) {
            return new IllegalStateException(
                    providerLabel + " 响应超时（" + resolveRequestTimeoutSeconds(provider) + " 秒），请检查模型服务状态或调整超时配置",
                    exception
            );
        }
        if (exception instanceof UnknownHostException
                || exception instanceof ConnectException
                || exception instanceof NoRouteToHostException
                || exception instanceof SocketException) {
            return new IllegalStateException(
                    providerLabel + " 连接失败，请检查服务地址、端口和网络连通性",
                    exception
            );
        }
        if (StringUtils.hasText(exception.getMessage())) {
            return new IllegalStateException(providerLabel + " 调用失败：" + exception.getMessage().trim(), exception);
        }
        return new IllegalStateException(providerLabel + " 调用失败，无法读取返回结果", exception);
    }

    private boolean preferCompatibleOllamaEndpoint(AIProvider provider) {
        if (provider == null || !StringUtils.hasText(provider.getBaseUrl())) {
            return false;
        }
        String normalized = normalizeBaseUrl(provider.getBaseUrl());
        return normalized.endsWith("/v1") || normalized.contains("/v1/");
    }

    private String requestOllamaChat(
            AIProvider provider,
            String modelName,
            String systemPrompt,
            String userPrompt,
            Double temperature,
            Integer maxTokens) throws IOException, InterruptedException {
        HttpRequest.Builder builder = createJsonPostBuilder(
                URI.create(buildOllamaChatUrl(provider.getBaseUrl())),
                objectMapper.writeValueAsString(buildOllamaChatRequestBody(provider, modelName, systemPrompt, userPrompt, temperature, maxTokens, false)),
                provider,
                resolveGenerationTimeoutSeconds(provider),
                "application/json"
        );

        HttpResponse<String> response = httpClient.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("模型服务返回异常状态码：" + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = extractOllamaContent(root);
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("模型服务没有返回可用文本");
        }
        return content.trim();
    }

    private void requestOllamaChatStream(
            AIProvider provider,
            String modelName,
            String systemPrompt,
            String userPrompt,
            Double temperature,
            Integer maxTokens,
            Consumer<String> onChunk) throws IOException, InterruptedException {
        HttpRequest.Builder builder = createJsonPostBuilder(
                URI.create(buildOllamaChatUrl(provider.getBaseUrl())),
                objectMapper.writeValueAsString(buildOllamaChatRequestBody(provider, modelName, systemPrompt, userPrompt, temperature, maxTokens, true)),
                provider,
                resolveStreamingTimeoutSeconds(provider),
                "application/x-ndjson, application/json"
        );

        HttpResponse<InputStream> response = httpClient.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofInputStream()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("模型服务返回异常状态码：" + response.statusCode() + " " + readErrorBody(response.body()));
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String payload = line.trim();
                if (!StringUtils.hasText(payload)) {
                    continue;
                }
                if (payload.startsWith("data:")) {
                    payload = payload.substring(5).trim();
                }
                if (!StringUtils.hasText(payload) || "[DONE]".equals(payload)) {
                    break;
                }

                JsonNode root = objectMapper.readTree(payload);
                validateStreamPayload(root);
                String delta = extractOllamaContent(root);
                if (hasRawText(delta)) {
                    onChunk.accept(delta);
                }
                if (root.path("done").asBoolean(false)) {
                    break;
                }
            }
        }
    }

    private String requestCompatibleChat(
            AIProvider provider,
            String modelName,
            String systemPrompt,
            String userPrompt,
            Double temperature,
            Integer maxTokens) throws IOException, InterruptedException {
        HttpRequest.Builder builder = createJsonPostBuilder(
                URI.create(buildCompatibleChatUrl(provider.getBaseUrl())),
                objectMapper.writeValueAsString(buildCompatibleRequestBody(provider, modelName, systemPrompt, userPrompt, temperature, maxTokens, false)),
                provider,
                resolveGenerationTimeoutSeconds(provider),
                "application/json"
        );

        HttpResponse<String> response = httpClient.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("模型服务返回异常状态码：" + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = extractCompatibleContent(root);
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("模型服务没有返回可用文本");
        }
        return content.trim();
    }

    private void requestCompatibleChatStream(
            AIProvider provider,
            String modelName,
            String systemPrompt,
            String userPrompt,
            Double temperature,
            Integer maxTokens,
            Consumer<String> onChunk) throws IOException, InterruptedException {
        HttpRequest.Builder builder = createJsonPostBuilder(
                URI.create(buildCompatibleChatUrl(provider.getBaseUrl())),
                objectMapper.writeValueAsString(buildCompatibleRequestBody(provider, modelName, systemPrompt, userPrompt, temperature, maxTokens, true)),
                provider,
                resolveStreamingTimeoutSeconds(provider),
                "text/event-stream, application/json"
        );

        HttpResponse<InputStream> response = httpClient.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofInputStream()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("模型服务返回异常状态码：" + response.statusCode() + " " + readErrorBody(response.body()));
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String payload = line.trim();
                if (!StringUtils.hasText(payload)
                        || payload.startsWith(":")
                        || payload.startsWith("event:")) {
                    continue;
                }

                if (payload.startsWith("data:")) {
                    payload = payload.substring(5).trim();
                }
                if (!StringUtils.hasText(payload)) {
                    continue;
                }
                if ("[DONE]".equals(payload)) {
                    break;
                }

                JsonNode root = objectMapper.readTree(payload);
                validateStreamPayload(root);
                String delta = extractCompatibleDelta(root);
                if (hasRawText(delta)) {
                    onChunk.accept(delta);
                }
                if (isCompatibleStreamFinished(root)) {
                    break;
                }
            }
        }
    }

    private ToolExecutionResult requestCompatibleChatWithTools(
            AIProvider provider,
            String modelName,
            String systemPrompt,
            String userPrompt,
            List<ToolDefinition> tools,
            Integer maxTokens,
            int maxToolCalls,
            Function<ToolCallRequest, String> toolExecutor) throws IOException, InterruptedException {
        ArrayNode messages = buildMessages(systemPrompt, userPrompt);
        List<ToolCallTrace> traces = new ArrayList<>();
        int safeMaxToolCalls = Math.max(0, maxToolCalls);
        int maxRounds = Math.max(2, safeMaxToolCalls + 2);

        for (int round = 0; round < maxRounds; round++) {
            HttpRequest.Builder builder = createJsonPostBuilder(
                    URI.create(buildCompatibleChatUrl(provider.getBaseUrl())),
                    objectMapper.writeValueAsString(buildCompatibleToolRequestBody(provider, modelName, messages, tools, maxTokens)),
                    provider,
                    resolveGenerationTimeoutSeconds(provider),
                    "application/json"
            );

            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("模型服务返回异常状态码：" + response.statusCode() + " " + compactError(response.body()));
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode firstChoice = getFirstChoice(root);
            if (firstChoice == null) {
                throw new IllegalStateException("模型服务没有返回可用结果");
            }

            JsonNode assistantMessage = firstChoice.path("message");
            List<ToolCallRequest> toolRequests = extractToolCallRequests(assistantMessage);
            String content = extractCompatibleContent(root).trim();

            if (toolRequests.isEmpty()) {
                if (!StringUtils.hasText(content)) {
                    throw new IllegalStateException("模型服务没有返回可用文本");
                }
                return new ToolExecutionResult(content, List.copyOf(traces));
            }

            messages.add(buildAssistantToolCallMessage(assistantMessage, content));
            for (ToolCallRequest toolRequest : toolRequests) {
                String resultJson;
                if (traces.size() >= safeMaxToolCalls) {
                    resultJson = writeToolErrorJson("max_tool_calls_exceeded", "工具调用次数已达到上限");
                } else {
                    resultJson = executeToolSafely(toolExecutor, toolRequest);
                }
                traces.add(new ToolCallTrace(
                        toolRequest.id(),
                        toolRequest.name(),
                        toolRequest.argumentsJson(),
                        resultJson
                ));
                messages.add(buildToolResultMessage(toolRequest.id(), resultJson));
            }
        }

        throw new IllegalStateException("模型在工具调用后仍未返回最终决策结果");
    }

    private HttpRequest.Builder createJsonPostBuilder(
            URI uri,
            String payload,
            AIProvider provider,
            int timeoutSeconds,
            String accept) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Accept", accept)
                .header("Content-Type", "application/json; charset=UTF-8");

        if (StringUtils.hasText(provider.getApiKey())) {
            builder.header("Authorization", "Bearer " + provider.getApiKey().trim());
        }
        return builder;
    }

    private HttpRequest.Builder createJsonGetBuilder(
            URI uri,
            AIProvider provider,
            int timeoutSeconds,
            String accept) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Accept", accept);

        if (StringUtils.hasText(provider.getApiKey())) {
            builder.header("Authorization", "Bearer " + provider.getApiKey().trim());
        }
        return builder;
    }

    private ProviderDiscoveryVO discoverOllamaModels(AIProvider provider) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(
                createJsonGetBuilder(
                        URI.create(buildOllamaTagsUrl(provider.getBaseUrl())),
                        provider,
                        resolveTimeoutSeconds(provider),
                        "application/json"
                ).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return new ProviderDiscoveryVO(
                    false,
                    "连接成功，但获取模型列表失败，状态码：" + response.statusCode(),
                    List.of(),
                    List.of()
            );
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode modelsNode = root.path("models");
        if (!modelsNode.isArray()) {
            return new ProviderDiscoveryVO(false, "Ollama 返回内容不符合预期。", List.of(), List.of());
        }

        Set<String> names = extractModelNames(modelsNode);
        if (names.isEmpty()) {
            return new ProviderDiscoveryVO(false, "连接成功，但当前还没有可用模型，请先执行 ollama pull。", List.of(), List.of());
        }

        return buildDiscoveryResult(names);
    }

    private ProviderDiscoveryVO discoverCompatibleModels(AIProvider provider) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(
                createJsonGetBuilder(
                        URI.create(buildCompatibleModelsUrl(provider.getBaseUrl())),
                        provider,
                        resolveTimeoutSeconds(provider),
                        "application/json"
                ).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return new ProviderDiscoveryVO(
                    false,
                    "连接成功，但获取模型列表失败，状态码：" + response.statusCode(),
                    List.of(),
                    List.of()
            );
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode modelsNode = root.path("data");
        if (!modelsNode.isArray()) {
            return new ProviderDiscoveryVO(false, "兼容接口返回内容不符合预期。", List.of(), List.of());
        }

        Set<String> names = extractModelNames(modelsNode);
        if (names.isEmpty()) {
            return new ProviderDiscoveryVO(false, "连接成功，但当前没有可用模型。", List.of(), List.of());
        }

        return buildDiscoveryResult(names);
    }

    private Set<String> extractModelNames(JsonNode modelsNode) {
        Set<String> names = new LinkedHashSet<>();
        for (JsonNode item : modelsNode) {
            String name = "";
            if (item.hasNonNull("id")) {
                name = item.get("id").asText();
            } else if (item.hasNonNull("name")) {
                name = item.get("name").asText();
            } else if (item.hasNonNull("model")) {
                name = item.get("model").asText();
            }
            name = StringUtils.trimWhitespace(name);
            if (StringUtils.hasText(name)) {
                names.add(name);
            }
        }
        return names;
    }

    private ProviderDiscoveryVO buildDiscoveryResult(Set<String> names) {
        List<String> allModels = new ArrayList<>(names);
        allModels.sort(String::compareToIgnoreCase);

        List<String> modelOptions = allModels.stream()
                .filter(name -> !isEmbeddingModel(name))
                .toList();
        List<String> embeddingOptions = allModels.stream()
                .filter(this::isEmbeddingModel)
                .toList();

        if (modelOptions.isEmpty()) {
            modelOptions = allModels;
        }
        if (embeddingOptions.isEmpty()) {
            embeddingOptions = allModels;
        }

        return new ProviderDiscoveryVO(
                true,
                "连接成功，已获取 " + allModels.size() + " 个模型。",
                new ArrayList<>(modelOptions),
                new ArrayList<>(embeddingOptions)
        );
    }

    private ObjectNode buildOllamaChatRequestBody(
            AIProvider provider,
            String modelName,
            String systemPrompt,
            String userPrompt,
            Double temperature,
            Integer maxTokens,
            boolean stream) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", modelName);
        requestBody.set("messages", buildMessages(systemPrompt, userPrompt));
        requestBody.put("stream", stream);
        requestBody.put("think", false);

        ObjectNode options = requestBody.putObject("options");
        options.put("temperature", resolveTemperature(provider, temperature));
        if (provider.getTopP() != null) {
            options.put("top_p", provider.getTopP());
        }
        options.put("num_predict", resolveMaxTokens(provider, maxTokens));
        return requestBody;
    }

    private ObjectNode buildCompatibleRequestBody(
            AIProvider provider,
            String modelName,
            String systemPrompt,
            String userPrompt,
            Double temperature,
            Integer maxTokens,
            boolean stream) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", modelName);
        requestBody.set("messages", buildMessages(systemPrompt, userPrompt));
        requestBody.put("temperature", resolveTemperature(provider, temperature));
        if (provider.getTopP() != null) {
            requestBody.put("top_p", provider.getTopP());
        }
        requestBody.put("max_tokens", resolveMaxTokens(provider, maxTokens));
        requestBody.put("stream", stream);
        if ("ollama".equalsIgnoreCase(provider.getProviderType())) {
            requestBody.put("reasoning_effort", "none");
        }
        return requestBody;
    }

    private ObjectNode buildCompatibleToolRequestBody(
            AIProvider provider,
            String modelName,
            ArrayNode messages,
            List<ToolDefinition> tools,
            Integer maxTokens) throws IOException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", modelName);
        requestBody.set("messages", messages);
        requestBody.put("temperature", resolveTemperature(provider, null));
        if (provider.getTopP() != null) {
            requestBody.put("top_p", provider.getTopP());
        }
        requestBody.put("max_tokens", resolveMaxTokens(provider, maxTokens));
        requestBody.put("stream", false);
        requestBody.put("tool_choice", "auto");
        requestBody.set("tools", buildToolDefinitions(tools));
        if ("ollama".equalsIgnoreCase(provider.getProviderType())) {
            requestBody.put("reasoning_effort", "none");
        }
        return requestBody;
    }

    private ArrayNode buildMessages(String systemPrompt, String userPrompt) {
        ArrayNode messages = objectMapper.createArrayNode();

        if (StringUtils.hasText(systemPrompt)) {
            ObjectNode system = objectMapper.createObjectNode();
            system.put("role", "system");
            system.put("content", systemPrompt.trim());
            messages.add(system);
        }

        ObjectNode user = objectMapper.createObjectNode();
        user.put("role", "user");
        user.put("content", StringUtils.hasText(userPrompt) ? userPrompt.trim() : "");
        messages.add(user);

        return messages;
    }

    private ArrayNode buildToolDefinitions(List<ToolDefinition> tools) throws IOException {
        ArrayNode toolArray = objectMapper.createArrayNode();
        for (ToolDefinition tool : tools) {
            if (tool == null || !StringUtils.hasText(tool.name())) {
                continue;
            }
            ObjectNode toolNode = objectMapper.createObjectNode();
            toolNode.put("type", "function");
            ObjectNode functionNode = toolNode.putObject("function");
            functionNode.put("name", tool.name().trim());
            functionNode.put("description", StringUtils.hasText(tool.description()) ? tool.description().trim() : "");
            String inputSchemaJson = StringUtils.hasText(tool.inputSchemaJson())
                    ? tool.inputSchemaJson().trim()
                    : "{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}";
            functionNode.set("parameters", objectMapper.readTree(inputSchemaJson));
            toolArray.add(toolNode);
        }
        return toolArray;
    }

    private List<ToolCallRequest> extractToolCallRequests(JsonNode assistantMessage) throws IOException {
        JsonNode toolCallsNode = assistantMessage.path("tool_calls");
        if (!toolCallsNode.isArray() || toolCallsNode.isEmpty()) {
            return List.of();
        }

        List<ToolCallRequest> requests = new ArrayList<>();
        for (JsonNode item : toolCallsNode) {
            String id = item.path("id").asText("");
            JsonNode functionNode = item.path("function");
            String name = functionNode.path("name").asText("");
            if (!StringUtils.hasText(id) || !StringUtils.hasText(name)) {
                continue;
            }

            JsonNode argumentsNode = functionNode.path("arguments");
            String argumentsJson;
            if (argumentsNode.isTextual()) {
                argumentsJson = argumentsNode.asText("{}");
            } else if (argumentsNode.isObject() || argumentsNode.isArray()) {
                argumentsJson = objectMapper.writeValueAsString(argumentsNode);
            } else {
                argumentsJson = "{}";
            }
            requests.add(new ToolCallRequest(id, name.trim(), argumentsJson));
        }
        return requests;
    }

    private ObjectNode buildAssistantToolCallMessage(JsonNode assistantMessage, String content) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "assistant");
        if (assistantMessage != null) {
            JsonNode toolCallsNode = assistantMessage.path("tool_calls");
            if (toolCallsNode.isArray() && !toolCallsNode.isEmpty()) {
                message.set("tool_calls", toolCallsNode.deepCopy());
            }
        }
        if (StringUtils.hasText(content)) {
            message.put("content", content);
        } else {
            message.putNull("content");
        }
        return message;
    }

    private ObjectNode buildToolResultMessage(String toolCallId, String resultJson) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "tool");
        message.put("tool_call_id", toolCallId);
        message.put("content", StringUtils.hasText(resultJson) ? resultJson : "{}");
        return message;
    }

    private String extractOllamaContent(JsonNode root) {
        String content = root.path("message").path("content").asText("");
        if (!hasRawText(content)) {
            content = root.path("response").asText("");
        }
        return content;
    }

    private String extractCompatibleContent(JsonNode root) {
        JsonNode firstChoice = getFirstChoice(root);
        if (firstChoice == null) {
            return "";
        }

        String content = extractMessageContent(firstChoice.path("message"));
        if (!hasRawText(content)) {
            content = firstChoice.path("text").asText("");
        }
        if (!hasRawText(content)) {
            content = firstChoice.path("delta").path("content").asText("");
        }
        return content;
    }

    private String extractCompatibleDelta(JsonNode root) {
        JsonNode firstChoice = getFirstChoice(root);
        if (firstChoice == null) {
            return "";
        }

        String delta = extractMessageContent(firstChoice.path("delta"));
        if (!hasRawText(delta)) {
            delta = extractMessageContent(firstChoice.path("message"));
        }
        if (!hasRawText(delta)) {
            delta = firstChoice.path("text").asText("");
        }
        return delta;
    }

    private String extractMessageContent(JsonNode messageNode) {
        if (messageNode == null || messageNode.isMissingNode() || messageNode.isNull()) {
            return "";
        }
        JsonNode contentNode = messageNode.path("content");
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                if (item == null || item.isNull()) {
                    continue;
                }
                String type = item.path("type").asText("");
                if (!StringUtils.hasText(type) || "text".equals(type)) {
                    String text = item.path("text").asText(item.asText(""));
                    if (hasRawText(text)) {
                        builder.append(text);
                    }
                }
            }
            return builder.toString();
        }
        return contentNode.asText("");
    }

    private JsonNode getFirstChoice(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return null;
        }
        return choices.get(0);
    }

    private void validateStreamPayload(JsonNode root) {
        JsonNode errorNode = root.path("error");
        if (!errorNode.isMissingNode() && !errorNode.isNull()) {
            String message = errorNode.isTextual()
                    ? errorNode.asText()
                    : errorNode.path("message").asText("");
            throw new IllegalStateException(
                    StringUtils.hasText(message) ? message : "模型服务流式返回了错误信息"
            );
        }
    }

    private boolean isCompatibleStreamFinished(JsonNode root) {
        if (root.path("done").asBoolean(false)) {
            return true;
        }

        JsonNode firstChoice = getFirstChoice(root);
        if (firstChoice == null) {
            return false;
        }

        JsonNode finishReason = firstChoice.path("finish_reason");
        return !finishReason.isMissingNode() && !finishReason.isNull() && hasRawText(finishReason.asText(""));
    }

    private String readErrorBody(InputStream inputStream) {
        try {
            String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
            return body.isEmpty() ? "" : "- " + body;
        } catch (IOException exception) {
            return "";
        }
    }

    private boolean hasRawText(String value) {
        return value != null && !value.isEmpty();
    }

    private String executeToolSafely(Function<ToolCallRequest, String> toolExecutor, ToolCallRequest request) {
        try {
            String result = toolExecutor.apply(request);
            return StringUtils.hasText(result) ? result.trim() : "{}";
        } catch (Exception exception) {
            return writeToolErrorJson("tool_execution_failed", exception.getMessage());
        }
    }

    private String writeToolErrorJson(String code, String message) {
        try {
            ObjectNode error = objectMapper.createObjectNode();
            error.put("error", code);
            error.put("message", StringUtils.hasText(message) ? message.trim() : "工具执行失败");
            return objectMapper.writeValueAsString(error);
        } catch (Exception exception) {
            return "{\"error\":\"" + code + "\"}";
        }
    }

    private String compactError(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return "";
        }
        String compact = responseBody.replaceAll("\\s+", " ").trim();
        return compact.length() > 240 ? compact.substring(0, 240) + "..." : compact;
    }

    private double resolveTemperature(AIProvider provider, Double overrideTemperature) {
        if (overrideTemperature != null) {
            return overrideTemperature;
        }
        return provider.getTemperature() != null ? provider.getTemperature() : 0.7;
    }

    private int resolveMaxTokens(AIProvider provider, Integer overrideMaxTokens) {
        if (overrideMaxTokens != null && overrideMaxTokens > 0) {
            return overrideMaxTokens;
        }
        return provider.getMaxTokens() != null && provider.getMaxTokens() > 0 ? provider.getMaxTokens() : 512;
    }

    private boolean isEmbeddingModel(String name) {
        String normalized = name == null ? "" : name.toLowerCase();
        return normalized.contains("embed")
                || normalized.contains("embedding")
                || normalized.contains("bge")
                || normalized.contains("nomic")
                || normalized.contains("mxbai");
    }

    private void clearOtherDefaults(Long currentId) {
        if (currentId == null) {
            return;
        }
        QueryWrapper<AIProvider> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("id", currentId).eq("deleted", 0).eq("is_default", 1);
        List<AIProvider> providers = list(queryWrapper);
        for (AIProvider provider : providers) {
            provider.setIsDefault(0);
            updateById(provider);
        }
    }
}
