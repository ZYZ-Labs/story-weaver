package com.storyweaver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "story.state-system")
public class StoryStateProperties {

    private boolean redisStoreEnabled = true;

    private long ttlMinutes = 720;

    private String eventKeyPrefix = "story:state:event:";

    private String chapterEventManifestKeyPrefix = "story:state:chapter-events:";

    private String snapshotKeyPrefix = "story:state:snapshot:";

    private String chapterSnapshotManifestKeyPrefix = "story:state:chapter-snapshots:";

    private String patchKeyPrefix = "story:state:patch:";

    private String chapterPatchManifestKeyPrefix = "story:state:chapter-patches:";

    private String chapterRevealStateKeyPrefix = "story:state:chapter-reveal:";

    private String chapterStateKeyPrefix = "story:state:chapter-state:";

    private String chapterSkeletonKeyPrefix = "story:state:chapter-skeleton:";

    public boolean isRedisStoreEnabled() {
        return redisStoreEnabled;
    }

    public void setRedisStoreEnabled(boolean redisStoreEnabled) {
        this.redisStoreEnabled = redisStoreEnabled;
    }

    public long getTtlMinutes() {
        return ttlMinutes;
    }

    public void setTtlMinutes(long ttlMinutes) {
        this.ttlMinutes = ttlMinutes;
    }

    public String getEventKeyPrefix() {
        return eventKeyPrefix;
    }

    public void setEventKeyPrefix(String eventKeyPrefix) {
        this.eventKeyPrefix = eventKeyPrefix;
    }

    public String getChapterEventManifestKeyPrefix() {
        return chapterEventManifestKeyPrefix;
    }

    public void setChapterEventManifestKeyPrefix(String chapterEventManifestKeyPrefix) {
        this.chapterEventManifestKeyPrefix = chapterEventManifestKeyPrefix;
    }

    public String getSnapshotKeyPrefix() {
        return snapshotKeyPrefix;
    }

    public void setSnapshotKeyPrefix(String snapshotKeyPrefix) {
        this.snapshotKeyPrefix = snapshotKeyPrefix;
    }

    public String getChapterSnapshotManifestKeyPrefix() {
        return chapterSnapshotManifestKeyPrefix;
    }

    public void setChapterSnapshotManifestKeyPrefix(String chapterSnapshotManifestKeyPrefix) {
        this.chapterSnapshotManifestKeyPrefix = chapterSnapshotManifestKeyPrefix;
    }

    public String getPatchKeyPrefix() {
        return patchKeyPrefix;
    }

    public void setPatchKeyPrefix(String patchKeyPrefix) {
        this.patchKeyPrefix = patchKeyPrefix;
    }

    public String getChapterPatchManifestKeyPrefix() {
        return chapterPatchManifestKeyPrefix;
    }

    public void setChapterPatchManifestKeyPrefix(String chapterPatchManifestKeyPrefix) {
        this.chapterPatchManifestKeyPrefix = chapterPatchManifestKeyPrefix;
    }

    public String getChapterRevealStateKeyPrefix() {
        return chapterRevealStateKeyPrefix;
    }

    public void setChapterRevealStateKeyPrefix(String chapterRevealStateKeyPrefix) {
        this.chapterRevealStateKeyPrefix = chapterRevealStateKeyPrefix;
    }

    public String getChapterStateKeyPrefix() {
        return chapterStateKeyPrefix;
    }

    public void setChapterStateKeyPrefix(String chapterStateKeyPrefix) {
        this.chapterStateKeyPrefix = chapterStateKeyPrefix;
    }

    public String getChapterSkeletonKeyPrefix() {
        return chapterSkeletonKeyPrefix;
    }

    public void setChapterSkeletonKeyPrefix(String chapterSkeletonKeyPrefix) {
        this.chapterSkeletonKeyPrefix = chapterSkeletonKeyPrefix;
    }
}
