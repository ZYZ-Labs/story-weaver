package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.domain.dto.CharacterRequestDTO;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.entity.ProjectCharacterLink;
import com.storyweaver.repository.ChapterCharacterMapper;
import com.storyweaver.repository.ChapterMapper;
import com.storyweaver.repository.CharacterMapper;
import com.storyweaver.repository.ProjectCharacterMapper;
import com.storyweaver.repository.ProjectMapper;
import com.storyweaver.service.CharacterService;
import com.storyweaver.service.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CharacterServiceImpl extends ServiceImpl<CharacterMapper, Character> implements CharacterService {

    private static final String DEFAULT_PROJECT_ROLE = "配角";

    private final ProjectService projectService;
    private final ProjectMapper projectMapper;
    private final ProjectCharacterMapper projectCharacterMapper;
    private final ChapterCharacterMapper chapterCharacterMapper;
    private final ChapterMapper chapterMapper;
    private final ObjectMapper objectMapper;

    public CharacterServiceImpl(
            ProjectService projectService,
            ProjectMapper projectMapper,
            ProjectCharacterMapper projectCharacterMapper,
            ChapterCharacterMapper chapterCharacterMapper,
            ChapterMapper chapterMapper,
            ObjectMapper objectMapper) {
        this.projectService = projectService;
        this.projectMapper = projectMapper;
        this.projectCharacterMapper = projectCharacterMapper;
        this.chapterCharacterMapper = chapterCharacterMapper;
        this.chapterMapper = chapterMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Character> getProjectCharacters(Long projectId, Long userId) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return List.of();
        }

        List<ProjectCharacterLink> links = projectCharacterMapper.selectList(
                new LambdaQueryWrapper<ProjectCharacterLink>()
                        .eq(ProjectCharacterLink::getProjectId, projectId)
                        .orderByAsc(ProjectCharacterLink::getId)
        );
        if (links.isEmpty()) {
            return List.of();
        }

        Map<Long, Character> characterMap = listActiveCharacters(
                links.stream().map(ProjectCharacterLink::getCharacterId).toList()
        ).stream().collect(Collectors.toMap(Character::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<Character> result = new ArrayList<>();
        for (ProjectCharacterLink link : links) {
            Character character = characterMap.get(link.getCharacterId());
            if (character != null) {
                character.setProjectRole(normalizeProjectRole(link.getProjectRole()));
                result.add(character);
            }
        }

        attachProjectSummaries(result);
        return result;
    }

    @Override
    public List<Character> listReusableCharacters(Long userId) {
        List<Character> characters = list(new LambdaQueryWrapper<Character>()
                .eq(Character::getOwnerUserId, userId)
                .eq(Character::getDeleted, 0)
                .orderByDesc(Character::getUpdateTime));
        attachProjectSummaries(characters);
        return characters;
    }

    @Override
    @Transactional
    public Character createCharacter(Long projectId, Long userId, CharacterRequestDTO requestDTO) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return null;
        }

        if (!StringUtils.hasText(requestDTO.getName())) {
            throw new IllegalArgumentException("人物名称不能为空");
        }

        Character character = new Character();
        character.setProjectId(projectId);
        character.setOwnerUserId(userId);
        character.setName(requestDTO.getName().trim());
        character.setDescription(trimToNull(requestDTO.getDescription()));
        character.setAttributes(normalizeAttributes(requestDTO.getAttributes()));
        save(character);

        upsertProjectLink(projectId, character.getId(), requestDTO.getProjectRole());
        return hydrateProjectCharacter(character.getId(), projectId);
    }

    @Override
    @Transactional
    public Character attachCharacter(Long projectId, Long characterId, Long userId, String projectRole) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return null;
        }

        Character character = getCharacterWithAuth(characterId, userId);
        if (character == null) {
            return null;
        }

        upsertProjectLink(projectId, characterId, projectRole);
        return hydrateProjectCharacter(characterId, projectId);
    }

    @Override
    @Transactional
    public boolean updateCharacter(Long projectId, Long characterId, Long userId, CharacterRequestDTO requestDTO) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return false;
        }

        Character existing = getCharacterWithAuth(characterId, userId);
        if (existing == null || findProjectLink(projectId, characterId) == null) {
            return false;
        }

        if (StringUtils.hasText(requestDTO.getName())) {
            existing.setName(requestDTO.getName().trim());
        }
        existing.setDescription(trimToNull(requestDTO.getDescription()));
        existing.setAttributes(normalizeAttributes(requestDTO.getAttributes()));
        updateById(existing);
        upsertProjectLink(projectId, characterId, requestDTO.getProjectRole());
        return true;
    }

    @Override
    @Transactional
    public boolean deleteCharacter(Long projectId, Long characterId, Long userId) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return false;
        }

        Character character = getCharacterWithAuth(characterId, userId);
        if (character == null) {
            return false;
        }

        ProjectCharacterLink link = findProjectLink(projectId, characterId);
        if (link == null) {
            return false;
        }

        projectCharacterMapper.deleteById(link.getId());

        List<Long> chapterIds = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                        .eq(Chapter::getProjectId, projectId)
                        .eq(Chapter::getDeleted, 0))
                .stream()
                .map(Chapter::getId)
                .toList();
        if (!chapterIds.isEmpty()) {
            chapterCharacterMapper.delete(new QueryWrapper<com.storyweaver.domain.entity.ChapterCharacterLink>()
                    .eq("character_id", characterId)
                    .in("chapter_id", chapterIds));
        }

        return true;
    }

    @Override
    public Character getCharacterWithAuth(Long characterId, Long userId) {
        Character character = getById(characterId);
        if (character == null || Integer.valueOf(1).equals(character.getDeleted())) {
            return null;
        }
        return Objects.equals(character.getOwnerUserId(), userId) ? character : null;
    }

    private Character hydrateProjectCharacter(Long characterId, Long projectId) {
        Character character = getById(characterId);
        if (character == null) {
            return null;
        }
        attachProjectSummaries(List.of(character));
        ProjectCharacterLink link = findProjectLink(projectId, characterId);
        character.setProjectRole(link == null ? DEFAULT_PROJECT_ROLE : normalizeProjectRole(link.getProjectRole()));
        return character;
    }

    private void attachProjectSummaries(List<Character> characters) {
        if (characters == null || characters.isEmpty()) {
            return;
        }

        List<Long> characterIds = characters.stream()
                .map(Character::getId)
                .filter(Objects::nonNull)
                .toList();
        if (characterIds.isEmpty()) {
            return;
        }

        List<ProjectCharacterLink> links = projectCharacterMapper.selectList(new QueryWrapper<ProjectCharacterLink>()
                .in("character_id", characterIds)
                .orderByAsc("id"));

        Map<Long, List<Long>> projectIdsByCharacter = new LinkedHashMap<>();
        Set<Long> projectIds = new LinkedHashSet<>();
        for (ProjectCharacterLink link : links) {
            if (link.getCharacterId() == null || link.getProjectId() == null) {
                continue;
            }
            projectIdsByCharacter.computeIfAbsent(link.getCharacterId(), key -> new ArrayList<>()).add(link.getProjectId());
            projectIds.add(link.getProjectId());
        }

        Map<Long, Project> projectMap = projectIds.isEmpty()
                ? Map.of()
                : projectMapper.selectBatchIds(projectIds).stream()
                        .filter(project -> project != null && !Integer.valueOf(1).equals(project.getDeleted()))
                        .collect(Collectors.toMap(Project::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        for (Character character : characters) {
            List<Long> linkedProjectIds = projectIdsByCharacter.getOrDefault(character.getId(), List.of()).stream()
                    .filter(projectMap::containsKey)
                    .toList();
            character.setProjectIds(linkedProjectIds);
            character.setProjectNames(linkedProjectIds.stream()
                    .map(projectMap::get)
                    .map(Project::getName)
                    .filter(StringUtils::hasText)
                    .toList());
        }
    }

    private List<Character> listActiveCharacters(List<Long> characterIds) {
        if (characterIds == null || characterIds.isEmpty()) {
            return List.of();
        }

        return list(new LambdaQueryWrapper<Character>()
                .in(Character::getId, characterIds)
                .eq(Character::getDeleted, 0)
                .orderByDesc(Character::getUpdateTime));
    }

    private ProjectCharacterLink findProjectLink(Long projectId, Long characterId) {
        return projectCharacterMapper.selectOne(new QueryWrapper<ProjectCharacterLink>()
                .eq("project_id", projectId)
                .eq("character_id", characterId));
    }

    private void upsertProjectLink(Long projectId, Long characterId, String projectRole) {
        ProjectCharacterLink existingLink = findProjectLink(projectId, characterId);
        if (existingLink == null) {
            ProjectCharacterLink link = new ProjectCharacterLink();
            link.setProjectId(projectId);
            link.setCharacterId(characterId);
            link.setProjectRole(normalizeProjectRole(projectRole));
            projectCharacterMapper.insert(link);
            return;
        }

        existingLink.setProjectRole(normalizeProjectRole(projectRole));
        projectCharacterMapper.updateById(existingLink);
    }

    private String normalizeProjectRole(String projectRole) {
        return StringUtils.hasText(projectRole) ? projectRole.trim() : DEFAULT_PROJECT_ROLE;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeAttributes(String attributes) {
        if (!StringUtils.hasText(attributes)) {
            return "{}";
        }

        try {
            objectMapper.readTree(attributes);
            return attributes.trim();
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }
}
