package com.storyweaver.controller;

import com.storyweaver.BaseTest;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.entity.ProjectCharacterLink;
import com.storyweaver.domain.entity.User;
import com.storyweaver.item.infrastructure.persistence.mapper.CharacterInventoryItemMapper;
import com.storyweaver.item.infrastructure.persistence.mapper.ItemMapper;
import com.storyweaver.repository.CharacterMapper;
import com.storyweaver.repository.ProjectCharacterMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ItemInventoryControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private CharacterMapper characterMapper;

    @Autowired
    private ProjectCharacterMapper projectCharacterMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private CharacterInventoryItemMapper characterInventoryItemMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String authToken;
    private Project project;
    private Character character;

    @BeforeEach
    void setUpData() {
        User user = new User();
        user.setUsername("inventorytest");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setNickname("背包测试用户");
        user.setEmail("inventory@test.com");
        user.setStatus(1);
        userMapper.insert(user);

        authToken = jwtUtil.generateToken(user.getUsername(), user.getId());

        project = new Project();
        project.setName("背包项目");
        project.setDescription("用于背包接口测试");
        project.setGenre("奇幻");
        project.setUserId(user.getId());
        project.setStatus(0);
        projectMapper.insert(project);

        character = new Character();
        character.setProjectId(project.getId());
        character.setOwnerUserId(user.getId());
        character.setName("测试角色");
        character.setDescription("用于背包测试");
        character.setAttributes("{}");
        characterMapper.insert(character);

        ProjectCharacterLink link = new ProjectCharacterLink();
        link.setProjectId(project.getId());
        link.setCharacterId(character.getId());
        link.setProjectRole("主角");
        projectCharacterMapper.insert(link);
    }

    @Test
    void shouldCreateItemAndAddToInventory() throws Exception {
        String itemJson = """
                {
                  "name": "止血药剂",
                  "description": "用于恢复体力的基础药剂",
                  "category": "consumable",
                  "rarity": "common",
                  "stackable": true,
                  "maxStack": 10,
                  "usable": true,
                  "equippable": false,
                  "slotType": "consumable",
                  "itemValue": 25,
                  "weight": 1,
                  "attributesJson": "{\\\"heal\\\":20}",
                  "effectJson": "{\\\"target\\\":\\\"self\\\"}",
                  "tags": "治疗, 常备"
                }
                """;

        String response = mockMvc.perform(post("/api/projects/" + project.getId() + "/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("止血药剂"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long itemId = extractId(response);

        String inventoryJson = """
                {
                  "itemId": %d,
                  "quantity": 3,
                  "durability": 100
                }
                """.formatted(itemId);

        mockMvc.perform(post("/api/projects/" + project.getId() + "/characters/" + character.getId() + "/inventory")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inventoryJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.quantity").value(3))
                .andExpect(jsonPath("$.data.item.name").value("止血药剂"));

        mockMvc.perform(get("/api/projects/" + project.getId() + "/characters/" + character.getId() + "/inventory")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].item.category").value("consumable"));
    }

    @Test
    void shouldRejectInvalidItemJson() throws Exception {
        String invalidItemJson = """
                {
                  "name": "损坏卷轴",
                  "attributesJson": "{broken",
                  "effectJson": "{}"
                }
                """;

        mockMvc.perform(post("/api/projects/" + project.getId() + "/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidItemJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("物品属性 JSON 格式不正确"));
    }

    @Test
    void shouldDeleteItemAndRemoveInventoryReference() throws Exception {
        String itemJson = """
                {
                  "name": "铁剑",
                  "category": "equipment",
                  "rarity": "common",
                  "stackable": false,
                  "equippable": true,
                  "slotType": "weapon",
                  "attributesJson": "{}",
                  "effectJson": "{}"
                }
                """;

        String response = mockMvc.perform(post("/api/projects/" + project.getId() + "/items")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long itemId = extractId(response);

        mockMvc.perform(post("/api/projects/" + project.getId() + "/characters/" + character.getId() + "/inventory")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":" + itemId + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/projects/" + project.getId() + "/items/" + itemId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/projects/" + project.getId() + "/characters/" + character.getId() + "/inventory")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private Long extractId(String responseBody) {
        String marker = "\"id\":";
        int start = responseBody.indexOf(marker);
        int end = responseBody.indexOf(',', start);
        return Long.parseLong(responseBody.substring(start + marker.length(), end).trim());
    }
}
