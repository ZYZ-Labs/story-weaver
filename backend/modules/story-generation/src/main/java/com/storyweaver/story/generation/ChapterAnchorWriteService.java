package com.storyweaver.story.generation;

import com.storyweaver.domain.dto.ChapterAnchorUpdateRequestDTO;

public interface ChapterAnchorWriteService {

    ChapterAnchorUpdateResult updateAnchors(Long userId, Long projectId, Long chapterId, ChapterAnchorUpdateRequestDTO requestDTO);
}
