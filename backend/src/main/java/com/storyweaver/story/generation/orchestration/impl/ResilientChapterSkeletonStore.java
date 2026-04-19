package com.storyweaver.story.generation.orchestration.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.config.StoryStateProperties;
import com.storyweaver.story.generation.orchestration.ChapterSkeleton;
import com.storyweaver.story.generation.orchestration.ChapterSkeletonStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ResilientChapterSkeletonStore implements ChapterSkeletonStore {

    private static final Logger log = LoggerFactory.getLogger(ResilientChapterSkeletonStore.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final StoryStateProperties properties;
    private final Map<String, ChapterSkeleton> fallbackStore = new LinkedHashMap<>();

    public ResilientChapterSkeletonStore(
            ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider,
            ObjectMapper objectMapper,
            StoryStateProperties properties) {
        this.stringRedisTemplate = stringRedisTemplateProvider.getIfAvailable();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public Optional<ChapterSkeleton> find(Long projectId, Long chapterId) {
        String key = buildKey(projectId, chapterId);
        if (isRedisStoreEnabled()) {
            try {
                String payload = stringRedisTemplate.opsForValue().get(key);
                if (StringUtils.hasText(payload)) {
                    ChapterSkeleton chapterSkeleton = objectMapper.readValue(payload, ChapterSkeleton.class);
                    fallbackStore.put(key, chapterSkeleton);
                    return Optional.of(chapterSkeleton);
                }
            } catch (JsonProcessingException ex) {
                log.warn("Failed to deserialize chapter skeleton {}/{}", projectId, chapterId, ex);
            } catch (RedisConnectionFailureException ex) {
                log.warn("Redis unavailable when reading chapter skeleton {}/{}", projectId, chapterId);
            } catch (RuntimeException ex) {
                log.warn("Failed to read chapter skeleton {}/{}", projectId, chapterId, ex);
            }
        }
        return Optional.ofNullable(fallbackStore.get(key));
    }

    @Override
    public ChapterSkeleton save(ChapterSkeleton chapterSkeleton) {
        String key = buildKey(chapterSkeleton.projectId(), chapterSkeleton.chapterId());
        fallbackStore.put(key, chapterSkeleton);
        if (!isRedisStoreEnabled()) {
            return chapterSkeleton;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(chapterSkeleton),
                    Duration.ofMinutes(Math.max(1L, properties.getTtlMinutes()))
            );
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize chapter skeleton {}/{}", chapterSkeleton.projectId(), chapterSkeleton.chapterId(), ex);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable when saving chapter skeleton {}/{}", chapterSkeleton.projectId(), chapterSkeleton.chapterId());
        } catch (RuntimeException ex) {
            log.warn("Failed to save chapter skeleton {}/{}", chapterSkeleton.projectId(), chapterSkeleton.chapterId(), ex);
        }
        return chapterSkeleton;
    }

    private boolean isRedisStoreEnabled() {
        return properties.isRedisStoreEnabled() && stringRedisTemplate != null;
    }

    private String buildKey(Long projectId, Long chapterId) {
        return properties.getChapterSkeletonKeyPrefix() + projectId + ":" + chapterId;
    }
}
