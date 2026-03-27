package com.storyweaver.service;

import com.storyweaver.domain.dto.AIWritingChatMessageRequestDTO;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.domain.vo.AIWritingChatParticipationVO;
import com.storyweaver.domain.vo.AIWritingChatSessionVO;
import com.storyweaver.domain.vo.AIWritingChatStreamEventVO;

import java.util.function.Consumer;

public interface AIWritingChatService {

    AIWritingChatSessionVO getSession(Long userId, Long chapterId);

    AIWritingChatSessionVO sendMessage(Long userId, Long chapterId, AIWritingChatMessageRequestDTO requestDTO);

    void streamMessage(
            Long userId,
            Long chapterId,
            AIWritingChatMessageRequestDTO requestDTO,
            Consumer<AIWritingChatStreamEventVO> eventConsumer);

    AIWritingChatSessionVO setMessagePinnedToBackground(Long userId, Long messageId, boolean pinned);

    boolean hasBackgroundContext(Long userId, Long chapterId);

    AIWritingChatParticipationVO buildParticipationContext(Long userId, Long chapterId, AIProvider provider, String model);
}
