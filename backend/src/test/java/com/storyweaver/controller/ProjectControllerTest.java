package com.storyweaver.controller;

import com.storyweaver.BaseTest;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.entity.User;
import com.storyweaver.repository.ProjectMapper;
import com.storyweaver.repository.UserMapper;
import com.storyweaver.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class ProjectControllerTest extends BaseTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ProjectMapper projectMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private User testUser;
    private String authToken;
    
    @BeforeEach
    void setUpTestData() {
        testUser = new User();
        testUser.setUsername("apitest");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setNickname("API测试用户");
        testUser.setEmail("api@test.com");
        testUser.setStatus(1);
        
        userMapper.insert(testUser);
        
        authToken = jwtUtil.generateToken(testUser.getUsername(), testUser.getId());
    }
    
    @Test
    void testCreateProject() throws Exception {
        String projectJson = """
            {
                "name": "API测试项目",
                "description": "通过API创建的测试项目",
                "genre": "玄幻",
                "status": 0
            }
            """;
        
        mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(projectJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("API测试项目"))
                .andExpect(jsonPath("$.data.genre").value("玄幻"));
    }
    
    @Test
    void testGetProjectList() throws Exception {
        Project project1 = new Project();
        project1.setName("项目A");
        project1.setDescription("测试项目A");
        project1.setGenre("科幻");
        project1.setUserId(testUser.getId());
        project1.setStatus(0);
        
        Project project2 = new Project();
        project2.setName("项目B");
        project2.setDescription("测试项目B");
        project2.setGenre("都市");
        project2.setUserId(testUser.getId());
        project2.setStatus(0);
        
        projectMapper.insert(project1);
        projectMapper.insert(project2);
        
        mockMvc.perform(get("/api/projects")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
    
    @Test
    void testGetProjectById() throws Exception {
        Project project = new Project();
        project.setName("详细查询项目");
        project.setDescription("用于详细查询的测试项目");
        project.setGenre("武侠");
        project.setUserId(testUser.getId());
        project.setStatus(0);
        
        projectMapper.insert(project);
        
        mockMvc.perform(get("/api/projects/" + project.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("详细查询项目"))
                .andExpect(jsonPath("$.data.genre").value("武侠"));
    }
    
    @Test
    void testUpdateProject() throws Exception {
        Project project = new Project();
        project.setName("原始项目");
        project.setDescription("原始描述");
        project.setGenre("历史");
        project.setUserId(testUser.getId());
        project.setStatus(0);
        
        projectMapper.insert(project);
        
        String updateJson = """
            {
                "name": "更新后的项目",
                "description": "更新后的描述",
                "genre": "奇幻"
            }
            """;
        
        mockMvc.perform(put("/api/projects/" + project.getId())
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
        
        Project updatedProject = projectMapper.selectById(project.getId());
        assertEquals("更新后的项目", updatedProject.getName());
        assertEquals("更新后的描述", updatedProject.getDescription());
        assertEquals("奇幻", updatedProject.getGenre());
    }
    
    @Test
    void testDeleteProject() throws Exception {
        Project project = new Project();
        project.setName("待删除项目");
        project.setDescription("这个项目将被删除");
        project.setGenre("悬疑");
        project.setUserId(testUser.getId());
        project.setStatus(0);
        
        projectMapper.insert(project);
        
        mockMvc.perform(delete("/api/projects/" + project.getId())
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
        
        Project deletedProject = projectMapper.selectById(project.getId());
        assertNull(deletedProject);
    }
    
    @Test
    void testCreateProject_Unauthorized() throws Exception {
        String projectJson = """
            {
                "name": "未授权测试",
                "description": "未授权创建的项目",
                "genre": "测试"
            }
            """;
        
        mockMvc.perform(post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(projectJson))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testGetProject_NotFound() throws Exception {
        mockMvc.perform(get("/api/projects/99999")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
    
    private void assertEquals(String expected, String actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
    
    private void assertNull(Object object) {
        org.junit.jupiter.api.Assertions.assertNull(object);
    }
}