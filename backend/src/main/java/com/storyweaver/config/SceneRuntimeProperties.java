package com.storyweaver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "story.scene-runtime")
public class SceneRuntimeProperties {

    private boolean redisStoreEnabled = true;

    private long ttlMinutes = 180;

    private String sceneStateKeyPrefix = "story:scene-runtime:state:";

    private String handoffKeyPrefix = "story:scene-runtime:handoff:";

    private String chapterManifestKeyPrefix = "story:scene-runtime:chapter-scenes:";

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

    public String getSceneStateKeyPrefix() {
        return sceneStateKeyPrefix;
    }

    public void setSceneStateKeyPrefix(String sceneStateKeyPrefix) {
        this.sceneStateKeyPrefix = sceneStateKeyPrefix;
    }

    public String getHandoffKeyPrefix() {
        return handoffKeyPrefix;
    }

    public void setHandoffKeyPrefix(String handoffKeyPrefix) {
        this.handoffKeyPrefix = handoffKeyPrefix;
    }

    public String getChapterManifestKeyPrefix() {
        return chapterManifestKeyPrefix;
    }

    public void setChapterManifestKeyPrefix(String chapterManifestKeyPrefix) {
        this.chapterManifestKeyPrefix = chapterManifestKeyPrefix;
    }
}
