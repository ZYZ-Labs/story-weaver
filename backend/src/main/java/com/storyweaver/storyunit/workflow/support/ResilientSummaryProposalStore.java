package com.storyweaver.storyunit.workflow.support;

import com.storyweaver.config.SummaryWorkflowProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.storyunit.summary.workflow.StructuredPatchProposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class ResilientSummaryProposalStore implements SummaryProposalStore {

    private static final Logger log = LoggerFactory.getLogger(ResilientSummaryProposalStore.class);
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SummaryWorkflowProperties properties;
    private final InMemorySummaryProposalStore fallbackStore;

    public ResilientSummaryProposalStore(
            ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider,
            ObjectMapper objectMapper,
            SummaryWorkflowProperties properties) {
        this.stringRedisTemplate = stringRedisTemplateProvider.getIfAvailable();
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.fallbackStore = new InMemorySummaryProposalStore();
    }

    @Override
    public StructuredPatchProposal save(StructuredPatchProposal proposal) {
        fallbackStore.save(proposal);
        if (!isRedisStoreEnabled()) {
            return proposal;
        }
        try {
            String payload = objectMapper.writeValueAsString(proposal);
            stringRedisTemplate.opsForValue().set(
                    buildKey(proposal.proposalId()),
                    payload,
                    Duration.ofMinutes(Math.max(1L, properties.getProposalTtlMinutes()))
            );
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize summary proposal {}", proposal.proposalId(), ex);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable when saving summary proposal {}", proposal.proposalId());
        } catch (RuntimeException ex) {
            log.warn("Failed to persist summary proposal {} to Redis", proposal.proposalId(), ex);
        }
        return proposal;
    }

    @Override
    public Optional<StructuredPatchProposal> find(String proposalId) {
        if (isRedisStoreEnabled()) {
            try {
                String payload = stringRedisTemplate.opsForValue().get(buildKey(proposalId));
                if (payload != null && !payload.isBlank()) {
                    StructuredPatchProposal proposal = objectMapper.readValue(payload, StructuredPatchProposal.class);
                    fallbackStore.save(proposal);
                    return Optional.of(proposal);
                }
            } catch (JsonProcessingException ex) {
                log.warn("Failed to deserialize summary proposal {}", proposalId, ex);
                removeRedisKey(proposalId);
            } catch (RedisConnectionFailureException ex) {
                log.warn("Redis unavailable when loading summary proposal {}", proposalId);
            } catch (RuntimeException ex) {
                log.warn("Failed to load summary proposal {} from Redis", proposalId, ex);
            }
        }
        return fallbackStore.find(proposalId);
    }

    @Override
    public void remove(String proposalId) {
        fallbackStore.remove(proposalId);
        removeRedisKey(proposalId);
    }

    private void removeRedisKey(String proposalId) {
        if (!isRedisStoreEnabled()) {
            return;
        }
        try {
            stringRedisTemplate.delete(buildKey(proposalId));
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable when deleting summary proposal {}", proposalId);
        } catch (RuntimeException ex) {
            log.warn("Failed to delete summary proposal {} from Redis", proposalId, ex);
        }
    }

    private boolean isRedisStoreEnabled() {
        return properties.isRedisProposalStoreEnabled() && stringRedisTemplate != null;
    }

    private String buildKey(String proposalId) {
        return properties.getProposalKeyPrefix() + proposalId;
    }
}
