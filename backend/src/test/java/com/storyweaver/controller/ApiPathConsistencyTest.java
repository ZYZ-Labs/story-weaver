package com.storyweaver.controller;

import com.storyweaver.service.ChapterService;
import com.storyweaver.service.CharacterService;
import com.storyweaver.service.ProjectService;
import com.storyweaver.service.UserService;
import com.storyweaver.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiPathConsistencyTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProjectController projectController = new ProjectController();
        ChapterController chapterController = new ChapterController();
        CharacterController characterController = new CharacterController();
        AuthController authController = new AuthController();

        ReflectionTestUtils.setField(projectController, "projectService", mock(ProjectService.class));
        ReflectionTestUtils.setField(projectController, "jwtUtil", mock(JwtUtil.class));

        ReflectionTestUtils.setField(chapterController, "chapterService", mock(ChapterService.class));
        ReflectionTestUtils.setField(chapterController, "jwtUtil", mock(JwtUtil.class));

        ReflectionTestUtils.setField(characterController, "characterService", mock(CharacterService.class));
        ReflectionTestUtils.setField(characterController, "jwtUtil", mock(JwtUtil.class));

        ReflectionTestUtils.setField(authController, "userService", mock(UserService.class));
        ReflectionTestUtils.setField(authController, "jwtUtil", mock(JwtUtil.class));

        mockMvc = MockMvcBuilders.standaloneSetup(
                projectController,
                chapterController,
                characterController,
                authController
        ).build();
    }

    @Test
    void apiPrefixMappingsShouldExistAndLegacyMappingsShouldNotExist() throws Exception {
        mockMvc.perform(options("/api/projects")).andExpect(status().isOk());
        mockMvc.perform(options("/api/projects/1/chapters")).andExpect(status().isOk());
        mockMvc.perform(options("/api/projects/1/characters")).andExpect(status().isOk());
        mockMvc.perform(options("/api/auth/login")).andExpect(status().isOk());

        mockMvc.perform(options("/projects")).andExpect(status().isNotFound());
        mockMvc.perform(options("/projects/1/chapters")).andExpect(status().isNotFound());
        mockMvc.perform(options("/projects/1/characters")).andExpect(status().isNotFound());
        mockMvc.perform(options("/auth/login")).andExpect(status().isNotFound());
    }
}
