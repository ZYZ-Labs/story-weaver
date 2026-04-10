package com.storyweaver.story.generation;

import com.storyweaver.domain.entity.Chapter;
import lombok.Data;

@Data
public class ChapterAnchorUpdateResult {

    private Chapter chapter;

    private ChapterAnchorBundle anchorBundle;

    private GenerationReadinessVO generationReadiness;
}
