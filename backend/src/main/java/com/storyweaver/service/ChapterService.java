package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.entity.Chapter;

import java.util.List;

public interface ChapterService extends IService<Chapter> {
    List<Chapter> getProjectChapters(Long projectId, Long userId);
    
    Chapter createChapter(Long projectId, Long userId, String title, String content, Integer orderNum);
    
    boolean updateChapter(Long chapterId, Long userId, Chapter chapter);
    
    boolean deleteChapter(Long chapterId, Long userId);
    
    Chapter getChapterWithAuth(Long chapterId, Long userId);
}