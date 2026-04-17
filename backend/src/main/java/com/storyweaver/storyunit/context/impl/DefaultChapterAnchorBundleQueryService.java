package com.storyweaver.storyunit.context.impl;

import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Outline;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.repository.ChapterMapper;
import com.storyweaver.repository.OutlineMapper;
import com.storyweaver.repository.ProjectMapper;
import com.storyweaver.story.generation.ChapterAnchorBundle;
import com.storyweaver.story.generation.ChapterAnchorResolver;
import com.storyweaver.storyunit.context.ChapterAnchorBundleQueryService;
import com.storyweaver.storyunit.context.ChapterAnchorBundleView;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DefaultChapterAnchorBundleQueryService implements ChapterAnchorBundleQueryService {

    private final ProjectMapper projectMapper;
    private final ChapterMapper chapterMapper;
    private final OutlineMapper outlineMapper;
    private final ChapterAnchorResolver chapterAnchorResolver;

    public DefaultChapterAnchorBundleQueryService(
            ProjectMapper projectMapper,
            ChapterMapper chapterMapper,
            OutlineMapper outlineMapper,
            ChapterAnchorResolver chapterAnchorResolver) {
        this.projectMapper = projectMapper;
        this.chapterMapper = chapterMapper;
        this.outlineMapper = outlineMapper;
        this.chapterAnchorResolver = chapterAnchorResolver;
    }

    @Override
    public Optional<ChapterAnchorBundleView> getChapterAnchorBundle(Long projectId, Long chapterId) {
        if (projectId == null || chapterId == null) {
            return Optional.empty();
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null || Integer.valueOf(1).equals(project.getDeleted()) || project.getUserId() == null) {
            return Optional.empty();
        }

        ChapterAnchorBundle bundle = chapterAnchorResolver.resolve(project.getUserId(), chapterId);
        if (bundle == null || !projectId.equals(bundle.getProjectId())) {
            return Optional.empty();
        }

        Chapter chapter = chapterMapper.selectById(chapterId);
        Outline outline = bundle.getChapterOutlineId() == null ? null : outlineMapper.selectById(bundle.getChapterOutlineId());

        return Optional.of(new ChapterAnchorBundleView(
                bundle.getProjectId(),
                bundle.getChapterId(),
                chapter == null ? "" : ContextViewSupport.trimToEmpty(chapter.getTitle()),
                bundle.getChapterOutlineId(),
                outline == null ? "" : ContextViewSupport.trimToEmpty(outline.getTitle()),
                bundle.getMainPovCharacterId(),
                bundle.getMainPovCharacterName(),
                ContextViewSupport.sanitizeDistinct(bundle.getRequiredCharacterNames()),
                ContextViewSupport.sanitizeDistinct(bundle.getStoryBeatTitles()),
                ContextViewSupport.sanitizeDistinct(bundle.getStoryBeatTitles()),
                bundle.getChapterSummary()
        ));
    }
}
