package com.storyweaver.ai.director.application;

import com.storyweaver.domain.dto.AIDirectorDecisionRequestDTO;
import com.storyweaver.domain.vo.AIDirectorDecisionVO;

public interface AIDirectorApplicationService {

    AIDirectorDecisionVO decide(Long userId, AIDirectorDecisionRequestDTO requestDTO);

    AIDirectorDecisionVO getLatestDecision(Long userId, Long chapterId);
}
