package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.session.SceneExecutionState;

import java.util.List;
import java.util.Optional;

public interface SceneExecutionStateQueryService {

    Optional<SceneExecutionState> getSceneState(Long projectId, Long chapterId, String sceneId);

    List<SceneExecutionState> listChapterScenes(Long projectId, Long chapterId);

    Optional<SceneExecutionState> findLatestChapterScene(Long projectId, Long chapterId);
}
