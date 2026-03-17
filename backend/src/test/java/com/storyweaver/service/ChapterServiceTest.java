package com.storyweaver.service;

import com.storyweaver.BaseTest;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.entity.User;
import com.storyweaver.repository.ChapterMapper;
import com.storyweaver.repository.ProjectMapper;
import com.storyweaver.repository.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChapterServiceTest extends BaseTest {
    
    @Autowired
    private ChapterService chapterService;
    
    @Autowired
    private ChapterMapper chapterMapper;
    
    @Autowired
    private ProjectMapper projectMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private User testUser;
    private Project testProject;
    
    @BeforeEach
    void setUpTestData() {
        testUser = new User();
        testUser.setUsername("chaptertest");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setNickname("章节测试用户");
        testUser.setEmail("chapter@test.com");
        testUser.setStatus(1);
        
        userMapper.insert(testUser);
        
        testProject = new Project();
        testProject.setName("章节测试项目");
        testProject.setDescription("用于章节测试的项目");
        testProject.setGenre("玄幻");
        testProject.setUserId(testUser.getId());
        testProject.setStatus(0);
        
        projectMapper.insert(testProject);
    }
    
    @Test
    void testCreateChapter() {
        Chapter chapter = new Chapter();
        chapter.setTitle("第一章：开端");
        chapter.setContent("这是一个测试章节内容...");
        chapter.setProjectId(testProject.getId());
        chapter.setOrderNum(1);
        chapter.setStatus(0);
        
        boolean result = chapterService.save(chapter);
        
        assertTrue(result);
        assertNotNull(chapter.getId());
        
        Chapter savedChapter = chapterMapper.selectById(chapter.getId());
        assertNotNull(savedChapter);
        assertEquals("第一章：开端", savedChapter.getTitle());
        assertEquals(testProject.getId(), savedChapter.getProjectId());
        assertEquals(1, savedChapter.getOrderNum());
    }
    
    @Test
    void testGetChaptersByProject() {
        Chapter chapter1 = new Chapter();
        chapter1.setTitle("第一章");
        chapter1.setContent("第一章内容");
        chapter1.setProjectId(testProject.getId());
        chapter1.setOrderNum(1);
        chapter1.setStatus(0);
        
        Chapter chapter2 = new Chapter();
        chapter2.setTitle("第二章");
        chapter2.setContent("第二章内容");
        chapter2.setProjectId(testProject.getId());
        chapter2.setOrderNum(2);
        chapter2.setStatus(0);
        
        chapterMapper.insert(chapter1);
        chapterMapper.insert(chapter2);
        
        List<Chapter> chapters = chapterService.list();
        
        assertNotNull(chapters);
        assertTrue(chapters.size() >= 2);
        
        boolean foundChapter1 = chapters.stream()
            .anyMatch(c -> "第一章".equals(c.getTitle()));
        boolean foundChapter2 = chapters.stream()
            .anyMatch(c -> "第二章".equals(c.getTitle()));
        
        assertTrue(foundChapter1);
        assertTrue(foundChapter2);
    }
    
    @Test
    void testUpdateChapterContent() {
        Chapter chapter = new Chapter();
        chapter.setTitle("原始章节");
        chapter.setContent("原始内容");
        chapter.setProjectId(testProject.getId());
        chapter.setOrderNum(1);
        chapter.setStatus(0);
        
        chapterMapper.insert(chapter);
        
        chapter.setContent("更新后的内容");
        chapter.setWordCount(1500);
        
        boolean result = chapterService.updateById(chapter);
        
        assertTrue(result);
        
        Chapter updatedChapter = chapterMapper.selectById(chapter.getId());
        assertEquals("更新后的内容", updatedChapter.getContent());
        assertEquals(1500, updatedChapter.getWordCount());
    }
    
    @Test
    void testDeleteChapter() {
        Chapter chapter = new Chapter();
        chapter.setTitle("待删除章节");
        chapter.setContent("这个章节将被删除");
        chapter.setProjectId(testProject.getId());
        chapter.setOrderNum(99);
        chapter.setStatus(0);
        
        chapterMapper.insert(chapter);
        
        boolean result = chapterService.removeById(chapter.getId());
        
        assertTrue(result);
        
        Chapter deletedChapter = chapterMapper.selectById(chapter.getId());
        assertNull(deletedChapter);
    }
    
    @Test
    void testGetChapterByNumber() {
        Chapter chapter = new Chapter();
        chapter.setTitle("特定章节");
        chapter.setContent("特定章节内容");
        chapter.setProjectId(testProject.getId());
        chapter.setOrderNum(5);
        chapter.setStatus(0);
        
        chapterMapper.insert(chapter);
        
        List<Chapter> chapters = chapterService.list();
        
        Chapter foundChapter = chapters.stream()
            .filter(c -> c.getOrderNum() == 5)
            .findFirst()
            .orElse(null);
        
        assertNotNull(foundChapter);
        assertEquals("特定章节", foundChapter.getTitle());
        assertEquals(5, foundChapter.getOrderNum());
    }
    
    @Test
    void testUpdateChapterStatus() {
        Chapter chapter = new Chapter();
        chapter.setTitle("状态测试章节");
        chapter.setContent("测试状态变更");
        chapter.setProjectId(testProject.getId());
        chapter.setOrderNum(1);
        chapter.setStatus(0);
        
        chapterMapper.insert(chapter);
        
        chapter.setStatus(1);
        
        boolean result = chapterService.updateById(chapter);
        
        assertTrue(result);
        
        Chapter updatedChapter = chapterMapper.selectById(chapter.getId());
        assertEquals(1, updatedChapter.getStatus());
    }
}