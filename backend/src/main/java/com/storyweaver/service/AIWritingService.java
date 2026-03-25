package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.dto.AIWritingRequestDTO;
import com.storyweaver.domain.entity.AIWritingRecord;
import com.storyweaver.domain.vo.AIWritingResponseVO;
import com.storyweaver.domain.vo.AIWritingStreamEventVO;

import java.util.List;
import java.util.function.Consumer;

public interface AIWritingService extends IService<AIWritingRecord> {

    AIWritingResponseVO generateContent(AIWritingRequestDTO requestDTO);

    void streamContent(AIWritingRequestDTO requestDTO, Consumer<AIWritingStreamEventVO> eventConsumer);

    List<AIWritingResponseVO> getRecordsByChapterId(Long chapterId);

    List<AIWritingResponseVO> getRecordsByProjectId(Long projectId);

    AIWritingResponseVO getRecordById(Long id);

    AIWritingResponseVO acceptGeneratedContent(Long id);

    void rejectGeneratedContent(Long id);
}
