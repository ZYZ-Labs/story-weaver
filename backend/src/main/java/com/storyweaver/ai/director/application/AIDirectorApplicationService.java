package com.storyweaver.ai.director.application;

import com.storyweaver.domain.dto.AIDirectorDecisionRequestDTO;
import com.storyweaver.domain.vo.AIDirectorDecisionVO;

public interface AIDirectorApplicationService {

    AIDirectorDecisionVO decide(Long userId, AIDirectorDecisionRequestDTO requestDTO);

    AIDirectorDecisionVO getDecision(Long userId, Long decisionId);

    AIDirectorDecisionVO getLatestDecision(Long userId, Long chapterId);
}
