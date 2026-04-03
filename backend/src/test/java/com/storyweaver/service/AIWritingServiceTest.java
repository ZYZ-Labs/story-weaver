package com.storyweaver.service;

import com.storyweaver.BaseTest;
import com.storyweaver.domain.dto.AIWritingRequestDTO;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.ChapterCharacterLink;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.entity.ProjectCharacterLink;
import com.storyweaver.domain.entity.User;
import com.storyweaver.domain.vo.AIWritingResponseVO;
import com.storyweaver.item.domain.entity.CharacterInventoryItem;
import com.storyweaver.item.domain.entity.ItemDefinition;
import com.storyweaver.item.infrastructure.persistence.mapper.CharacterInventoryItemMapper;
import com.storyweaver.item.infrastructure.persistence.mapper.ItemMapper;
import com.storyweaver.repository.ChapterCharacterMapper;
import com.storyweaver.repository.ChapterMapper;
import com.storyweaver.repository.CharacterMapper;
import com.storyweaver.repository.ProjectCharacterMapper;
import com.storyweaver.repository.ProjectMapper;
import com.storyweaver.repository.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIWritingServiceTest extends BaseTest {

    @Autowired
    private AIWritingService aiWritingService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ChapterMapper chapterMapper;

    @Autowired
    private CharacterMapper characterMapper;

    @Autowired
    private ProjectCharacterMapper projectCharacterMapper;

    @Autowired
    private ChapterCharacterMapper chapterCharacterMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private CharacterInventoryItemMapper characterInventoryItemMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AIProviderService aiProviderService;

    @MockBean
    private AIModelRoutingService aiModelRoutingService;

    @MockBean
    private AIWritingChatService aiWritingChatService;

    private User testUser;
    private Project testProject;
    private Chapter testChapter;
    private Character requiredCharacter;
    private Character unrelatedCharacter;

    @BeforeEach
    void setUpTestData() {
        testUser = new User();
        testUser.setUsername("aiwritingtest");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setNickname("AI 写作测试用户");
        testUser.setEmail("aiwriting@test.com");
        testUser.setStatus(1);
        userMapper.insert(testUser);

        testProject = new Project();
        testProject.setName("背包上下文项目");
        testProject.setDescription("验证背包是否进入 AI 写作上下文");
        testProject.setGenre("玄幻");
        testProject.setUserId(testUser.getId());
        testProject.setStatus(0);
        projectMapper.insert(testProject);

        testChapter = new Chapter();
        testChapter.setProjectId(testProject.getId());
        testChapter.setTitle("第一章：山门试剑");
        testChapter.setContent("");
        testChapter.setOrderNum(1);
        testChapter.setStatus(0);
        testChapter.setWordCount(0);
        chapterMapper.insert(testChapter);

        requiredCharacter = createCharacter("林渊");
        unrelatedCharacter = createCharacter("沈月");

        linkCharacterToProject(requiredCharacter, "主角");
        linkCharacterToProject(unrelatedCharacter, "对手");
        linkCharacterToChapter(requiredCharacter, true);

        createInventoryItem(requiredCharacter, "青锋剑", "剑修常用佩剑", 1, 1, 88, null);
        createInventoryItem(requiredCharacter, "止血散", "用于止血恢复", 3, 0, 100, "受伤时优先使用");
        createInventoryItem(unrelatedCharacter, "影刃", "不应进入本章 prompt", 1, 1, 100, null);

        AIProvider provider = new AIProvider();
        provider.setId(99L);
        provider.setName("mock-provider");
        provider.setEnabled(1);
        provider.setModelName("mock-model");

        when(aiModelRoutingService.resolve(any(), any(), any()))
                .thenReturn(new AIModelRoutingService.ResolvedModelSelection(provider, "mock-model"));
        when(aiWritingChatService.hasBackgroundContext(any(), any())).thenReturn(false);
        when(aiProviderService.generateText(
                any(),
                anyString(),
                anyString(),
                anyString(),
                nullable(Double.class),
                nullable(Integer.class)
        )).thenReturn(
                "1. 让林渊在试剑前确认装备状态",
                "林渊按住剑柄，先试了试呼吸与步伐。",
                "结论：通过\n摘要：背包约束已满足。\n问题：\n- 无"
        );
    }

    @Test
    void shouldIncludeRequiredCharacterInventoryInWritingPrompt() {
        AIWritingRequestDTO request = new AIWritingRequestDTO();
        request.setChapterId(testChapter.getId());
        request.setCurrentContent("");
        request.setWritingType("continue");
        request.setUserInstruction("围绕试剑前的准备写一段正文。");
        request.setPromptSnapshot("请保持人物动作和携带物一致。");

        AIWritingResponseVO response = aiWritingService.generateContent(testUser.getId(), request);

        assertNotNull(response);
        assertEquals("林渊按住剑柄，先试了试呼吸与步伐。", response.getGeneratedContent());

        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiProviderService, atLeast(2)).generateText(
                any(),
                anyString(),
                anyString(),
                userPromptCaptor.capture(),
                nullable(Double.class),
                nullable(Integer.class)
        );

        List<String> prompts = userPromptCaptor.getAllValues();
        assertTrue(prompts.stream().anyMatch(prompt ->
                prompt.contains("【章节人物背包】")
                        && prompt.contains("林渊：青锋剑 x1（已装备；耐久88；剑修常用佩剑）")
                        && prompt.contains("止血散 x3（受伤时优先使用）")
                        && !prompt.contains("沈月：")
                        && !prompt.contains("影刃")
        ));
    }

    private Character createCharacter(String name) {
        Character character = new Character();
        character.setOwnerUserId(testUser.getId());
        character.setName(name);
        character.setDescription(name + "的角色设定");
        character.setAttributes("{}");
        characterMapper.insert(character);
        return character;
    }

    private void linkCharacterToProject(Character character, String projectRole) {
        ProjectCharacterLink link = new ProjectCharacterLink();
        link.setProjectId(testProject.getId());
        link.setCharacterId(character.getId());
        link.setProjectRole(projectRole);
        projectCharacterMapper.insert(link);
    }

    private void linkCharacterToChapter(Character character, boolean required) {
        ChapterCharacterLink link = new ChapterCharacterLink();
        link.setChapterId(testChapter.getId());
        link.setCharacterId(character.getId());
        link.setRequiredFlag(required ? 1 : 0);
        chapterCharacterMapper.insert(link);
    }

    private void createInventoryItem(
            Character character,
            String itemName,
            String itemDescription,
            int quantity,
            int equipped,
            int durability,
            String notes) {
        ItemDefinition item = new ItemDefinition();
        item.setProjectId(testProject.getId());
        item.setOwnerUserId(testUser.getId());
        item.setName(itemName);
        item.setDescription(itemDescription);
        item.setCategory("prop");
        item.setRarity("common");
        item.setStackable(quantity > 1 ? 1 : 0);
        item.setMaxStack(quantity > 1 ? 20 : 1);
        item.setUsable(0);
        item.setEquippable(1);
        item.setSlotType("misc");
        item.setItemValue(0);
        item.setWeight(0);
        item.setAttributesJson("{}");
        item.setEffectJson("{}");
        item.setTags("");
        item.setSourceType("manual");
        itemMapper.insert(item);

        CharacterInventoryItem inventoryItem = new CharacterInventoryItem();
        inventoryItem.setProjectId(testProject.getId());
        inventoryItem.setCharacterId(character.getId());
        inventoryItem.setItemId(item.getId());
        inventoryItem.setQuantity(quantity);
        inventoryItem.setEquipped(equipped);
        inventoryItem.setDurability(durability);
        inventoryItem.setNotes(notes);
        inventoryItem.setSortOrder(0);
        characterInventoryItemMapper.insert(inventoryItem);
    }
}
