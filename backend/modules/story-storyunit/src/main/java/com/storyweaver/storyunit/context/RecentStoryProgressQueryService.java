package com.storyweaver.storyunit.context;

public interface RecentStoryProgressQueryService {

    RecentStoryProgressView getRecentStoryProgress(Long projectId, int limit);
}
