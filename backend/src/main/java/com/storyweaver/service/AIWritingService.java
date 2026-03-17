package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.dto.AIWritingRequestDTO;
import com.storyweaver.domain.entity.AIWritingRecord;
import com.storyweaver.domain.vo.AIWritingResponseVO;

import java.util.List;

public interface AIWritingService extends IService<AIWritingRecord> {
    
    AIWritingResponseVO generateContent(AIWritingRequestDTO requestDTO);
    
    List<AIWritingResponseVO> getRecordsByChapterId(Long chapterId);
    
    AIWritingResponseVO getRecordById(Long id);
    
    AIWritingResponseVO acceptGeneratedContent(Long id);
    
    void rejectGeneratedContent(Long id);
}