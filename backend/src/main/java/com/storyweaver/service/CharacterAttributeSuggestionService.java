package com.storyweaver.service;

import com.storyweaver.domain.dto.CharacterAttributeSuggestionRequestDTO;
import com.storyweaver.domain.vo.CharacterAttributeSuggestionVO;

public interface CharacterAttributeSuggestionService {
    CharacterAttributeSuggestionVO generateAttributes(Long projectId, Long userId, CharacterAttributeSuggestionRequestDTO requestDTO);
}
