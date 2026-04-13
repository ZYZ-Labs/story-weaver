package com.storyweaver.story.generation;

import com.storyweaver.domain.dto.StructuredSummaryApplyRequestDTO;

public interface StructuredSummaryApplyService {

    StructuredSummaryApplyResult apply(Long userId, StructuredSummaryApplyRequestDTO requestDTO);
}
