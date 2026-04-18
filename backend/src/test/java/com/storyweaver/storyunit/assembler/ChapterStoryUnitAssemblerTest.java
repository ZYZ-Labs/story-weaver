package com.storyweaver.storyunit.assembler;

import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.storyunit.facet.summary.DefaultSummaryFacet;
import com.storyweaver.storyunit.projection.ChapterProjectionSource;
import com.storyweaver.storyunit.service.impl.RuleBasedStorySummaryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChapterStoryUnitAssemblerTest {

    @Test
    void shouldAssembleSummaryFacetWhenChapterSummaryIsNull() {
        Chapter chapter = new Chapter();
        chapter.setId(32L);
        chapter.setProjectId(28L);
        chapter.setTitle("算法少女苏晚");
        chapter.setSummary(null);
        chapter.setContent("苏晚在训练室里拆解旧比赛录像，并指出主角决策逻辑的盲区。");
        chapter.setChapterStatus("draft");
        chapter.setWordCount(3546);

        ChapterProjectionSource source = new ChapterProjectionSource(
                chapter,
                null,
                null,
                List.of(),
                List.of()
        );

        ChapterStoryUnitAssembler assembler = new ChapterStoryUnitAssembler(new RuleBasedStorySummaryService());
        var storyUnit = assembler.adapter().toStoryUnit(source);

        var summaryFacet = assembler.assembleSummaryFacet(source, storyUnit);

        assertTrue(summaryFacet.isPresent());
        DefaultSummaryFacet facet = (DefaultSummaryFacet) summaryFacet.orElseThrow();
        assertEquals("算法少女苏晚", facet.displayTitle());
        assertEquals("算法少女苏晚", facet.oneLineSummary());
        assertTrue(facet.longSummary().contains("苏晚在训练室里拆解旧比赛录像"));
    }
}
