package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.dto.ChapterRequestDTO;
import com.storyweaver.domain.entity.Chapter;

import java.util.List;

public interface ChapterService extends IService<Chapter> {
    List<Chapter> getProjectChapters(Long projectId, Long userId);

    Chapter createChapter(Long projectId, Long userId, ChapterRequestDTO requestDTO);

    boolean updateChapter(Long projectId, Long chapterId, Long userId, ChapterRequestDTO requestDTO);

    boolean deleteChapter(Long chapterId, Long userId);

    Chapter getChapterWithAuth(Long chapterId, Long userId);

    List<String> getRequiredCharacterNames(Long chapterId);
}
