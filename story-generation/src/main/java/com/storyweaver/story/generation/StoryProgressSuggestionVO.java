package com.storyweaver.story.generation;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class StoryProgressSuggestionVO {

    private String scope;

    private String predictedProgressType;

    private Integer confidence;

    private List<String> reasons = new ArrayList<>();

    private BasedOn basedOn = new BasedOn();

    private Map<String, Object> recommendedFields = new LinkedHashMap<>();

    public void addReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return;
        }
        reasons.add(reason.trim());
    }

    public void putRecommendedField(String key, Object value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        recommendedFields.put(key, value);
    }

    @Data
    public static class BasedOn {

        private Integer chapterCount = 0;

        private Integer plotCount = 0;

        private Integer causalityCount = 0;

        private List<Long> existingChapterIds = new ArrayList<>();

        private List<Long> existingPlotIds = new ArrayList<>();

        private List<Long> existingCausalityIds = new ArrayList<>();

        private Long currentOutlineId;

        private Long currentChapterId;
    }
}
