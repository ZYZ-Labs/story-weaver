package com.storyweaver.domain.vo;

import java.util.List;

public record ProviderDiscoveryVO(
        boolean success,
        String message,
        List<String> models,
        List<String> embeddingModels
) {
}
