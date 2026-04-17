package com.storyweaver.storyunit.context.impl;

import com.storyweaver.storyunit.context.StoryUnitSummaryView;
import com.storyweaver.storyunit.facet.summary.DefaultSummaryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StoryScope;
import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.model.StoryUnit;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitStatus;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.model.StoryUnitVersion;
import com.storyweaver.storyunit.service.ProjectedStoryUnit;
import com.storyweaver.storyunit.service.StoryUnitQueryService;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultStoryUnitSummaryQueryServiceTest {

    @Test
    void shouldBuildSummaryViewFromProjectedStoryUnit() {
        StoryUnitQueryService storyUnitQueryService = mock(StoryUnitQueryService.class);
        StoryUnitRef ref = new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER);
        StoryUnit unit = new StoryUnit(
                ref,
                28L,
                StoryScope.CHAPTER,
                Map.of(),
                StoryUnitStatus.REFINED,
                new StoryUnitVersion(1),
                "snapshot-31",
                new StorySourceTrace("test", "test", "test", "31")
        );
        DefaultSummaryFacet summaryFacet = new DefaultSummaryFacet(
                "第一章 雨夜来信",
                "林沉舟在雨夜收到邀请",
                "第一章从退役后的日常切入，并以一封邀请信重新拉回主线。",
                "",
                "",
                "",
                java.util.List.of()
        );
        ProjectedStoryUnit projected = new ProjectedStoryUnit(unit, Map.of(FacetType.SUMMARY, summaryFacet));

        when(storyUnitQueryService.getProjected(ref)).thenReturn(Optional.of(projected));

        DefaultStoryUnitSummaryQueryService service = new DefaultStoryUnitSummaryQueryService(storyUnitQueryService);

        Optional<StoryUnitSummaryView> result = service.getStoryUnitSummary(ref);

        assertTrue(result.isPresent());
        assertEquals(StoryUnitType.CHAPTER, result.get().unitType());
        assertEquals("第一章 雨夜来信", result.get().title());
        assertEquals("第一章从退役后的日常切入，并以一封邀请信重新拉回主线。", result.get().summary());
    }
}
