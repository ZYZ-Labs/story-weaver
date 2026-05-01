package com.storyweaver.story.generation.orchestration;

import java.util.Locale;

public enum ChapterNarrativeRuntimeMode {
    SCENE("scene"),
    NODE("node");

    private final String apiValue;

    ChapterNarrativeRuntimeMode(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }

    public static ChapterNarrativeRuntimeMode fromValue(String value) {
        if (value == null || value.isBlank()) {
            return SCENE;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ChapterNarrativeRuntimeMode item : values()) {
            if (item.apiValue.equals(normalized)) {
                return item;
            }
        }
        throw new IllegalArgumentException("不支持的章节运行模式: " + value);
    }
}
