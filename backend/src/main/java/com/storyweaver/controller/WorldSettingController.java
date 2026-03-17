package com.storyweaver.controller;

import com.storyweaver.domain.dto.WorldSettingDTO;
import com.storyweaver.domain.vo.WorldSettingVO;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.service.WorldSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/world-settings")
public class WorldSettingController {

    @Autowired
    private WorldSettingService worldSettingService;

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<WorldSettingVO>> getWorldSettingsByProjectId(
            @PathVariable Long projectId,
            Authentication authentication) {
        SecurityUtils.getCurrentUserId(authentication);
        List<WorldSettingVO> worldSettings = worldSettingService.getWorldSettingsByProjectId(projectId);
        return ResponseEntity.ok(worldSettings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorldSettingVO> getWorldSettingById(
            @PathVariable Long id,
            Authentication authentication) {
        SecurityUtils.getCurrentUserId(authentication);
        WorldSettingVO worldSetting = worldSettingService.getWorldSettingById(id);
        if (worldSetting == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(worldSetting);
    }

    @PostMapping
    public ResponseEntity<WorldSettingVO> createWorldSetting(
            @Validated @RequestBody WorldSettingDTO worldSettingDTO,
            Authentication authentication) {
        SecurityUtils.getCurrentUserId(authentication);
        WorldSettingVO createdWorldSetting = worldSettingService.createWorldSetting(worldSettingDTO);
        return ResponseEntity.ok(createdWorldSetting);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorldSettingVO> updateWorldSetting(
            @PathVariable Long id,
            @Validated @RequestBody WorldSettingDTO worldSettingDTO,
            Authentication authentication) {
        SecurityUtils.getCurrentUserId(authentication);
        WorldSettingVO updatedWorldSetting = worldSettingService.updateWorldSetting(id, worldSettingDTO);
        if (updatedWorldSetting == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updatedWorldSetting);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorldSetting(
            @PathVariable Long id,
            Authentication authentication) {
        SecurityUtils.getCurrentUserId(authentication);
        worldSettingService.deleteWorldSetting(id);
        return ResponseEntity.ok().build();
    }
}
