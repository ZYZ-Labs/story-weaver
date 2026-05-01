package com.storyweaver.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.service.SystemConfigService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AIProviderServiceImplTest {

    @Test
    void shouldUseProviderTimeoutWhenGlobalTimeoutIsLarger() {
        AIProviderServiceImpl service = createService("3600");

        AIProvider provider = new AIProvider();
        provider.setTimeoutSeconds(60);

        assertEquals(60, service.resolveRequestTimeoutSeconds(provider));
    }

    @Test
    void shouldRespectSmallerGlobalTimeoutCap() {
        AIProviderServiceImpl service = createService("45");

        AIProvider provider = new AIProvider();
        provider.setTimeoutSeconds(60);

        assertEquals(45, service.resolveRequestTimeoutSeconds(provider));
    }

    @Test
    void shouldFallbackToProviderDefaultWhenProviderTimeoutMissing() {
        AIProviderServiceImpl service = createService("0");

        AIProvider provider = new AIProvider();

        assertEquals(60, service.resolveRequestTimeoutSeconds(provider));
    }

    private AIProviderServiceImpl createService(String configuredTimeoutValue) {
        SystemConfigService systemConfigService = (SystemConfigService) Proxy.newProxyInstance(
                SystemConfigService.class.getClassLoader(),
                new Class<?>[]{SystemConfigService.class},
                (proxy, method, args) -> {
                    if ("getConfigValue".equals(method.getName())) {
                        return configuredTimeoutValue;
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
        return new AIProviderServiceImpl(new ObjectMapper(), systemConfigService);
    }
}
