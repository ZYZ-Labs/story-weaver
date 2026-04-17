package com.storyweaver.storyunit.context.impl;

import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.ProjectCharacterLink;
import com.storyweaver.item.infrastructure.persistence.mapper.CharacterInventoryItemMapper;
import com.storyweaver.item.infrastructure.persistence.mapper.ItemMapper;
import com.storyweaver.repository.CharacterMapper;
import com.storyweaver.repository.ProjectCharacterMapper;
import com.storyweaver.storyunit.context.CharacterRuntimeStateView;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultCharacterRuntimeStateQueryServiceTest {

    @Test
    void shouldIgnoreNullStateTagsAndStillReturnRuntimeState() {
        CharacterMapper characterMapper = mock(CharacterMapper.class);
        ProjectCharacterMapper projectCharacterMapper = mock(ProjectCharacterMapper.class);
        CharacterInventoryItemMapper characterInventoryItemMapper = mock(CharacterInventoryItemMapper.class);
        ItemMapper itemMapper = mock(ItemMapper.class);

        Character character = new Character();
        character.setId(15L);
        character.setName("林沉舟");
        character.setDescription("沉寂两年的前职业选手");
        character.setCoreGoal(null);
        character.setGrowthArc(null);
        character.setActiveStage(null);
        character.setIsRetired(0);
        character.setDeleted(0);

        ProjectCharacterLink projectLink = new ProjectCharacterLink();
        projectLink.setProjectId(28L);
        projectLink.setCharacterId(15L);
        projectLink.setRoleType(null);
        projectLink.setProjectRole("主角");

        when(characterMapper.selectById(15L)).thenReturn(character);
        when(projectCharacterMapper.selectOne(any())).thenReturn(projectLink);
        when(characterInventoryItemMapper.selectList(any())).thenReturn(java.util.List.of());

        DefaultCharacterRuntimeStateQueryService service = new DefaultCharacterRuntimeStateQueryService(
                characterMapper,
                projectCharacterMapper,
                characterInventoryItemMapper,
                itemMapper
        );

        Optional<CharacterRuntimeStateView> result = service.getCharacterRuntimeState(28L, 15L);

        assertTrue(result.isPresent());
        assertEquals("林沉舟", result.orElseThrow().characterName());
        assertEquals("沉寂两年的前职业选手", result.orElseThrow().attitudeSummary());
        assertEquals(java.util.List.of("主角"), result.orElseThrow().stateTags());
    }
}
