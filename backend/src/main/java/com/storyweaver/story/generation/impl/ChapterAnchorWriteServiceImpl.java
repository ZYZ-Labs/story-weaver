package com.storyweaver.story.generation.impl;

import com.storyweaver.domain.dto.ChapterAnchorUpdateRequestDTO;
import com.storyweaver.domain.dto.ChapterRequestDTO;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.service.ChapterService;
import com.storyweaver.story.generation.ChapterAnchorBundle;
import com.storyweaver.story.generation.ChapterAnchorResolver;
import com.storyweaver.story.generation.ChapterAnchorUpdateResult;
import com.storyweaver.story.generation.ChapterAnchorWriteService;
import com.storyweaver.story.generation.GenerationReadinessService;
import com.storyweaver.story.generation.GenerationReadinessVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ChapterAnchorWriteServiceImpl implements ChapterAnchorWriteService {

    private final ChapterService chapterService;
    private final ChapterAnchorResolver chapterAnchorResolver;
    private final GenerationReadinessService generationReadinessService;

    public ChapterAnchorWriteServiceImpl(
            ChapterService chapterService,
            ChapterAnchorResolver chapterAnchorResolver,
            GenerationReadinessService generationReadinessService) {
        this.chapterService = chapterService;
        this.chapterAnchorResolver = chapterAnchorResolver;
        this.generationReadinessService = generationReadinessService;
    }

    @Override
    @Transactional
    public ChapterAnchorUpdateResult updateAnchors(Long userId, Long projectId, Long chapterId, ChapterAnchorUpdateRequestDTO requestDTO) {
        Chapter current = chapterService.getChapterWithAuth(chapterId, userId);
        if (current == null || !Objects.equals(current.getProjectId(), projectId)) {
            throw new IllegalArgumentException("章节不存在或无权访问");
        }

        ChapterRequestDTO update = new ChapterRequestDTO();
        update.setTitle(current.getTitle());
        update.setSummary(current.getSummary());
        update.setContent(current.getContent());
        update.setOrderNum(current.getOrderNum());
        update.setStatus(current.getStatus());
        update.setChapterStatus(current.getChapterStatus());
        update.setPrevChapterId(current.getPrevChapterId());
        update.setNextChapterId(current.getNextChapterId());
        update.setOutlineId(requestDTO == null ? null : requestDTO.getOutlineId());
        update.setMainPovCharacterId(requestDTO == null ? null : requestDTO.getMainPovCharacterId());
        update.setRequiredCharacterIds(copyLongList(requestDTO == null ? null : requestDTO.getRequiredCharacterIds()));
        update.setStoryBeatIds(copyLongList(requestDTO == null ? null : requestDTO.getStoryBeatIds()));

        boolean updated = chapterService.updateChapter(projectId, chapterId, userId, update);
        if (!updated) {
            throw new IllegalStateException("章节锚点写回失败");
        }

        Chapter refreshed = chapterService.getChapterWithAuth(chapterId, userId);
        ChapterAnchorBundle anchorBundle = chapterAnchorResolver.resolve(userId, chapterId);
        GenerationReadinessVO generationReadiness = generationReadinessService.evaluate(userId, chapterId);

        ChapterAnchorUpdateResult result = new ChapterAnchorUpdateResult();
        result.setChapter(refreshed);
        result.setAnchorBundle(anchorBundle);
        result.setGenerationReadiness(generationReadiness);
        return result;
    }

    private List<Long> copyLongList(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(values);
    }
}
