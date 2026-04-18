package com.storyweaver.storyunit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.config.SceneRuntimeProperties;
import com.storyweaver.storyunit.service.SceneRuntimeStateStore;
import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneHandoffSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ResilientSceneRuntimeStateStore implements SceneRuntimeStateStore {

    private static final Logger log = LoggerFactory.getLogger(ResilientSceneRuntimeStateStore.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SceneRuntimeProperties properties;
    private final InMemorySceneRuntimeStateStore fallbackStore;

    public ResilientSceneRuntimeStateStore(
            ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider,
            ObjectMapper objectMapper,
            SceneRuntimeProperties properties) {
        this.stringRedisTemplate = stringRedisTemplateProvider.getIfAvailable();
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.fallbackStore = new InMemorySceneRuntimeStateStore();
    }

    @Override
    public Optional<SceneExecutionState> getSceneState(Long projectId, Long chapterId, String sceneId) {
        if (!StringUtils.hasText(sceneId)) {
            return Optional.empty();
        }
        if (isRedisStoreEnabled()) {
            try {
                String payload = stringRedisTemplate.opsForValue().get(buildSceneStateKey(projectId, chapterId, sceneId));
                if (StringUtils.hasText(payload)) {
                    SceneExecutionState state = objectMapper.readValue(payload, SceneExecutionState.class);
                    fallbackStore.saveSceneState(state);
                    return Optional.of(state);
                }
            } catch (JsonProcessingException ex) {
                log.warn("Failed to deserialize scene runtime state {}/{}/{}", projectId, chapterId, sceneId, ex);
                removeSceneStateKey(projectId, chapterId, sceneId);
            } catch (RedisConnectionFailureException ex) {
                log.warn("Redis unavailable when loading scene runtime state {}/{}/{}", projectId, chapterId, sceneId);
            } catch (RuntimeException ex) {
                log.warn("Failed to load scene runtime state {}/{}/{}", projectId, chapterId, sceneId, ex);
            }
        }
        return fallbackStore.getSceneState(projectId, chapterId, sceneId);
    }

    @Override
    public List<SceneExecutionState> listChapterScenes(Long projectId, Long chapterId) {
        if (isRedisStoreEnabled()) {
            try {
                List<String> sceneIds = readSceneManifest(projectId, chapterId);
                if (!sceneIds.isEmpty()) {
                    List<SceneExecutionState> states = new ArrayList<>();
                    for (String sceneId : sceneIds) {
                        getSceneState(projectId, chapterId, sceneId).ifPresent(states::add);
                    }
                    if (!states.isEmpty()) {
                        return states.stream()
                                .sorted(Comparator.comparingInt(SceneExecutionState::sceneIndex).thenComparing(SceneExecutionState::sceneId))
                                .toList();
                    }
                }
            } catch (JsonProcessingException ex) {
                log.warn("Failed to deserialize scene manifest {}/{}", projectId, chapterId, ex);
                removeSceneManifestKey(projectId, chapterId);
            } catch (RedisConnectionFailureException ex) {
                log.warn("Redis unavailable when listing scene runtime states {}/{}", projectId, chapterId);
            } catch (RuntimeException ex) {
                log.warn("Failed to list scene runtime states {}/{}", projectId, chapterId, ex);
            }
        }
        return fallbackStore.listChapterScenes(projectId, chapterId);
    }

    @Override
    public SceneExecutionState saveSceneState(SceneExecutionState sceneExecutionState) {
        fallbackStore.saveSceneState(sceneExecutionState);
        if (!isRedisStoreEnabled()) {
            return sceneExecutionState;
        }
        try {
            String payload = objectMapper.writeValueAsString(sceneExecutionState);
            stringRedisTemplate.opsForValue().set(
                    buildSceneStateKey(sceneExecutionState.projectId(), sceneExecutionState.chapterId(), sceneExecutionState.sceneId()),
                    payload,
                    Duration.ofMinutes(Math.max(1L, properties.getTtlMinutes()))
            );
            writeSceneManifest(sceneExecutionState.projectId(), sceneExecutionState.chapterId(), sceneExecutionState.sceneId());
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize scene runtime state {}/{}/{}",
                    sceneExecutionState.projectId(), sceneExecutionState.chapterId(), sceneExecutionState.sceneId(), ex);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable when saving scene runtime state {}/{}/{}",
                    sceneExecutionState.projectId(), sceneExecutionState.chapterId(), sceneExecutionState.sceneId());
        } catch (RuntimeException ex) {
            log.warn("Failed to persist scene runtime state {}/{}/{}",
                    sceneExecutionState.projectId(), sceneExecutionState.chapterId(), sceneExecutionState.sceneId(), ex);
        }
        return sceneExecutionState;
    }

    @Override
    public Optional<SceneHandoffSnapshot> findHandoffToScene(Long projectId, Long chapterId, String sceneId) {
        if (!StringUtils.hasText(sceneId)) {
            return Optional.empty();
        }
        if (isRedisStoreEnabled()) {
            try {
                String payload = stringRedisTemplate.opsForValue().get(buildHandoffKey(projectId, chapterId, sceneId));
                if (StringUtils.hasText(payload)) {
                    SceneHandoffSnapshot snapshot = objectMapper.readValue(payload, SceneHandoffSnapshot.class);
                    fallbackStore.saveHandoff(snapshot);
                    return Optional.of(snapshot);
                }
            } catch (JsonProcessingException ex) {
                log.warn("Failed to deserialize scene handoff {}/{}/{}", projectId, chapterId, sceneId, ex);
                removeHandoffKey(projectId, chapterId, sceneId);
            } catch (RedisConnectionFailureException ex) {
                log.warn("Redis unavailable when loading scene handoff {}/{}/{}", projectId, chapterId, sceneId);
            } catch (RuntimeException ex) {
                log.warn("Failed to load scene handoff {}/{}/{}", projectId, chapterId, sceneId, ex);
            }
        }
        return fallbackStore.findHandoffToScene(projectId, chapterId, sceneId);
    }

    @Override
    public SceneHandoffSnapshot saveHandoff(SceneHandoffSnapshot snapshot) {
        fallbackStore.saveHandoff(snapshot);
        if (!isRedisStoreEnabled()) {
            return snapshot;
        }
        try {
            String payload = objectMapper.writeValueAsString(snapshot);
            stringRedisTemplate.opsForValue().set(
                    buildHandoffKey(snapshot.projectId(), snapshot.chapterId(), snapshot.toSceneId()),
                    payload,
                    Duration.ofMinutes(Math.max(1L, properties.getTtlMinutes()))
            );
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize scene handoff {}/{}/{}",
                    snapshot.projectId(), snapshot.chapterId(), snapshot.toSceneId(), ex);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable when saving scene handoff {}/{}/{}",
                    snapshot.projectId(), snapshot.chapterId(), snapshot.toSceneId());
        } catch (RuntimeException ex) {
            log.warn("Failed to persist scene handoff {}/{}/{}",
                    snapshot.projectId(), snapshot.chapterId(), snapshot.toSceneId(), ex);
        }
        return snapshot;
    }

    private boolean isRedisStoreEnabled() {
        return properties.isRedisStoreEnabled() && stringRedisTemplate != null;
    }

    private String buildSceneStateKey(Long projectId, Long chapterId, String sceneId) {
        return properties.getSceneStateKeyPrefix() + projectId + ":" + chapterId + ":" + sceneId.trim();
    }

    private String buildHandoffKey(Long projectId, Long chapterId, String sceneId) {
        return properties.getHandoffKeyPrefix() + projectId + ":" + chapterId + ":" + sceneId.trim();
    }

    private String buildChapterManifestKey(Long projectId, Long chapterId) {
        return properties.getChapterManifestKeyPrefix() + projectId + ":" + chapterId;
    }

    private void writeSceneManifest(Long projectId, Long chapterId, String sceneId) throws JsonProcessingException {
        List<String> current = readSceneManifest(projectId, chapterId);
        if (!current.contains(sceneId)) {
            current = new ArrayList<>(current);
            current.add(sceneId);
            stringRedisTemplate.opsForValue().set(
                    buildChapterManifestKey(projectId, chapterId),
                    objectMapper.writeValueAsString(current),
                    Duration.ofMinutes(Math.max(1L, properties.getTtlMinutes()))
            );
        }
    }

    private List<String> readSceneManifest(Long projectId, Long chapterId) throws JsonProcessingException {
        String payload = stringRedisTemplate.opsForValue().get(buildChapterManifestKey(projectId, chapterId));
        if (!StringUtils.hasText(payload)) {
            return List.of();
        }
        List<String> sceneIds = objectMapper.readValue(payload, new TypeReference<List<String>>() {});
        return sceneIds == null ? List.of() : sceneIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private void removeSceneStateKey(Long projectId, Long chapterId, String sceneId) {
        if (!isRedisStoreEnabled()) {
            return;
        }
        try {
            stringRedisTemplate.delete(buildSceneStateKey(projectId, chapterId, sceneId));
        } catch (RuntimeException ex) {
            log.warn("Failed to delete invalid scene runtime state {}/{}/{}", projectId, chapterId, sceneId, ex);
        }
    }

    private void removeHandoffKey(Long projectId, Long chapterId, String sceneId) {
        if (!isRedisStoreEnabled()) {
            return;
        }
        try {
            stringRedisTemplate.delete(buildHandoffKey(projectId, chapterId, sceneId));
        } catch (RuntimeException ex) {
            log.warn("Failed to delete invalid scene handoff {}/{}/{}", projectId, chapterId, sceneId, ex);
        }
    }

    private void removeSceneManifestKey(Long projectId, Long chapterId) {
        if (!isRedisStoreEnabled()) {
            return;
        }
        try {
            stringRedisTemplate.delete(buildChapterManifestKey(projectId, chapterId));
        } catch (RuntimeException ex) {
            log.warn("Failed to delete invalid scene manifest {}/{}", projectId, chapterId, ex);
        }
    }

    private static final class InMemorySceneRuntimeStateStore implements SceneRuntimeStateStore {

        private final Map<String, SceneExecutionState> sceneStateMap = new LinkedHashMap<>();
        private final Map<String, SceneHandoffSnapshot> handoffMap = new LinkedHashMap<>();

        @Override
        public Optional<SceneExecutionState> getSceneState(Long projectId, Long chapterId, String sceneId) {
            return Optional.ofNullable(sceneStateMap.get(buildSceneKey(projectId, chapterId, sceneId)));
        }

        @Override
        public List<SceneExecutionState> listChapterScenes(Long projectId, Long chapterId) {
            return sceneStateMap.values().stream()
                    .filter(state -> projectId.equals(state.projectId()) && chapterId.equals(state.chapterId()))
                    .sorted(Comparator.comparingInt(SceneExecutionState::sceneIndex).thenComparing(SceneExecutionState::sceneId))
                    .toList();
        }

        @Override
        public SceneExecutionState saveSceneState(SceneExecutionState sceneExecutionState) {
            sceneStateMap.put(
                    buildSceneKey(sceneExecutionState.projectId(), sceneExecutionState.chapterId(), sceneExecutionState.sceneId()),
                    sceneExecutionState
            );
            return sceneExecutionState;
        }

        @Override
        public Optional<SceneHandoffSnapshot> findHandoffToScene(Long projectId, Long chapterId, String sceneId) {
            return Optional.ofNullable(handoffMap.get(buildSceneKey(projectId, chapterId, sceneId)));
        }

        @Override
        public SceneHandoffSnapshot saveHandoff(SceneHandoffSnapshot snapshot) {
            handoffMap.put(buildSceneKey(snapshot.projectId(), snapshot.chapterId(), snapshot.toSceneId()), snapshot);
            return snapshot;
        }

        private static String buildSceneKey(Long projectId, Long chapterId, String sceneId) {
            return projectId + ":" + chapterId + ":" + sceneId;
        }
    }
}
