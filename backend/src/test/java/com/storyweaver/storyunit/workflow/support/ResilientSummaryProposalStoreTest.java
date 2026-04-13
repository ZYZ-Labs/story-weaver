package com.storyweaver.storyunit.workflow.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.config.SummaryWorkflowProperties;
import com.storyweaver.storyunit.model.FacetType;
import com.storyweaver.storyunit.model.StorySourceTrace;
import com.storyweaver.storyunit.model.StoryUnitRef;
import com.storyweaver.storyunit.model.StoryUnitType;
import com.storyweaver.storyunit.patch.PatchOperation;
import com.storyweaver.storyunit.patch.PatchOperationType;
import com.storyweaver.storyunit.patch.PatchStatus;
import com.storyweaver.storyunit.patch.StoryPatch;
import com.storyweaver.storyunit.summary.workflow.StructuredPatchProposal;
import com.storyweaver.storyunit.summary.workflow.SummaryInputIntent;
import com.storyweaver.storyunit.summary.workflow.SummaryOperatorMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResilientSummaryProposalStoreTest {

    @Test
    void shouldFallbackToInMemoryWhenRedisTemplateMissing() {
        SummaryWorkflowProperties properties = new SummaryWorkflowProperties();
        properties.setRedisProposalStoreEnabled(true);
        ResilientSummaryProposalStore store = new ResilientSummaryProposalStore(
                new NullStringRedisTemplateProvider(),
                new ObjectMapper(),
                properties
        );

        StructuredPatchProposal proposal = buildProposal();
        store.save(proposal);

        assertTrue(store.find(proposal.proposalId()).isPresent());
        assertEquals(proposal.proposalId(), store.find(proposal.proposalId()).orElseThrow().proposalId());

        store.remove(proposal.proposalId());
        assertTrue(store.find(proposal.proposalId()).isEmpty());
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
                List.of(),
                List.of(),
                true
        );
    }

    private static class NullStringRedisTemplateProvider implements ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> {

        @Override
        public org.springframework.data.redis.core.StringRedisTemplate getObject(Object... args) {
            return null;
        }

        @Override
        public org.springframework.data.redis.core.StringRedisTemplate getIfAvailable() {
            return null;
        }

        @Override
        public org.springframework.data.redis.core.StringRedisTemplate getIfUnique() {
            return null;
        }

        @Override
        public org.springframework.data.redis.core.StringRedisTemplate getObject() {
            return null;
        }
    }
}
