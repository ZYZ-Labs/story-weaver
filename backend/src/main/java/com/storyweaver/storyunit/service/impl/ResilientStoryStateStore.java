package com.storyweaver.storyunit.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.config.StoryStateProperties;
import com.storyweaver.storyunit.facet.reveal.ReaderRevealState;
import com.storyweaver.storyunit.event.StoryEvent;
import com.storyweaver.storyunit.patch.StoryPatch;
import com.storyweaver.storyunit.service.ReaderRevealStateStore;
import com.storyweaver.storyunit.service.StoryEventStore;
import com.storyweaver.storyunit.service.StoryPatchStore;
import com.storyweaver.storyunit.service.StorySnapshotStore;
import com.storyweaver.storyunit.snapshot.StorySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ResilientStoryStateStore implements StoryEventStore, StorySnapshotStore, StoryPatchStore, ReaderRevealStateStore {

    private static final Logger log = LoggerFactory.getLogger(ResilientStoryStateStore.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final StoryStateProperties properties;
    private final InMemoryStoryStateStore fallbackStore;

    public ResilientStoryStateStore(
            ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider,
            ObjectMapper objectMapper,
            StoryStateProperties properties) {
        this.stringRedisTemplate = stringRedisTemplateProvider.getIfAvailable();
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.fallbackStore = new InMemoryStoryStateStore();
    }

    @Override
    public StoryEvent appendEvent(StoryEvent event) {
        fallbackStore.appendEvent(event);
        if (!isRedisStoreEnabled()) {
            return event;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    buildEventKey(event.eventId()),
                    objectMapper.writeValueAsString(event),
                    Duration.ofMinutes(Math.max(1L, properties.getTtlMinutes()))
            );
            writeManifest(buildChapterEventManifestKey(event.projectId(), event.chapterId()), event.eventId());
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize story event {}", event.eventId(), ex);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable when appending story event {}", event.eventId());
        } catch (RuntimeException ex) {
            log.warn("Failed to append story event {}", event.eventId(), ex);
        }
        return event;
    }

    @Override
    public List<StoryEvent> listChapterEvents(Long projectId, Long chapterId) {
        if (isRedisStoreEnabled()) {
            try {
                List<String> eventIds = readManifest(buildChapterEventManifestKey(projectId, chapterId));
                if (!eventIds.isEmpty()) {
                    List<StoryEvent> events = new ArrayList<>();
                    for (String eventId : eventIds) {
                        String payload = stringRedisTemplate.opsForValue().get(buildEventKey(eventId));
                        if (!StringUtils.hasText(payload)) {
                            continue;
                        }
                        StoryEvent event = objectMapper.readValue(payload, StoryEvent.class);
                        fallbackStore.appendEvent(event);
                        events.add(event);
                    }
                    if (!events.isEmpty()) {
                        return List.copyOf(events);
                    }
                }
            } catch (JsonProcessingException ex) {
                log.warn("Failed to deserialize story events for chapter {}/{}", projectId, chapterId, ex);
            } catch (RedisConnectionFailureException ex) {
                log.warn("Redis unavailable when listing story events for chapter {}/{}", projectId, chapterId);
            } catch (RuntimeException ex) {
                log.warn("Failed to list story events for chapter {}/{}", projectId, chapterId, ex);
            }
        }
        return fallbackStore.listChapterEvents(projectId, chapterId);
    }

    @Override
    public StorySnapshot saveSnapshot(StorySnapshot snapshot) {
        fallbackStore.saveSnapshot(snapshot);
        if (!isRedisStoreEnabled()) {
            return snapshot;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    buildSnapshotKey(snapshot.snapshotId()),
                    objectMapper.writeValueAsString(snapshot),
                    Duration.ofMinutes(Math.max(1L, properties.getTtlMinutes()))
            );
            writeManifest(buildChapterSnapshotManifestKey(snapshot.projectId(), snapshot.chapterId()), snapshot.snapshotId());
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize story snapshot {}", snapshot.snapshotId(), ex);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable when saving story snapshot {}", snapshot.snapshotId());
        } catch (RuntimeException ex) {
            log.warn("Failed to save story snapshot {}", snapshot.snapshotId(), ex);
        }
        return snapshot;
    }

    @Override
    public List<StorySnapshot> listChapterSnapshots(Long projectId, Long chapterId) {
        if (isRedisStoreEnabled()) {
            try {
                List<String> snapshotIds = readManifest(buildChapterSnapshotManifestKey(projectId, chapterId));
                if (!snapshotIds.isEmpty()) {
                    List<StorySnapshot> snapshots = new ArrayList<>();
                    for (String snapshotId : snapshotIds) {
                        String payload = stringRedisTemplate.opsForValue().get(buildSnapshotKey(snapshotId));
                        if (!StringUtils.hasText(payload)) {
                            continue;
                        }
                        StorySnapshot snapshot = objectMapper.readValue(payload, StorySnapshot.class);
                        fallbackStore.saveSnapshot(snapshot);
                        snapshots.add(snapshot);
                    }
                    if (!snapshots.isEmpty()) {
                        return List.copyOf(snapshots);
                    }
                }
            } catch (JsonProcessingException ex) {
                log.warn("Failed to deserialize story snapshots for chapter {}/{}", projectId, chapterId, ex);
            } catch (RedisConnectionFailureException ex) {
                log.warn("Redis unavailable when listing story snapshots for chapter {}/{}", projectId, chapterId);
            } catch (RuntimeException ex) {
                log.warn("Failed to list story snapshots for chapter {}/{}", projectId, chapterId, ex);
            }
        }
        return fallbackStore.listChapterSnapshots(projectId, chapterId);
    }

    @Override
    public StoryPatch appendPatch(Long projectId, Long chapterId, StoryPatch patch) {
        fallbackStore.appendPatch(projectId, chapterId, patch);
        if (!isRedisStoreEnabled()) {
            return patch;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    buildPatchKey(patch.patchId()),
                    objectMapper.writeValueAsString(patch),
                    Duration.ofMinutes(Math.max(1L, properties.getTtlMinutes()))
            );
            writeManifest(buildChapterPatchManifestKey(projectId, chapterId), patch.patchId());
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize story patch {}", patch.patchId(), ex);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable when appending story patch {}", patch.patchId());
        } catch (RuntimeException ex) {
            log.warn("Failed to append story patch {}", patch.patchId(), ex);
        }
        return patch;
    }

    @Override
    public List<StoryPatch> listChapterPatches(Long projectId, Long chapterId) {
        if (isRedisStoreEnabled()) {
            try {
                List<String> patchIds = readManifest(buildChapterPatchManifestKey(projectId, chapterId));
                if (!patchIds.isEmpty()) {
                    List<StoryPatch> patches = new ArrayList<>();
                    for (String patchId : patchIds) {
                        String payload = stringRedisTemplate.opsForValue().get(buildPatchKey(patchId));
                        if (!StringUtils.hasText(payload)) {
                            continue;
                        }
                        StoryPatch patch = objectMapper.readValue(payload, StoryPatch.class);
                        fallbackStore.appendPatch(projectId, chapterId, patch);
                        patches.add(patch);
                    }
                    if (!patches.isEmpty()) {
                        return List.copyOf(patches);
                    }
                }
            } catch (JsonProcessingException ex) {
                log.warn("Failed to deserialize story patches for chapter {}/{}", projectId, chapterId, ex);
            } catch (RedisConnectionFailureException ex) {
                log.warn("Redis unavailable when listing story patches for chapter {}/{}", projectId, chapterId);
            } catch (RuntimeException ex) {
                log.warn("Failed to list story patches for chapter {}/{}", projectId, chapterId, ex);
            }
        }
        return fallbackStore.listChapterPatches(projectId, chapterId);
    }

    @Override
    public ReaderRevealState saveChapterRevealState(ReaderRevealState state) {
        fallbackStore.saveChapterRevealState(state);
        if (!isRedisStoreEnabled()) {
            return state;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    buildChapterRevealStateKey(state.projectId(), state.chapterId()),
                    objectMapper.writeValueAsString(state),
                    Duration.ofMinutes(Math.max(1L, properties.getTtlMinutes()))
            );
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize chapter reveal state for chapter {}/{}", state.projectId(), state.chapterId(), ex);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable when saving chapter reveal state for chapter {}/{}", state.projectId(), state.chapterId());
        } catch (RuntimeException ex) {
            log.warn("Failed to save chapter reveal state for chapter {}/{}", state.projectId(), state.chapterId(), ex);
        }
        return state;
    }

    @Override
    public Optional<ReaderRevealState> findChapterRevealState(Long projectId, Long chapterId) {
        if (isRedisStoreEnabled()) {
            try {
                String payload = stringRedisTemplate.opsForValue().get(buildChapterRevealStateKey(projectId, chapterId));
                if (StringUtils.hasText(payload)) {
                    ReaderRevealState state = objectMapper.readValue(payload, ReaderRevealState.class);
                    fallbackStore.saveChapterRevealState(state);
                    return Optional.of(state);
                }
            } catch (JsonProcessingException ex) {
                log.warn("Failed to deserialize chapter reveal state for chapter {}/{}", projectId, chapterId, ex);
            } catch (RedisConnectionFailureException ex) {
                log.warn("Redis unavailable when reading chapter reveal state for chapter {}/{}", projectId, chapterId);
            } catch (RuntimeException ex) {
                log.warn("Failed to read chapter reveal state for chapter {}/{}", projectId, chapterId, ex);
            }
        }
        return fallbackStore.findChapterRevealState(projectId, chapterId);
    }

    private boolean isRedisStoreEnabled() {
        return properties.isRedisStoreEnabled() && stringRedisTemplate != null;
    }

    private String buildEventKey(String eventId) {
        return properties.getEventKeyPrefix() + eventId;
    }

    private String buildChapterEventManifestKey(Long projectId, Long chapterId) {
        return properties.getChapterEventManifestKeyPrefix() + projectId + ":" + chapterId;
    }

    private String buildSnapshotKey(String snapshotId) {
        return properties.getSnapshotKeyPrefix() + snapshotId;
    }

    private String buildChapterSnapshotManifestKey(Long projectId, Long chapterId) {
        return properties.getChapterSnapshotManifestKeyPrefix() + projectId + ":" + chapterId;
    }

    private String buildPatchKey(String patchId) {
        return properties.getPatchKeyPrefix() + patchId;
    }

    private String buildChapterPatchManifestKey(Long projectId, Long chapterId) {
        return properties.getChapterPatchManifestKeyPrefix() + projectId + ":" + chapterId;
    }

    private String buildChapterRevealStateKey(Long projectId, Long chapterId) {
        return properties.getChapterRevealStateKeyPrefix() + projectId + ":" + chapterId;
    }

    private void writeManifest(String manifestKey, String id) throws JsonProcessingException {
        List<String> current = readManifest(manifestKey);
        if (!current.contains(id)) {
            List<String> next = new ArrayList<>(current);
            next.add(id);
            stringRedisTemplate.opsForValue().set(
                    manifestKey,
                    objectMapper.writeValueAsString(next),
                    Duration.ofMinutes(Math.max(1L, properties.getTtlMinutes()))
            );
        }
    }

    private List<String> readManifest(String manifestKey) throws JsonProcessingException {
        String payload = stringRedisTemplate.opsForValue().get(manifestKey);
        if (!StringUtils.hasText(payload)) {
            return List.of();
        }
        List<String> ids = objectMapper.readValue(payload, new TypeReference<List<String>>() {});
        return ids == null ? List.of() : ids.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static final class InMemoryStoryStateStore implements StoryEventStore, StorySnapshotStore, StoryPatchStore, ReaderRevealStateStore {

        private final Map<String, StoryEvent> eventMap = new LinkedHashMap<>();
        private final Map<String, List<String>> chapterEventIndex = new LinkedHashMap<>();
        private final Map<String, StorySnapshot> snapshotMap = new LinkedHashMap<>();
        private final Map<String, List<String>> chapterSnapshotIndex = new LinkedHashMap<>();
        private final Map<String, StoryPatch> patchMap = new LinkedHashMap<>();
        private final Map<String, List<String>> chapterPatchIndex = new LinkedHashMap<>();
        private final Map<String, ReaderRevealState> chapterRevealStateMap = new LinkedHashMap<>();

        @Override
        public StoryEvent appendEvent(StoryEvent event) {
            eventMap.put(event.eventId(), event);
            chapterEventIndex.computeIfAbsent(chapterKey(event.projectId(), event.chapterId()), key -> new ArrayList<>());
            List<String> ids = chapterEventIndex.get(chapterKey(event.projectId(), event.chapterId()));
            if (!ids.contains(event.eventId())) {
                ids.add(event.eventId());
            }
            return event;
        }

        @Override
        public List<StoryEvent> listChapterEvents(Long projectId, Long chapterId) {
            return chapterEventIndex.getOrDefault(chapterKey(projectId, chapterId), List.of()).stream()
                    .map(eventMap::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        @Override
        public StorySnapshot saveSnapshot(StorySnapshot snapshot) {
            snapshotMap.put(snapshot.snapshotId(), snapshot);
            chapterSnapshotIndex.computeIfAbsent(chapterKey(snapshot.projectId(), snapshot.chapterId()), key -> new ArrayList<>());
            List<String> ids = chapterSnapshotIndex.get(chapterKey(snapshot.projectId(), snapshot.chapterId()));
            if (!ids.contains(snapshot.snapshotId())) {
                ids.add(snapshot.snapshotId());
            }
            return snapshot;
        }

        @Override
        public List<StorySnapshot> listChapterSnapshots(Long projectId, Long chapterId) {
            return chapterSnapshotIndex.getOrDefault(chapterKey(projectId, chapterId), List.of()).stream()
                    .map(snapshotMap::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        @Override
        public StoryPatch appendPatch(Long projectId, Long chapterId, StoryPatch patch) {
            patchMap.put(patch.patchId(), patch);
            chapterPatchIndex.computeIfAbsent(chapterKey(projectId, chapterId), key -> new ArrayList<>());
            List<String> ids = chapterPatchIndex.get(chapterKey(projectId, chapterId));
            if (!ids.contains(patch.patchId())) {
                ids.add(patch.patchId());
            }
            return patch;
        }

        @Override
        public List<StoryPatch> listChapterPatches(Long projectId, Long chapterId) {
            return chapterPatchIndex.getOrDefault(chapterKey(projectId, chapterId), List.of()).stream()
                    .map(patchMap::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        @Override
        public ReaderRevealState saveChapterRevealState(ReaderRevealState state) {
            chapterRevealStateMap.put(chapterKey(state.projectId(), state.chapterId()), state);
            return state;
        }

        @Override
        public Optional<ReaderRevealState> findChapterRevealState(Long projectId, Long chapterId) {
            return Optional.ofNullable(chapterRevealStateMap.get(chapterKey(projectId, chapterId)));
        }

        private String chapterKey(Long projectId, Long chapterId) {
            return projectId + ":" + chapterId;
        }
    }
}
