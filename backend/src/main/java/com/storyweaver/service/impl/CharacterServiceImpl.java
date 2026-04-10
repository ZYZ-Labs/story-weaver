package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.domain.dto.CharacterRequestDTO;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.entity.ProjectCharacterLink;
import com.storyweaver.item.domain.entity.CharacterInventoryItem;
import com.storyweaver.item.domain.entity.ItemDefinition;
import com.storyweaver.item.domain.support.ItemCatalogRules;
import com.storyweaver.item.infrastructure.persistence.mapper.CharacterInventoryItemMapper;
import com.storyweaver.item.infrastructure.persistence.mapper.ItemMapper;
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
    private final CharacterInventoryItemMapper characterInventoryItemMapper;
    private final ItemMapper itemMapper;
    private final ObjectMapper objectMapper;

    public CharacterServiceImpl(
            ProjectService projectService,
            ProjectMapper projectMapper,
            ProjectCharacterMapper projectCharacterMapper,
            ChapterCharacterMapper chapterCharacterMapper,
            ChapterMapper chapterMapper,
            CharacterInventoryItemMapper characterInventoryItemMapper,
            ItemMapper itemMapper,
            ObjectMapper objectMapper) {
        this.projectService = projectService;
        this.projectMapper = projectMapper;
        this.projectCharacterMapper = projectCharacterMapper;
        this.chapterCharacterMapper = chapterCharacterMapper;
        this.chapterMapper = chapterMapper;
        this.characterInventoryItemMapper = characterInventoryItemMapper;
        this.itemMapper = itemMapper;
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
                String resolvedRoleType = resolveRoleType(link.getRoleType(), link.getProjectRole());
                character.setProjectRole(resolvedRoleType);
                character.setRoleType(resolvedRoleType);
                result.add(character);
            }
        }

        attachProjectSummaries(result);
        attachInventorySummaries(projectId, result);
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
        applyCharacterRequest(character, requestDTO);
        save(character);

        upsertProjectLink(projectId, character.getId(), resolveRoleType(requestDTO.getRoleType(), requestDTO.getProjectRole()));
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

        applyCharacterRequest(existing, requestDTO);
        updateById(existing);
        upsertProjectLink(projectId, characterId, resolveRoleType(requestDTO.getRoleType(), requestDTO.getProjectRole()));
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

        characterInventoryItemMapper.delete(new LambdaQueryWrapper<CharacterInventoryItem>()
                .eq(CharacterInventoryItem::getProjectId, projectId)
                .eq(CharacterInventoryItem::getCharacterId, characterId)
                .eq(CharacterInventoryItem::getDeleted, 0));

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
        String resolvedRoleType = link == null ? DEFAULT_PROJECT_ROLE : resolveRoleType(link.getRoleType(), link.getProjectRole());
        character.setProjectRole(resolvedRoleType);
        character.setRoleType(resolvedRoleType);
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

    private void attachInventorySummaries(Long projectId, List<Character> characters) {
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

        List<CharacterInventoryItem> inventoryItems = characterInventoryItemMapper.selectList(
                new LambdaQueryWrapper<CharacterInventoryItem>()
                        .eq(CharacterInventoryItem::getProjectId, projectId)
                        .in(CharacterInventoryItem::getCharacterId, characterIds)
                        .eq(CharacterInventoryItem::getDeleted, 0)
        );
        if (inventoryItems.isEmpty()) {
            for (Character character : characters) {
                character.setInventoryItemCount(0);
                character.setEquippedItemCount(0);
                character.setRareItemCount(0);
            }
            return;
        }

        List<Long> itemIds = inventoryItems.stream()
                .map(CharacterInventoryItem::getItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ItemDefinition> itemMap = itemIds.isEmpty()
                ? Map.of()
                : itemMapper.selectBatchIds(itemIds).stream()
                        .filter(item -> item != null && !Integer.valueOf(1).equals(item.getDeleted()))
                        .collect(Collectors.toMap(ItemDefinition::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        Map<Long, Integer> totalCountByCharacter = new LinkedHashMap<>();
        Map<Long, Integer> equippedCountByCharacter = new LinkedHashMap<>();
        Map<Long, Integer> rareCountByCharacter = new LinkedHashMap<>();
        for (CharacterInventoryItem inventoryItem : inventoryItems) {
            if (inventoryItem.getCharacterId() == null) {
                continue;
            }
            int quantity = inventoryItem.getQuantity() == null ? 1 : Math.max(1, inventoryItem.getQuantity());
            totalCountByCharacter.merge(inventoryItem.getCharacterId(), quantity, Integer::sum);
            if (Integer.valueOf(1).equals(inventoryItem.getEquipped())) {
                equippedCountByCharacter.merge(inventoryItem.getCharacterId(), quantity, Integer::sum);
            }
            ItemDefinition item = itemMap.get(inventoryItem.getItemId());
            if (item != null && ItemCatalogRules.isRare(item.getRarity())) {
                rareCountByCharacter.merge(inventoryItem.getCharacterId(), quantity, Integer::sum);
            }
        }

        for (Character character : characters) {
            character.setInventoryItemCount(totalCountByCharacter.getOrDefault(character.getId(), 0));
            character.setEquippedItemCount(equippedCountByCharacter.getOrDefault(character.getId(), 0));
            character.setRareItemCount(rareCountByCharacter.getOrDefault(character.getId(), 0));
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
        String resolvedRoleType = normalizeProjectRole(projectRole);
        ProjectCharacterLink existingLink = findProjectLink(projectId, characterId);
        if (existingLink == null) {
            ProjectCharacterLink link = new ProjectCharacterLink();
            link.setProjectId(projectId);
            link.setCharacterId(characterId);
            link.setProjectRole(resolvedRoleType);
            link.setRoleType(resolvedRoleType);
            projectCharacterMapper.insert(link);
            return;
        }

        existingLink.setProjectRole(resolvedRoleType);
        existingLink.setRoleType(resolvedRoleType);
        projectCharacterMapper.updateById(existingLink);
    }

    private String normalizeProjectRole(String projectRole) {
        return StringUtils.hasText(projectRole) ? projectRole.trim() : DEFAULT_PROJECT_ROLE;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void applyCharacterRequest(Character character, CharacterRequestDTO requestDTO) {
        if (StringUtils.hasText(requestDTO.getName())) {
            character.setName(requestDTO.getName().trim());
        }
        character.setDescription(trimToNull(requestDTO.getDescription()));

        String normalizedAttributes = requestDTO.getAttributes() != null
                ? normalizeAttributes(requestDTO.getAttributes())
                : defaultJson(character.getAttributes());
        String normalizedAdvancedProfile = requestDTO.getAdvancedProfileJson() != null
                ? normalizeAttributes(requestDTO.getAdvancedProfileJson())
                : defaultJson(StringUtils.hasText(character.getAdvancedProfileJson()) ? character.getAdvancedProfileJson() : normalizedAttributes);

        String identityFallback = firstNonBlank(
                character.getIdentity(),
                readJsonString(normalizedAdvancedProfile, "identity"),
                readJsonString(normalizedAdvancedProfile, "身份"),
                readJsonString(normalizedAttributes, "身份")
        );
        String coreGoalFallback = firstNonBlank(
                character.getCoreGoal(),
                readJsonString(normalizedAdvancedProfile, "coreGoal"),
                readJsonString(normalizedAdvancedProfile, "目标"),
                readJsonString(normalizedAttributes, "目标")
        );
        String growthArcFallback = firstNonBlank(
                character.getGrowthArc(),
                readJsonString(normalizedAdvancedProfile, "growthArc"),
                readJsonString(normalizedAdvancedProfile, "成长弧线")
        );
        String activeStageFallback = firstNonBlank(
                character.getActiveStage(),
                readJsonString(normalizedAdvancedProfile, "activeStage"),
                readJsonString(normalizedAdvancedProfile, "当前阶段")
        );

        String identity = requestDTO.getIdentity() != null ? trimToNull(requestDTO.getIdentity()) : identityFallback;
        String coreGoal = requestDTO.getCoreGoal() != null ? trimToNull(requestDTO.getCoreGoal()) : coreGoalFallback;
        String growthArc = requestDTO.getGrowthArc() != null ? trimToNull(requestDTO.getGrowthArc()) : growthArcFallback;
        String activeStage = requestDTO.getActiveStage() != null ? trimToNull(requestDTO.getActiveStage()) : activeStageFallback;
        Long firstAppearanceChapterId = requestDTO.getFirstAppearanceChapterId() != null
                ? requestDTO.getFirstAppearanceChapterId()
                : character.getFirstAppearanceChapterId();
        Integer isRetired = requestDTO.getIsRetired() != null
                ? (requestDTO.getIsRetired() ? 1 : 0)
                : (character.getIsRetired() == null ? 0 : character.getIsRetired());

        character.setIdentity(identity);
        character.setCoreGoal(coreGoal);
        character.setGrowthArc(growthArc);
        character.setFirstAppearanceChapterId(firstAppearanceChapterId);
        character.setActiveStage(activeStage);
        character.setIsRetired(isRetired);
        character.setAttributes(mergeLegacyAttributes(normalizedAttributes, identity, coreGoal, growthArc, activeStage, isRetired));
        character.setAdvancedProfileJson(mergeAdvancedProfile(
                normalizedAdvancedProfile,
                identity,
                coreGoal,
                growthArc,
                firstAppearanceChapterId,
                activeStage,
                isRetired
        ));
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

    private String defaultJson(String rawJson) {
        return StringUtils.hasText(rawJson) ? normalizeAttributes(rawJson) : "{}";
    }

    private String resolveRoleType(String roleType, String projectRole) {
        if (StringUtils.hasText(roleType)) {
            return normalizeProjectRole(roleType);
        }
        return normalizeProjectRole(projectRole);
    }

    private String readJsonString(String rawJson, String key) {
        if (!StringUtils.hasText(rawJson) || !StringUtils.hasText(key)) {
            return null;
        }
        try {
            Map<String, Object> values = objectMapper.readValue(rawJson, new TypeReference<Map<String, Object>>() {});
            Object value = values.get(key);
            if (value == null) {
                return null;
            }
            String text = String.valueOf(value).trim();
            return text.isEmpty() ? null : text;
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private String mergeLegacyAttributes(
            String baseJson,
            String identity,
            String coreGoal,
            String growthArc,
            String activeStage,
            Integer isRetired) {
        Map<String, Object> values = toMutableMap(baseJson);
        putOrRemove(values, "身份", identity);
        putOrRemove(values, "目标", coreGoal);
        putOrRemove(values, "成长弧线", growthArc);
        putOrRemove(values, "当前阶段", activeStage);
        putOrRemove(values, "是否退场", isRetired == null ? null : isRetired);
        return writeJson(values);
    }

    private String mergeAdvancedProfile(
            String baseJson,
            String identity,
            String coreGoal,
            String growthArc,
            Long firstAppearanceChapterId,
            String activeStage,
            Integer isRetired) {
        Map<String, Object> values = toMutableMap(baseJson);
        putOrRemove(values, "identity", identity);
        putOrRemove(values, "coreGoal", coreGoal);
        putOrRemove(values, "growthArc", growthArc);
        putOrRemove(values, "firstAppearanceChapterId", firstAppearanceChapterId);
        putOrRemove(values, "activeStage", activeStage);
        putOrRemove(values, "isRetired", isRetired == null ? null : isRetired == 1);
        return writeJson(values);
    }

    private Map<String, Object> toMutableMap(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return new LinkedHashMap<>();
        }
        try {
            return new LinkedHashMap<>(objectMapper.readValue(rawJson, new TypeReference<Map<String, Object>>() {}));
        } catch (JsonProcessingException exception) {
            return new LinkedHashMap<>();
        }
    }

    private void putOrRemove(Map<String, Object> values, String key, Object value) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        if (value == null) {
            values.remove(key);
            return;
        }
        if (value instanceof String textValue) {
            if (StringUtils.hasText(textValue)) {
                values.put(key, textValue.trim());
            } else {
                values.remove(key);
            }
            return;
        }
        values.put(key, value);
    }

    private String writeJson(Map<String, Object> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
