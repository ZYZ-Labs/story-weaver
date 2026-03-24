package com.storyweaver.domain.vo;

import java.util.List;

public record NameSuggestionVO(
        List<String> suggestions,
        String providerName,
        String modelName
) {
}
