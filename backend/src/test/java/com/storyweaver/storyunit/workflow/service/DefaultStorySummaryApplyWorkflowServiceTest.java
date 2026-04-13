package com.storyweaver.storyunit.workflow.service;

import com.storyweaver.storyunit.facet.summary.DefaultSummaryFacet;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.patch.PatchOperation;
import com.storyweaver.storyunit.patch.PatchOperationType;
import com.storyweaver.storyunit.patch.PatchStatus;
import com.storyweaver.storyunit.patch.StoryPatch;
import com.storyweaver.storyunit.summary.workflow.StructuredPatchProposal;
import com.storyweaver.storyunit.summary.workflow.SummaryApplyCommand;
import com.storyweaver.storyunit.summary.workflow.SummaryApplyResult;
import com.storyweaver.storyunit.summary.workflow.SummaryInputIntent;
import com.storyweaver.storyunit.summary.workflow.SummaryOperatorMode;
import com.storyweaver.storyunit.workflow.exception.SummaryProposalConflictException;
import com.storyweaver.storyunit.workflow.handler.StorySummaryTargetHandler;
import com.storyweaver.storyunit.workflow.support.InMemorySummaryProposalStore;
import com.storyweaver.storyunit.workflow.support.SummaryProposalStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultStorySummaryApplyWorkflowServiceTest {

    @Test
    void shouldApplyAndRemoveStoredProposal() {
        StorySummaryTargetHandler handler = mock(StorySummaryTargetHandler.class);
        when(handler.targetType()).thenReturn(StoryUnitType.CHARACTER);

        SummaryProposalStore proposalStore = new InMemorySummaryProposalStore();
        StructuredPatchProposal proposal = buildProposal();
        proposalStore.save(proposal);

        SummaryApplyResult expected = new SummaryApplyResult(
                true,
                new DefaultSummaryFacet("人物", "一行摘要", "长摘要", "", "", "", List.of()),
                proposal.targetRef(),
                List.of("ok")
        );
        when(handler.apply(eq(100L), eq(commandFor(proposal)), eq(proposal))).thenReturn(expected);

        DefaultStorySummaryApplyWorkflowService service = new DefaultStorySummaryApplyWorkflowService(
                List.of(handler),
                proposalStore
        );

        SummaryApplyResult result = service.apply(100L, commandFor(proposal));

        assertTrue(result.applied());
        assertEquals(proposal.targetRef(), result.updatedUnitRef());
        assertTrue(proposalStore.find(proposal.proposalId()).isEmpty());
        verify(handler).apply(eq(100L), eq(commandFor(proposal)), eq(proposal));
    }

    @Test
    void shouldRejectMismatchedTargetRef() {
        StorySummaryTargetHandler handler = mock(StorySummaryTargetHandler.class);
        when(handler.targetType()).thenReturn(StoryUnitType.CHARACTER);

        SummaryProposalStore proposalStore = new InMemorySummaryProposalStore();
        StructuredPatchProposal proposal = buildProposal();
        proposalStore.save(proposal);

        DefaultStorySummaryApplyWorkflowService service = new DefaultStorySummaryApplyWorkflowService(
                List.of(handler),
                proposalStore
        );

        SummaryApplyCommand mismatched = new SummaryApplyCommand(
                proposal.proposalId(),
                new StoryUnitRef("999", "character:999", StoryUnitType.CHARACTER),
                true,
                100L
        );

        SummaryProposalConflictException error = assertThrows(
                SummaryProposalConflictException.class,
                () -> service.apply(100L, mismatched)
        );

        assertEquals("proposal 目标与当前操作对象不匹配", error.getMessage());
    }

    private static SummaryApplyCommand commandFor(StructuredPatchProposal proposal) {
        return new SummaryApplyCommand(proposal.proposalId(), proposal.targetRef(), true, 100L);
    }

    private static StructuredPatchProposal buildProposal() {
        StoryUnitRef targetRef = new StoryUnitRef("12", "character:12", StoryUnitType.CHARACTER);
        StoryPatch patch = new StoryPatch(
                "patch-1",
                targetRef,
                FacetType.CANON,
                List.of(new PatchOperation(PatchOperationType.REPLACE, "/description", "新的摘要")),
                "将按摘要更新人物：新的摘要",
                PatchStatus.PENDING_CONFIRMATION,
                new StorySourceTrace("summary-first", "summary-first", "SummaryInputDraft", "12")
        );
        return new StructuredPatchProposal(
                "proposal-1",
                targetRef,
                28L,
                patch,
                SummaryInputIntent.REFINE,
                SummaryOperatorMode.EXPERT,
                "新的摘要",
                "新的摘要",
                List.of(FacetType.CANON, FacetType.SUMMARY),
                List.of(),
                List.of(),
                true
        );
    }
}
