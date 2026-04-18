package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneHandoffSnapshot;

import java.util.List;
import java.util.Optional;

public interface SceneRuntimeStateStore {

    Optional<SceneExecutionState> getSceneState(Long projectId, Long chapterId, String sceneId);

    List<SceneExecutionState> listChapterScenes(Long projectId, Long chapterId);

    SceneExecutionState saveSceneState(SceneExecutionState sceneExecutionState);

    Optional<SceneHandoffSnapshot> findHandoffToScene(Long projectId, Long chapterId, String sceneId);

    SceneHandoffSnapshot saveHandoff(SceneHandoffSnapshot snapshot);
}
