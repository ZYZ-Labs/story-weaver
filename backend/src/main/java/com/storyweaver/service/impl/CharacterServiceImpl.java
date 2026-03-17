package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.repository.CharacterMapper;
import com.storyweaver.service.CharacterService;
import com.storyweaver.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CharacterServiceImpl extends ServiceImpl<CharacterMapper, Character> implements CharacterService {
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<Character> getProjectCharacters(Long projectId, Long userId) {
        // 验证用户是否有权限访问该项目
        QueryWrapper<Project> projectQuery = new QueryWrapper<>();
        projectQuery.eq("id", projectId)
                   .eq("user_id", userId)
                   .eq("deleted", 0);
        
        Project project = projectService.getOne(projectQuery);
        if (project == null) {
            return List.of();
        }
        
        QueryWrapper<Character> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id", projectId)
                   .eq("deleted", 0)
                   .orderByAsc("create_time");
        
        return list(queryWrapper);
    }

    @Override
    public Character createCharacter(Long projectId, Long userId, String name, String description, String attributes) {
        // 验证用户是否有权限访问该项目
        QueryWrapper<Project> projectQuery = new QueryWrapper<>();
        projectQuery.eq("id", projectId)
                   .eq("user_id", userId)
                   .eq("deleted", 0);
        
        Project project = projectService.getOne(projectQuery);
        if (project == null) {
            return null;
        }
        
        Character character = new Character();
        character.setProjectId(projectId);
        character.setName(name);
        character.setDescription(description);
        
        // 验证 JSON 格式
        if (attributes != null && !attributes.trim().isEmpty()) {
            try {
                objectMapper.readTree(attributes);
                character.setAttributes(attributes);
            } catch (JsonProcessingException e) {
                // 如果 JSON 无效，设置为空对象
                character.setAttributes("{}");
            }
        } else {
            character.setAttributes("{}");
        }
        
        save(character);
        return character;
    }

    @Override
    public boolean updateCharacter(Long characterId, Long userId, Character character) {
        Character existing = getCharacterWithAuth(characterId, userId);
        if (existing == null) {
            return false;
        }
        
        existing.setName(character.getName());
        existing.setDescription(character.getDescription());
        
        // 验证 JSON 格式
        if (character.getAttributes() != null && !character.getAttributes().trim().isEmpty()) {
            try {
                objectMapper.readTree(character.getAttributes());
                existing.setAttributes(character.getAttributes());
            } catch (JsonProcessingException e) {
                // 如果 JSON 无效，保持原样
            }
        }
        
        return updateById(existing);
    }

    @Override
    public boolean deleteCharacter(Long characterId, Long userId) {
        Character character = getCharacterWithAuth(characterId, userId);
        if (character == null) {
            return false;
        }
        
        return removeById(characterId);
    }

    @Override
    public Character getCharacterWithAuth(Long characterId, Long userId) {
        QueryWrapper<Character> characterQuery = new QueryWrapper<>();
        characterQuery.eq("id", characterId)
                     .eq("deleted", 0);
        
        Character character = getOne(characterQuery);
        if (character == null) {
            return null;
        }
        
        // 验证用户是否有权限访问该项目
        QueryWrapper<Project> projectQuery = new QueryWrapper<>();
        projectQuery.eq("id", character.getProjectId())
                   .eq("user_id", userId)
                   .eq("deleted", 0);
        
        Project project = projectService.getOne(projectQuery);
        if (project == null) {
            return null;
        }
        
        return character;
    }
}