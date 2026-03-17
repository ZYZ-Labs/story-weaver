package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.entity.Character;

import java.util.List;

public interface CharacterService extends IService<Character> {
    List<Character> getProjectCharacters(Long projectId, Long userId);
    
    Character createCharacter(Long projectId, Long userId, String name, String description, String attributes);
    
    boolean updateCharacter(Long characterId, Long userId, Character character);
    
    boolean deleteCharacter(Long characterId, Long userId);
    
    Character getCharacterWithAuth(Long characterId, Long userId);
}