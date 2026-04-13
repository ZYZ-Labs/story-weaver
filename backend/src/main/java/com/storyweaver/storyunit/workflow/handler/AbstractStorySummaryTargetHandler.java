package com.storyweaver.storyunit.workflow.handler;

import com.storyweaver.storyunit.facet.summary.DefaultSummaryFacet;
import com.storyweaver.storyunit.facet.summary.SummaryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.patch.PatchOperation;
import com.storyweaver.storyunit.patch.PatchOperationType;
import com.storyweaver.storyunit.patch.PatchStatus;
import com.storyweaver.storyunit.patch.StoryPatch;
import com.storyweaver.storyunit.service.StoryUnitQueryService;
import com.storyweaver.storyunit.summary.StorySummaryDraft;
import com.storyweaver.storyunit.summary.StorySummaryService;
import com.storyweaver.storyunit.summary.workflow.StructuredPatchProposal;
import com.storyweaver.storyunit.summary.workflow.SummaryChangePreview;
import com.storyweaver.storyunit.summary.workflow.SummaryInputDraft;
import com.storyweaver.storyunit.summary.workflow.SummaryInputIntent;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public abstract class AbstractStorySummaryTargetHandler implements StorySummaryTargetHandler {

    private final StoryUnitQueryService storyUnitQueryService;

    private final StorySummaryService storySummaryService;

    protected AbstractStorySummaryTargetHandler(
            StoryUnitQueryService storyUnitQueryService,
            StorySummaryService storySummaryService) {
        this.storyUnitQueryService = storyUnitQueryService;
        this.storySummaryService = storySummaryService;
    }

    @Override
    public StructuredPatchProposal propose(Long userId, SummaryInputDraft inputDraft) {
        boolean createIntent = inputDraft.intent() == SummaryInputIntent.CREATE || inputDraft.targetSourceId() == null;
        String summaryText = inputDraft.summaryText().trim();
        String proposalId = UUID.randomUUID().toString();
        Long targetSourceId = createIntent ? null : requireTargetSourceId(inputDraft);
        StoryUnitRef targetRef = createIntent
                ? toCreationTargetRef(proposalId)
                : toTargetRef(targetSourceId);

        StoryPatch patch = new StoryPatch(
                proposalId + ":patch",
                targetRef,
                FacetType.CANON,
                List.of(new PatchOperation(createIntent ? PatchOperationType.ADD : PatchOperationType.REPLACE, summaryFieldPath(), summaryText)),
                patchSummaryText(summaryText, createIntent),
                PatchStatus.PENDING_CONFIRMATION,
                new StorySourceTrace(
                        "summary-first",
                        "summary-first",
                        "SummaryInputDraft",
                        createIntent ? "create:" + targetType().name().toLowerCase(Locale.ROOT) : String.valueOf(targetSourceId)
                )
        );

        return new StructuredPatchProposal(
                proposalId,
                targetRef,
                inputDraft.projectId(),
                patch,
                createIntent ? SummaryInputIntent.CREATE : inputDraft.intent(),
                inputDraft.operatorMode(),
                summaryText,
                summaryText,
                List.of(FacetType.CANON, FacetType.SUMMARY),
                riskNotes(),
                pendingQuestions(),
                true
        );
    }

    @Override
    public SummaryChangePreview preview(Long userId, StructuredPatchProposal proposal) {
        SummaryFacet current = isCreateProposal(proposal)
                ? emptyCreateSummary(proposal)
                : currentSummary(proposal.targetRef());
        SummaryFacet after = storySummaryService.summarize(new StorySummaryDraft(
                targetType(),
                proposalDisplayTitle(proposal),
                List.of(proposal.proposalSummary(), current.oneLineSummary(), current.displayTitle()),
                List.of(proposal.proposalSummary(), current.longSummary(), current.oneLineSummary()),
                splitFacts(current.stateSummary()),
                splitFacts(current.relationSummary()),
                List.of(isCreateProposal(proposal) ? "待确认创建" : "待确认写回"),
                proposal.pendingQuestions()
        ));

        return new SummaryChangePreview(
                current,
                after,
                changeSummaryText(proposal),
                proposal.affectedFacets(),
                proposal.requiresConfirmation(),
                proposal.riskNotes()
        );
    }

    protected SummaryFacet currentSummary(StoryUnitRef targetRef) {
        return storyUnitQueryService.getSummaryFacet(targetRef)
                .orElseGet(() -> new DefaultSummaryFacet(
                        targetRef.unitKey(),
                        "",
                        "",
                        "",
                        "",
                        "",
                        List.of()
                ));
    }

    protected StoryUnitRef toTargetRef(Long targetSourceId) {
        String unitId = String.valueOf(targetSourceId);
        String unitKey = targetType().name().toLowerCase(Locale.ROOT) + ":" + unitId;
        return new StoryUnitRef(unitId, unitKey, targetType());
    }

    protected StoryUnitRef toCreationTargetRef(String proposalId) {
        String unitId = "draft:" + proposalId;
        String unitKey = targetType().name().toLowerCase(Locale.ROOT) + ":" + unitId;
        return new StoryUnitRef(unitId, unitKey, targetType());
    }

    protected Long requireTargetSourceId(SummaryInputDraft inputDraft) {
        if (inputDraft.targetSourceId() == null) {
            throw new IllegalArgumentException("targetSourceId 不能为空");
        }
        return inputDraft.targetSourceId();
    }

    protected List<String> splitFacts(String value) {
        if (value == null || value.trim().isEmpty()) {
            return List.of();
        }
        return List.of(value.split("；")).stream()
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    protected boolean isCreateProposal(StructuredPatchProposal proposal) {
        return proposal.inputIntent() == SummaryInputIntent.CREATE || proposal.targetRef().unitId().startsWith("draft:");
    }

    protected SummaryFacet emptyCreateSummary(StructuredPatchProposal proposal) {
        return new DefaultSummaryFacet(
                createPreviewTitle(proposal.proposalSummary()),
                "",
                "",
                "",
                "",
                "",
                proposal.pendingQuestions()
        );
    }

    protected String patchSummaryText(String summaryText, boolean createIntent) {
        return (createIntent ? "将按摘要创建" : "将按摘要更新") + targetLabel() + "：" + summaryText;
    }

    protected String proposalDisplayTitle(StructuredPatchProposal proposal) {
        return isCreateProposal(proposal) ? createPreviewTitle(proposal.proposalSummary()) : currentSummary(proposal.targetRef()).displayTitle();
    }

    protected String changeSummaryText(StructuredPatchProposal proposal) {
        if (isCreateProposal(proposal)) {
            return "将创建" + targetLabel() + "并生成首轮 SummaryFacet";
        }
        return "将写回" + targetLabel() + "的摘要字段，并刷新 SummaryFacet";
    }

    protected String createPreviewTitle(String summaryText) {
        String normalized = summaryText == null ? "" : summaryText.trim();
        if (normalized.isEmpty()) {
            return "待创建" + targetLabel();
        }
        int endIndex = normalized.length();
        String punctuation = "，。；：,:!?！？\n";
        for (int i = 0; i < normalized.length(); i++) {
            if (punctuation.indexOf(normalized.charAt(i)) >= 0) {
                endIndex = i;
                break;
            }
        }
        String candidate = normalized.substring(0, Math.min(endIndex, 16)).trim();
        return candidate.isEmpty() ? "待创建" + targetLabel() : candidate;
    }

    protected abstract String summaryFieldPath();

    protected abstract String targetLabel();

    protected abstract List<String> riskNotes();

    protected List<String> pendingQuestions() {
        return List.of();
    }
}
