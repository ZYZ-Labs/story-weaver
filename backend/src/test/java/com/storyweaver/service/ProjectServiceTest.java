package com.storyweaver.service;

import com.storyweaver.BaseTest;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.entity.User;
import com.storyweaver.repository.ProjectMapper;
import com.storyweaver.repository.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectServiceTest extends BaseTest {
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private ProjectMapper projectMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private User testUser;
    
    @BeforeEach
    void setUpTestData() {
        testUser = new User();
        testUser.setUsername("projecttest");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setNickname("项目测试用户");
        testUser.setEmail("project@test.com");
        testUser.setStatus(1);
        
        userMapper.insert(testUser);
    }
    
    @Test
    void testCreateProject() {
        Project project = new Project();
        project.setName("测试小说项目");
        project.setDescription("这是一个测试小说项目");
        project.setGenre("玄幻");
        project.setUserId(testUser.getId());
        project.setStatus(0);
        
        boolean result = projectService.save(project);
        
        assertTrue(result);
        assertNotNull(project.getId());
        
        Project savedProject = projectMapper.selectById(project.getId());
        assertNotNull(savedProject);
        assertEquals("测试小说项目", savedProject.getName());
        assertEquals(testUser.getId(), savedProject.getUserId());
    }
    
    @Test
    void testGetProjectById() {
        Project project = new Project();
        project.setName("查询测试项目");
        project.setDescription("用于查询测试的项目");
        project.setGenre("科幻");
        project.setUserId(testUser.getId());
        project.setStatus(0);
        
        projectMapper.insert(project);
        
        Project foundProject = projectService.getById(project.getId());
        
        assertNotNull(foundProject);
        assertEquals("查询测试项目", foundProject.getName());
        assertEquals("科幻", foundProject.getGenre());
    }
    
    @Test
    void testUpdateProject() {
        Project project = new Project();
        project.setName("原始项目");
        project.setDescription("原始描述");
        project.setGenre("都市");
        project.setUserId(testUser.getId());
        project.setStatus(0);
        
        projectMapper.insert(project);
        
        project.setName("更新后的项目");
        project.setDescription("更新后的描述");
        
        boolean result = projectService.updateById(project);
        
        assertTrue(result);
        
        Project updatedProject = projectMapper.selectById(project.getId());
        assertEquals("更新后的项目", updatedProject.getName());
        assertEquals("更新后的描述", updatedProject.getDescription());
    }
    
    @Test
    void testDeleteProject() {
        Project project = new Project();
        project.setName("待删除项目");
        project.setDescription("这个项目将被删除");
        project.setGenre("武侠");
        project.setUserId(testUser.getId());
        project.setStatus(0);
        
        projectMapper.insert(project);
        
        boolean result = projectService.removeById(project.getId());
        
        assertTrue(result);
        
        Project deletedProject = projectMapper.selectById(project.getId());
        assertNull(deletedProject);
    }
    
    @Test
    void testGetProjectsByOwner() {
        Project project1 = new Project();
        project1.setName("项目1");
        project1.setDescription("第一个测试项目");
        project1.setGenre("玄幻");
        project1.setUserId(testUser.getId());
        project1.setStatus(0);
        
        Project project2 = new Project();
        project2.setName("项目2");
        project2.setDescription("第二个测试项目");
        project2.setGenre("科幻");
        project2.setUserId(testUser.getId());
        project2.setStatus(0);
        
        projectMapper.insert(project1);
        projectMapper.insert(project2);
        
        List<Project> projects = projectService.list();
        
        assertNotNull(projects);
        assertTrue(projects.size() >= 2);
        
        boolean foundProject1 = projects.stream()
            .anyMatch(p -> "项目1".equals(p.getName()));
        boolean foundProject2 = projects.stream()
            .anyMatch(p -> "项目2".equals(p.getName()));
        
        assertTrue(foundProject1);
        assertTrue(foundProject2);
    }
}