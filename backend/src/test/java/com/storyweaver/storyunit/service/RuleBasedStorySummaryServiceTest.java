package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.facet.summary.DefaultSummaryFacet;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.service.impl.RuleBasedStorySummaryService;
import com.storyweaver.storyunit.summary.StorySummaryDraft;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleBasedStorySummaryServiceTest {

    private final RuleBasedStorySummaryService service = new RuleBasedStorySummaryService();

    @Test
    void shouldGenerateStableSummaryFacetFromDraft() {
        DefaultSummaryFacet summary = service.summarize(new StorySummaryDraft(
                StoryUnitType.CHAPTER,
                "第一章 雨夜来信",
                List.of("", "主角在雨夜收到一封改变命运的来信"),
                List.of("", "第一章从主角的日常切入，并以雨夜来信触发主线"),
                List.of("章节状态：draft", "字数：0", "字数：0"),
                List.of("关联大纲：卷一开篇", "POV：林沉舟", "POV：林沉舟"),
                List.of(),
                List.of("这一章是否需要补全 reader reveal")
        ));

        assertEquals("第一章 雨夜来信", summary.displayTitle());
        assertEquals("主角在雨夜收到一封改变命运的来信", summary.oneLineSummary());
        assertEquals("第一章从主角的日常切入，并以雨夜来信触发主线", summary.longSummary());
        assertEquals("章节状态：draft；字数：0", summary.stateSummary());
        assertEquals("关联大纲：卷一开篇；POV：林沉舟", summary.relationSummary());
        assertEquals(List.of("这一章是否需要补全 reader reveal"), summary.pendingQuestions());
    }

    @Test
    void shouldFallbackToDisplayTitleWhenSummaryCandidatesAreBlank() {
        DefaultSummaryFacet summary = service.summarize(new StorySummaryDraft(
                StoryUnitType.CHARACTER,
                "林沉舟",
                List.of("", " "),
                List.of(),
                List.of("状态：活跃"),
                List.of("关联项目 1 个"),
                List.of(),
                List.of()
        ));

        assertEquals("林沉舟", summary.displayTitle());
        assertEquals("林沉舟", summary.oneLineSummary());
        assertEquals("林沉舟", summary.longSummary());
        assertEquals("状态：活跃", summary.stateSummary());
        assertTrue(summary.changeSummary().isEmpty());
    }
}
