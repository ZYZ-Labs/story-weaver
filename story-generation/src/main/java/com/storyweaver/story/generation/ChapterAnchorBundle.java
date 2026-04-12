package com.storyweaver.story.generation;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ChapterAnchorBundle {

    private Long chapterId;

    private Long projectId;

    private Long chapterOutlineId;

    private Long volumeOutlineId;

    private Long mainPovCharacterId;

    private String mainPovCharacterName;

    private List<Long> requiredCharacterIds = new ArrayList<>();

    private List<String> requiredCharacterNames = new ArrayList<>();

    private List<Long> storyBeatIds = new ArrayList<>();

    private List<String> storyBeatTitles = new ArrayList<>();

    private List<Long> relatedWorldSettingIds = new ArrayList<>();

    private List<String> relatedWorldSettingNames = new ArrayList<>();

    private String chapterSummary;

    private String chapterStatus;

    private Map<String, String> anchorSources = new LinkedHashMap<>();

    public void markSource(String anchorKey, String source) {
        if (anchorKey == null || anchorKey.isBlank() || source == null || source.isBlank()) {
            return;
        }
        anchorSources.put(anchorKey, source.trim());
    }

    public String sourceOf(String anchorKey) {
        return anchorSources.getOrDefault(anchorKey, "missing");
    }

    public boolean hasCharacterAnchor() {
        return mainPovCharacterId != null || (requiredCharacterIds != null && !requiredCharacterIds.isEmpty());
    }
}
