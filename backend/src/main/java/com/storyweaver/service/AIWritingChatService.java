package com.storyweaver.service;

import com.storyweaver.domain.dto.AIWritingChatMessageRequestDTO;
import com.storyweaver.domain.vo.AIWritingChatSessionVO;

public interface AIWritingChatService {

    AIWritingChatSessionVO getSession(Long userId, Long chapterId);

    AIWritingChatSessionVO sendMessage(Long userId, Long chapterId, AIWritingChatMessageRequestDTO requestDTO);

    AIWritingChatSessionVO setMessagePinnedToBackground(Long userId, Long messageId, boolean pinned);

    String buildBackgroundContext(Long userId, Long chapterId);
}
