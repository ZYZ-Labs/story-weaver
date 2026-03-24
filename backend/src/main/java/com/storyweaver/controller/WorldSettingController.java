package com.storyweaver.controller;

import com.storyweaver.domain.dto.WorldSettingDTO;
import com.storyweaver.domain.vo.WorldSettingVO;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.service.ProjectService;
import com.storyweaver.service.WorldSettingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/world-settings")
public class WorldSettingController {

    private final WorldSettingService worldSettingService;
    private final ProjectService projectService;

    public WorldSettingController(WorldSettingService worldSettingService, ProjectService projectService) {
        this.worldSettingService = worldSettingService;
        this.projectService = projectService;
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<WorldSettingVO>> getWorldSettingsByProjectId(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return ResponseEntity.status(404).build();
        }

        return ResponseEntity.ok(worldSettingService.getWorldSettingsByProjectId(projectId));
    }

    @GetMapping("/library")
    public ResponseEntity<List<WorldSettingVO>> getWorldSettingLibrary(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        return ResponseEntity.ok(worldSettingService.listLibraryWorldSettings(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorldSettingVO> getWorldSettingById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        WorldSettingVO worldSettingVO = worldSettingService.getWorldSettingById(id, userId);
        return worldSettingVO == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(worldSettingVO);
    }

    @PostMapping
    public ResponseEntity<WorldSettingVO> createWorldSetting(
            @Validated @RequestBody WorldSettingDTO worldSettingDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        if (!projectService.hasProjectAccess(worldSettingDTO.getProjectId(), userId)) {
            return ResponseEntity.status(404).build();
        }

        return ResponseEntity.ok(worldSettingService.createWorldSetting(worldSettingDTO, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorldSettingVO> updateWorldSetting(
            @PathVariable Long id,
            @Validated @RequestBody WorldSettingDTO worldSettingDTO,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        if (!worldSettingService.hasAccess(id, userId)) {
            return ResponseEntity.notFound().build();
        }

        WorldSettingVO updatedWorldSetting = worldSettingService.updateWorldSetting(id, worldSettingDTO, userId);
        return updatedWorldSetting == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updatedWorldSetting);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorldSetting(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        if (!worldSettingService.hasAccess(id, userId)) {
            return ResponseEntity.notFound().build();
        }

        worldSettingService.deleteWorldSetting(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/projects/{projectId}")
    public ResponseEntity<Void> attachWorldSettingToProject(
            @PathVariable Long id,
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return ResponseEntity.status(404).build();
        }

        return worldSettingService.attachWorldSettingToProject(id, projectId, userId)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}/projects/{projectId}")
    public ResponseEntity<Void> detachWorldSettingFromProject(
            @PathVariable Long id,
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return ResponseEntity.status(404).build();
        }

        return worldSettingService.detachWorldSettingFromProject(id, projectId, userId)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }
}
