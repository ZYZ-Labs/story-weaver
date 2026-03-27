package com.storyweaver.service;

import com.storyweaver.domain.entity.AIProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AIModelRoutingService {

    private final AIProviderService aiProviderService;
    private final SystemConfigService systemConfigService;

    public AIModelRoutingService(AIProviderService aiProviderService, SystemConfigService systemConfigService) {
        this.aiProviderService = aiProviderService;
        this.systemConfigService = systemConfigService;
    }

    public ResolvedModelSelection resolve(Long selectedProviderId, String selectedModel, String entryPoint) {
        AIProvider provider = resolveProvider(selectedProviderId, entryPoint);
        String model = resolveModel(provider, selectedModel, entryPoint);
        return new ResolvedModelSelection(provider, model);
    }

    public AIProvider resolveProvider(Long selectedProviderId, String entryPoint) {
        if (selectedProviderId != null) {
            AIProvider provider = aiProviderService.getById(selectedProviderId);
            if (isAvailable(provider)) {
                return provider;
            }
        }

        Long configuredProviderId = parseLong(systemConfigService.getConfigValue(resolveProviderKey(entryPoint)));
        if (configuredProviderId != null) {
            AIProvider provider = aiProviderService.getById(configuredProviderId);
            if (isAvailable(provider)) {
                return provider;
            }
        }

        Long fallbackProviderId = parseLong(systemConfigService.getConfigValue("default_ai_provider_id"));
        if (fallbackProviderId != null) {
            AIProvider provider = aiProviderService.getById(fallbackProviderId);
            if (isAvailable(provider)) {
                return provider;
            }
        }

        return aiProviderService.listProviders().stream()
                .filter(this::isAvailable)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("当前没有可用的模型服务，请先启用一个 Provider"));
    }

    public String resolveModel(AIProvider provider, String selectedModel, String entryPoint) {
        if (StringUtils.hasText(selectedModel)) {
            return selectedModel.trim();
        }

        String configuredModel = systemConfigService.getConfigValue(resolveModelKey(entryPoint));
        if (StringUtils.hasText(configuredModel)) {
            return configuredModel.trim();
        }

        String fallbackModel = systemConfigService.getConfigValue("default_ai_model");
        if (StringUtils.hasText(fallbackModel)) {
            return fallbackModel.trim();
        }

        if (provider != null && StringUtils.hasText(provider.getModelName())) {
            return provider.getModelName().trim();
        }

        throw new IllegalStateException("当前默认模型尚未配置，请先在系统设置里指定模型");
    }

    public String normalizeEntryPoint(String entryPoint) {
        if (!StringUtils.hasText(entryPoint)) {
            return "default";
        }
        return entryPoint.trim().toLowerCase();
    }

    private boolean isAvailable(AIProvider provider) {
        return provider != null
                && !Integer.valueOf(1).equals(provider.getDeleted())
                && Integer.valueOf(1).equals(provider.getEnabled());
    }

    private String resolveProviderKey(String entryPoint) {
        return switch (normalizeEntryPoint(entryPoint)) {
            case "draft" -> "draft_ai_provider_id";
            case "writing-center", "writing_center", "writing" -> "writing_ai_provider_id";
            default -> "default_ai_provider_id";
        };
    }

    private String resolveModelKey(String entryPoint) {
        return switch (normalizeEntryPoint(entryPoint)) {
            case "draft" -> "draft_ai_model";
            case "writing-center", "writing_center", "writing" -> "writing_ai_model";
            default -> "default_ai_model";
        };
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public record ResolvedModelSelection(AIProvider provider, String model) {
    }
}
