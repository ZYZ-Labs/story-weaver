package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.dto.CharacterRequestDTO;
import com.storyweaver.domain.entity.Character;

import java.util.List;

public interface CharacterService extends IService<Character> {
    List<Character> getProjectCharacters(Long projectId, Long userId);

    List<Character> listReusableCharacters(Long userId);

    Character createCharacter(Long projectId, Long userId, CharacterRequestDTO requestDTO);

    Character attachCharacter(Long projectId, Long characterId, Long userId, String projectRole);

    boolean updateCharacter(Long projectId, Long characterId, Long userId, CharacterRequestDTO requestDTO);

    boolean deleteCharacter(Long projectId, Long characterId, Long userId);

    Character getCharacterWithAuth(Long characterId, Long userId);
}
