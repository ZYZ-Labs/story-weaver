package com.storyweaver.controller;

import com.storyweaver.domain.dto.CharacterAttributeSuggestionRequestDTO;
import com.storyweaver.domain.dto.CharacterRequestDTO;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.service.CharacterAttributeSuggestionService;
import com.storyweaver.service.CharacterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class CharacterController {

    private final CharacterService characterService;
    private final CharacterAttributeSuggestionService characterAttributeSuggestionService;

    public CharacterController(
            CharacterService characterService,
            CharacterAttributeSuggestionService characterAttributeSuggestionService) {
        this.characterService = characterService;
        this.characterAttributeSuggestionService = characterAttributeSuggestionService;
    }

    @GetMapping("/api/characters/library")
    public ResponseEntity<Map<String, Object>> listCharacterLibrary(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);

        List<Character> characters = characterService.listReusableCharacters(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", characters);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/projects/{projectId}/characters")
    public ResponseEntity<Map<String, Object>> getProjectCharacters(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);

        List<Character> characters = characterService.getProjectCharacters(projectId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", characters);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/projects/{projectId}/characters")
    public ResponseEntity<Map<String, Object>> createOrAttachCharacter(
            @PathVariable Long projectId,
            @RequestBody CharacterRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);

        Character character = requestDTO.getExistingCharacterId() != null
                ? characterService.attachCharacter(projectId, requestDTO.getExistingCharacterId(), userId, requestDTO.getProjectRole())
                : characterService.createCharacter(projectId, userId, requestDTO);

        if (character == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "人物不存在、项目不存在或无权访问"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", requestDTO.getExistingCharacterId() != null ? "关联成功" : "创建成功",
                "data", character
        ));
    }

    @PutMapping("/api/projects/{projectId}/characters/{characterId}")
    public ResponseEntity<Map<String, Object>> updateCharacter(
            @PathVariable Long projectId,
            @PathVariable Long characterId,
            @RequestBody CharacterRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);

        boolean success = characterService.updateCharacter(projectId, characterId, userId, requestDTO);
        if (!success) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "人物不存在、未关联当前项目或无权访问"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "更新成功"
        ));
    }

    @DeleteMapping("/api/projects/{projectId}/characters/{characterId}")
    public ResponseEntity<Map<String, Object>> deleteCharacter(
            @PathVariable Long projectId,
            @PathVariable Long characterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);

        boolean success = characterService.deleteCharacter(projectId, characterId, userId);
        if (!success) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "人物不存在、未关联当前项目或无权访问"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "已移出当前项目"
        ));
    }

    @PostMapping("/api/projects/{projectId}/characters/attribute-suggestions")
    public ResponseEntity<Map<String, Object>> generateCharacterAttributes(
            @PathVariable Long projectId,
            @RequestBody CharacterAttributeSuggestionRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", characterAttributeSuggestionService.generateAttributes(projectId, userId, requestDTO));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/projects/{projectId}/characters/{characterId}")
    public ResponseEntity<Map<String, Object>> getCharacter(
            @PathVariable Long projectId,
            @PathVariable Long characterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);

        List<Character> projectCharacters = characterService.getProjectCharacters(projectId, userId);
        Character character = projectCharacters.stream()
                .filter(item -> characterId.equals(item.getId()))
                .findFirst()
                .orElse(null);
        if (character == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "人物不存在、未关联当前项目或无权访问"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "获取成功",
                "data", character
        ));
    }
}
