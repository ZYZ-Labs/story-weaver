package com.storyweaver.story.generation;

import com.storyweaver.domain.dto.StructuredCreationApplyRequestDTO;

public interface StructuredCreationApplyService {

    StructuredCreationApplyResult apply(Long userId, StructuredCreationApplyRequestDTO requestDTO);
}
