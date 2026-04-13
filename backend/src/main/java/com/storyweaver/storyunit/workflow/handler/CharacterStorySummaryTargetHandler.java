package com.storyweaver.storyunit.workflow.handler;

import com.storyweaver.domain.dto.StructuredSummaryApplyRequestDTO;
import com.storyweaver.domain.dto.CharacterRequestDTO;
import com.storyweaver.story.generation.StructuredSummaryApplyResult;
import com.storyweaver.story.generation.StructuredSummaryApplyService;
import com.storyweaver.service.CharacterService;
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
public class CharacterStorySummaryTargetHandler extends AbstractStorySummaryTargetHandler {

    private final StructuredSummaryApplyService structuredSummaryApplyService;
    private final CharacterService characterService;

    public CharacterStorySummaryTargetHandler(
            StoryUnitQueryService storyUnitQueryService,
            StorySummaryService storySummaryService,
            StructuredSummaryApplyService structuredSummaryApplyService,
            CharacterService characterService) {
        super(storyUnitQueryService, storySummaryService);
        this.structuredSummaryApplyService = structuredSummaryApplyService;
        this.characterService = characterService;
    }

    @Override
    public StoryUnitType targetType() {
        return StoryUnitType.CHARACTER;
    }

    @Override
    public SummaryApplyResult apply(Long userId, SummaryApplyCommand command, StructuredPatchProposal proposal) {
        if (!command.confirmed()) {
            throw new IllegalArgumentException("未确认的 proposal 不能写回");
        }
        if (isCreateProposal(proposal)) {
            CharacterRequestDTO requestDTO = new CharacterRequestDTO();
            requestDTO.setName(createPreviewTitle(proposal.proposalSummary()));
            requestDTO.setDescription(proposal.proposalSummary());
            requestDTO.setProjectRole("配角");
            requestDTO.setRoleType("配角");

            com.storyweaver.domain.entity.Character created = characterService.createCharacter(proposal.projectId(), userId, requestDTO);
            if (created == null) {
                throw new IllegalStateException("人物创建失败");
            }

            StoryUnitRef targetRef = toTargetRef(created.getId());
            return new SummaryApplyResult(
                    true,
                    currentSummary(targetRef),
                    targetRef,
                    List.of("已根据摘要创建人物；身份、目标和成长弧仍建议在专家模式补充")
            );
        }

        StructuredSummaryApplyRequestDTO requestDTO = new StructuredSummaryApplyRequestDTO();
        requestDTO.setScope("character");
        requestDTO.setProjectId(proposal.projectId());
        requestDTO.setTargetId(Long.valueOf(proposal.targetRef().unitId()));
        requestDTO.setCanonSummaryText(proposal.proposalSummary());

        Map<String, Object> structuredFields = new LinkedHashMap<>();
        structuredFields.put("description", proposal.proposalSummary());
        requestDTO.setStructuredFields(structuredFields);

        StructuredSummaryApplyResult result = structuredSummaryApplyService.apply(userId, requestDTO);
        return new SummaryApplyResult(
                true,
                currentSummary(proposal.targetRef()),
                proposal.targetRef(),
                result.getCanonDocumentId() == null ? List.of() : List.of("已同步更新 canon document")
        );
    }

    @Override
    protected String summaryFieldPath() {
        return "/description";
    }

    @Override
    protected String targetLabel() {
        return "人物";
    }

    @Override
    protected List<String> riskNotes() {
        return List.of("首轮只写回人物描述，不自动拆解身份、目标和成长弧");
    }

    @Override
    protected String createPreviewTitle(String summaryText) {
        String derived = super.createPreviewTitle(summaryText);
        return derived.length() > 12 ? derived.substring(0, 12) : derived;
    }
}
