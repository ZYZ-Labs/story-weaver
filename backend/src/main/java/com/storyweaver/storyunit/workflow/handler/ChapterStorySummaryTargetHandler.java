package com.storyweaver.storyunit.workflow.handler;

import com.storyweaver.domain.dto.StructuredSummaryApplyRequestDTO;
import com.storyweaver.domain.dto.ChapterRequestDTO;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.story.generation.StructuredSummaryApplyResult;
import com.storyweaver.story.generation.StructuredSummaryApplyService;
import com.storyweaver.service.ChapterService;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.service.StoryUnitQueryService;
import com.storyweaver.storyunit.summary.StorySummaryService;
import com.storyweaver.storyunit.summary.workflow.StructuredPatchProposal;
import com.storyweaver.storyunit.summary.workflow.SummaryApplyCommand;
import com.storyweaver.storyunit.summary.workflow.SummaryApplyResult;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChapterStorySummaryTargetHandler extends AbstractStorySummaryTargetHandler {

    private final StructuredSummaryApplyService structuredSummaryApplyService;
    private final ChapterService chapterService;

    public ChapterStorySummaryTargetHandler(
            StoryUnitQueryService storyUnitQueryService,
            StorySummaryService storySummaryService,
            StructuredSummaryApplyService structuredSummaryApplyService,
            ChapterService chapterService) {
        super(storyUnitQueryService, storySummaryService);
        this.structuredSummaryApplyService = structuredSummaryApplyService;
        this.chapterService = chapterService;
    }

    @Override
    public StoryUnitType targetType() {
        return StoryUnitType.CHAPTER;
    }

    @Override
    public SummaryApplyResult apply(Long userId, SummaryApplyCommand command, StructuredPatchProposal proposal) {
        if (!command.confirmed()) {
            throw new IllegalArgumentException("未确认的 proposal 不能写回");
        }
        if (isCreateProposal(proposal)) {
            ChapterRequestDTO requestDTO = new ChapterRequestDTO();
            requestDTO.setTitle(createPreviewTitle(proposal.proposalSummary()));
            requestDTO.setSummary(proposal.proposalSummary());
            requestDTO.setChapterStatus("draft");
            requestDTO.setOrderNum(nextChapterOrder(proposal.projectId(), userId));

            Chapter created = chapterService.createChapter(proposal.projectId(), userId, requestDTO);
            if (created == null || created.getId() == null) {
                throw new IllegalStateException("章节创建失败");
            }

            StoryUnitRef targetRef = toTargetRef(created.getId());
            return new SummaryApplyResult(
                    true,
                    currentSummary(targetRef),
                    targetRef,
                    List.of("已根据摘要创建章节；正文、锚点与剧情绑定仍需后续补充")
            );
        }

        StructuredSummaryApplyRequestDTO requestDTO = new StructuredSummaryApplyRequestDTO();
        requestDTO.setScope("chapter");
        requestDTO.setProjectId(proposal.projectId());
        requestDTO.setTargetId(Long.valueOf(proposal.targetRef().unitId()));
        requestDTO.setCanonSummaryText(proposal.proposalSummary());

        Map<String, Object> structuredFields = new LinkedHashMap<>();
        structuredFields.put("summary", proposal.proposalSummary());
        requestDTO.setStructuredFields(structuredFields);

        StructuredSummaryApplyResult result = structuredSummaryApplyService.apply(userId, requestDTO);
        List<String> warnings = result.getGenerationReadiness() == null
                ? List.of("首轮只写回章节摘要，不修改正文")
                : List.of("首轮只写回章节摘要，不修改正文", "readiness 已重新评估");

        return new SummaryApplyResult(
                true,
                currentSummary(proposal.targetRef()),
                proposal.targetRef(),
                warnings
        );
    }

    @Override
    protected String summaryFieldPath() {
        return "/summary";
    }

    @Override
    protected String targetLabel() {
        return "章节";
    }

    @Override
    protected List<String> riskNotes() {
        return List.of("首轮只写回章节摘要，不自动修改正文、锚点和剧情绑定");
    }

    @Override
    protected String createPreviewTitle(String summaryText) {
        String derived = super.createPreviewTitle(summaryText);
        return derived.length() > 18 ? derived.substring(0, 18) : derived;
    }

    private Integer nextChapterOrder(Long projectId, Long userId) {
        return chapterService.getProjectChapters(projectId, userId).stream()
                .map(Chapter::getOrderNum)
                .filter(order -> order != null && order > 0)
                .max(Integer::compareTo)
                .map(order -> order + 1)
                .orElse(1);
    }
}
