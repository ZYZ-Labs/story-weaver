package com.storyweaver.service;

import com.storyweaver.domain.entity.AIProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AIModelRoutingServiceTest {

    @Test
    void shouldRouteChapterWorkspaceSceneDraftToWritingConfig() {
        AIProvider writingProvider = createProvider(11L, "writing-provider-model");
        AIProvider defaultProvider = createProvider(22L, "default-provider-model");

        AIProviderService aiProviderService = (AIProviderService) Proxy.newProxyInstance(
                AIProviderService.class.getClassLoader(),
                new Class<?>[]{AIProviderService.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getById" -> {
                        Long providerId = (Long) args[0];
                        if (providerId.equals(writingProvider.getId())) {
                            yield writingProvider;
                        }
                        if (providerId.equals(defaultProvider.getId())) {
                            yield defaultProvider;
                        }
                        yield null;
                    }
                    case "listProviders" -> List.of(writingProvider, defaultProvider);
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        SystemConfigService systemConfigService = (SystemConfigService) Proxy.newProxyInstance(
                SystemConfigService.class.getClassLoader(),
                new Class<?>[]{SystemConfigService.class},
                (proxy, method, args) -> {
                    if (!"getConfigValue".equals(method.getName())) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return Map.of(
                            "writing_ai_provider_id", "11",
                            "default_ai_provider_id", "22",
                            "writing_ai_model", "writing-model",
                            "default_ai_model", "default-model"
                    ).get(args[0]);
                }
        );

        AIModelRoutingService service = new AIModelRoutingService(aiProviderService, systemConfigService);
        AIModelRoutingService.ResolvedModelSelection selection = service.resolve(
                null,
                null,
                "phase8.chapter-workspace.scene-draft"
        );

        assertEquals(11L, selection.provider().getId());
        assertEquals("writing-model", selection.model());
    }

    private AIProvider createProvider(Long id, String modelName) {
        AIProvider provider = new AIProvider();
        provider.setId(id);
        provider.setModelName(modelName);
        provider.setEnabled(1);
        provider.setDeleted(0);
        return provider;
    }
}
