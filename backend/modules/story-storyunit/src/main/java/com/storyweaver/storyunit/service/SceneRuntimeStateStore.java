package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.session.SceneExecutionState;
import com.storyweaver.storyunit.session.SceneHandoffSnapshot;

import java.util.List;
import java.util.Optional;

public interface SceneRuntimeStateStore {

    Optional<SceneExecutionState> getSceneState(Long projectId, Long chapterId, String sceneId);

    List<SceneExecutionState> listChapterScenes(Long projectId, Long chapterId);

    SceneExecutionState saveSceneState(SceneExecutionState sceneExecutionState);

    void deleteSceneState(Long projectId, Long chapterId, String sceneId);

    Optional<SceneHandoffSnapshot> findHandoffToScene(Long projectId, Long chapterId, String sceneId);

    List<SceneHandoffSnapshot> listChapterHandoffs(Long projectId, Long chapterId);

    SceneHandoffSnapshot saveHandoff(SceneHandoffSnapshot snapshot);

    void deleteHandoffsFromScene(Long projectId, Long chapterId, String sceneId);

    void deleteHandoffsReferencingScene(Long projectId, Long chapterId, String sceneId);
}
