package com.storyweaver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "story.summary-workflow")
public class SummaryWorkflowProperties {

    private boolean redisProposalStoreEnabled = true;

    private long proposalTtlMinutes = 30;

    private String proposalKeyPrefix = "story:summary-proposal:";

    private int conversationTimeoutSeconds = 12;

    private int conversationMaxTokens = 900;

    public boolean isRedisProposalStoreEnabled() {
        return redisProposalStoreEnabled;
    }

    public void setRedisProposalStoreEnabled(boolean redisProposalStoreEnabled) {
        this.redisProposalStoreEnabled = redisProposalStoreEnabled;
    }

    public long getProposalTtlMinutes() {
        return proposalTtlMinutes;
    }

    public void setProposalTtlMinutes(long proposalTtlMinutes) {
        this.proposalTtlMinutes = proposalTtlMinutes;
    }

    public String getProposalKeyPrefix() {
        return proposalKeyPrefix;
    }

    public void setProposalKeyPrefix(String proposalKeyPrefix) {
        this.proposalKeyPrefix = proposalKeyPrefix;
    }

    public int getConversationTimeoutSeconds() {
        return conversationTimeoutSeconds;
    }

    public void setConversationTimeoutSeconds(int conversationTimeoutSeconds) {
        this.conversationTimeoutSeconds = conversationTimeoutSeconds;
    }

    public int getConversationMaxTokens() {
        return conversationMaxTokens;
    }

    public void setConversationMaxTokens(int conversationMaxTokens) {
        this.conversationMaxTokens = conversationMaxTokens;
    }
}
