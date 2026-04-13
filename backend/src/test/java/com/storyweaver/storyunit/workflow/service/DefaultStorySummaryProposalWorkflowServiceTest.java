package com.storyweaver.storyunit.workflow.service;

import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.patch.PatchOperation;
import com.storyweaver.storyunit.patch.PatchOperationType;
import com.storyweaver.storyunit.patch.PatchStatus;
import com.storyweaver.storyunit.patch.StoryPatch;
import com.storyweaver.storyunit.summary.workflow.StructuredPatchProposal;
import com.storyweaver.storyunit.summary.workflow.SummaryInputDraft;
import com.storyweaver.storyunit.summary.workflow.SummaryInputIntent;
import com.storyweaver.storyunit.summary.workflow.SummaryOperatorMode;
import com.storyweaver.storyunit.workflow.handler.StorySummaryTargetHandler;
import com.storyweaver.storyunit.workflow.support.InMemorySummaryProposalStore;
import com.storyweaver.storyunit.workflow.support.SummaryProposalStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultStorySummaryProposalWorkflowServiceTest {

    @Test
    void shouldDelegateToMatchingHandlerAndPersistProposal() {
        StorySummaryTargetHandler handler = mock(StorySummaryTargetHandler.class);
        when(handler.targetType()).thenReturn(StoryUnitType.CHAPTER);

        SummaryInputDraft inputDraft = new SummaryInputDraft(
                StoryUnitType.CHAPTER,
                31L,
                28L,
                "补一段新的章节摘要",
                SummaryInputIntent.REFINE,
                SummaryOperatorMode.EXPERT
        );
        StructuredPatchProposal proposal = buildProposal();
        when(handler.propose(eq(200L), eq(inputDraft))).thenReturn(proposal);

        SummaryProposalStore proposalStore = new InMemorySummaryProposalStore();
        DefaultStorySummaryProposalWorkflowService service = new DefaultStorySummaryProposalWorkflowService(
                List.of(handler),
                proposalStore
        );

        StructuredPatchProposal result = service.propose(200L, inputDraft);

        assertEquals(proposal.proposalId(), result.proposalId());
        assertEquals(28L, result.projectId());
        assertTrue(proposalStore.find(proposal.proposalId()).isPresent());
        verify(handler).propose(eq(200L), eq(inputDraft));
    }

    private static StructuredPatchProposal buildProposal() {
        StoryUnitRef targetRef = new StoryUnitRef("31", "chapter:31", StoryUnitType.CHAPTER);
        StoryPatch patch = new StoryPatch(
                "patch-31",
                targetRef,
                FacetType.CANON,
                List.of(new PatchOperation(PatchOperationType.REPLACE, "/summary", "补一段新的章节摘要")),
                "将按摘要更新章节：补一段新的章节摘要",
                PatchStatus.PENDING_CONFIRMATION,
                new StorySourceTrace("summary-first", "summary-first", "SummaryInputDraft", "31")
        );
        return new StructuredPatchProposal(
                "proposal-31",
                targetRef,
                28L,
                patch,
                SummaryInputIntent.REFINE,
                SummaryOperatorMode.EXPERT,
                "补一段新的章节摘要",
                "补一段新的章节摘要",
                List.of(FacetType.CANON, FacetType.SUMMARY),
                List.of("首轮只写回章节摘要"),
                List.of(),
                true
        );
    }
}
