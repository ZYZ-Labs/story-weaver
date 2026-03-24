package com.storyweaver.service;

import com.storyweaver.domain.dto.NameSuggestionRequestDTO;
import com.storyweaver.domain.vo.NameSuggestionVO;

public interface NameSuggestionService {
    NameSuggestionVO generateSuggestions(Long projectId, Long userId, NameSuggestionRequestDTO requestDTO);
}
