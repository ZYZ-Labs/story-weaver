package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.repository.ChapterMapper;
import com.storyweaver.service.ChapterService;
import com.storyweaver.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChapterServiceImpl extends ServiceImpl<ChapterMapper, Chapter> implements ChapterService {
    @Autowired
    private ProjectService projectService;

    @Override
    public List<Chapter> getProjectChapters(Long projectId, Long userId) {
        // 验证用户是否有权限访问该项目
        QueryWrapper<Project> projectQuery = new QueryWrapper<>();
        projectQuery.eq("id", projectId)
                   .eq("user_id", userId)
                   .eq("deleted", 0);
        
        Project project = projectService.getOne(projectQuery);
        if (project == null) {
            return List.of();
        }
        
        QueryWrapper<Chapter> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id", projectId)
                   .eq("deleted", 0)
                   .orderByAsc("order_num", "create_time");
        
        return list(queryWrapper);
    }

    @Override
    public Chapter createChapter(Long projectId, Long userId, String title, String content, Integer orderNum) {
        // 验证用户是否有权限访问该项目
        QueryWrapper<Project> projectQuery = new QueryWrapper<>();
        projectQuery.eq("id", projectId)
                   .eq("user_id", userId)
                   .eq("deleted", 0);
        
        Project project = projectService.getOne(projectQuery);
        if (project == null) {
            return null;
        }
        
        Chapter chapter = new Chapter();
        chapter.setProjectId(projectId);
        chapter.setTitle(title);
        chapter.setContent(content);
        chapter.setOrderNum(orderNum != null ? orderNum : 0);
        chapter.setStatus(0); // 草稿状态
        chapter.setWordCount(content != null ? content.length() : 0);
        
        save(chapter);
        return chapter;
    }

    @Override
    public boolean updateChapter(Long chapterId, Long userId, Chapter chapter) {
        Chapter existing = getChapterWithAuth(chapterId, userId);
        if (existing == null) {
            return false;
        }
        
        existing.setTitle(chapter.getTitle());
        existing.setContent(chapter.getContent());
        existing.setOrderNum(chapter.getOrderNum());
        existing.setStatus(chapter.getStatus());
        if (chapter.getContent() != null) {
            existing.setWordCount(chapter.getContent().length());
        }
        
        return updateById(existing);
    }

    @Override
    public boolean deleteChapter(Long chapterId, Long userId) {
        Chapter chapter = getChapterWithAuth(chapterId, userId);
        if (chapter == null) {
            return false;
        }
        
        return removeById(chapterId);
    }

    @Override
    public Chapter getChapterWithAuth(Long chapterId, Long userId) {
        QueryWrapper<Chapter> chapterQuery = new QueryWrapper<>();
        chapterQuery.eq("id", chapterId)
                   .eq("deleted", 0);
        
        Chapter chapter = getOne(chapterQuery);
        if (chapter == null) {
            return null;
        }
        
        // 验证用户是否有权限访问该项目
        QueryWrapper<Project> projectQuery = new QueryWrapper<>();
        projectQuery.eq("id", chapter.getProjectId())
                   .eq("user_id", userId)
                   .eq("deleted", 0);
        
        Project project = projectService.getOne(projectQuery);
        if (project == null) {
            return null;
        }
        
        return chapter;
    }
}