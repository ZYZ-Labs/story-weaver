package com.storyweaver.storyunit.service;

import com.storyweaver.storyunit.patch.StoryPatch;

import java.util.List;

public interface StoryPatchStore {

    StoryPatch appendPatch(Long projectId, Long chapterId, StoryPatch patch);

    List<StoryPatch> listChapterPatches(Long projectId, Long chapterId);
}
