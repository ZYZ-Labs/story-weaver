package com.storyweaver.storyunit.workflow.handler;

import com.storyweaver.domain.dto.WorldSettingDTO;
import com.storyweaver.domain.vo.WorldSettingVO;
import com.storyweaver.service.WorldSettingService;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.service.StoryUnitQueryService;
import com.storyweaver.storyunit.summary.StorySummaryService;
import com.storyweaver.storyunit.summary.workflow.StructuredPatchProposal;
import com.storyweaver.storyunit.summary.workflow.SummaryApplyCommand;
import com.storyweaver.storyunit.summary.workflow.SummaryApplyResult;
import com.storyweaver.storyunit.workflow.exception.SummaryProposalConflictException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorldSettingStorySummaryTargetHandler extends AbstractStorySummaryTargetHandler {

    private final WorldSettingService worldSettingService;

    public WorldSettingStorySummaryTargetHandler(
            StoryUnitQueryService storyUnitQueryService,
            StorySummaryService storySummaryService,
            WorldSettingService worldSettingService) {
        super(storyUnitQueryService, storySummaryService);
        this.worldSettingService = worldSettingService;
    }

    @Override
    public StoryUnitType targetType() {
        return StoryUnitType.WORLD_SETTING;
    }

    @Override
    public SummaryApplyResult apply(Long userId, SummaryApplyCommand command, StructuredPatchProposal proposal) {
        if (!command.confirmed()) {
            throw new IllegalArgumentException("未确认的 proposal 不能写回");
        }
        if (isCreateProposal(proposal)) {
            WorldSettingDTO create = new WorldSettingDTO();
            create.setProjectId(proposal.projectId());
            create.setName(createPreviewTitle(proposal.proposalSummary()));
            create.setDescription(proposal.proposalSummary());
            create.setCategory("世界规则");

            WorldSettingVO created = worldSettingService.createWorldSetting(create, userId);
            if (created == null || created.getId() == null) {
                throw new IllegalStateException("世界观创建失败");
            }

            return new SummaryApplyResult(
                    true,
                    currentSummary(toTargetRef(created.getId())),
                    toTargetRef(created.getId()),
                    List.of("已根据摘要创建世界观；分类与结构字段仍建议在专家模式补充")
            );
        }

        Long worldSettingId = Long.valueOf(proposal.targetRef().unitId());
        WorldSettingVO current = worldSettingService.getWorldSettingById(worldSettingId, userId);
        if (current == null) {
            throw new IllegalArgumentException("世界观不存在或无权访问");
        }
        if (!current.getProjectId().equals(proposal.projectId())) {
            throw new SummaryProposalConflictException("proposal 项目与当前世界观不匹配");
        }

        WorldSettingDTO update = new WorldSettingDTO();
        update.setProjectId(current.getProjectId());
        update.setName(current.getName());
        update.setDescription(proposal.proposalSummary());
        update.setCategory(current.getCategory());

        WorldSettingVO updated = worldSettingService.updateWorldSetting(worldSettingId, update, userId);
        if (updated == null) {
            throw new IllegalStateException("世界观摘要写回失败");
        }

        return new SummaryApplyResult(
                true,
                currentSummary(proposal.targetRef()),
                proposal.targetRef(),
                List.of("首轮只写回世界观描述，不自动拆分标题、内容和分类")
        );
    }

    @Override
    protected String summaryFieldPath() {
        return "/description";
    }

    @Override
    protected String targetLabel() {
        return "世界观";
    }

    @Override
    protected List<String> riskNotes() {
        return List.of("首轮只写回世界观描述，不自动改标题、内容和分类");
    }

    @Override
    protected String createPreviewTitle(String summaryText) {
        String derived = super.createPreviewTitle(summaryText);
        return derived.length() > 14 ? derived.substring(0, 14) : derived;
    }
}
