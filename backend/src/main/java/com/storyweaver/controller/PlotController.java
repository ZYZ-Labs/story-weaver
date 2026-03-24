package com.storyweaver.controller;

import com.storyweaver.domain.entity.Plot;
import com.storyweaver.security.SecurityUtils;
import com.storyweaver.service.PlotCrudService;
import com.storyweaver.service.PlotService;
import com.storyweaver.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class PlotController {

    private final PlotService plotService;
    private final PlotCrudService plotCrudService;
    private final ProjectService projectService;

    public PlotController(PlotService plotService, PlotCrudService plotCrudService, ProjectService projectService) {
        this.plotService = plotService;
        this.plotCrudService = plotCrudService;
        this.projectService = projectService;
    }

    @GetMapping("/api/projects/{projectId}/plotlines")
    public ResponseEntity<Map<String, Object>> getProjectPlots(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "项目不存在或无权访问"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "获取成功", "data", plotService.getProjectPlots(projectId)));
    }

    @PostMapping("/api/projects/{projectId}/plotlines")
    public ResponseEntity<Map<String, Object>> createPlot(
            @PathVariable Long projectId,
            @RequestBody Plot plot,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        Plot created = plotCrudService.createPlot(projectId, userId, plot);
        if (created == null) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "项目不存在或无权访问"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "创建成功", "data", created));
    }

    @GetMapping("/api/plotlines/{id}")
    public ResponseEntity<Map<String, Object>> getPlot(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        Plot plot = plotCrudService.getPlot(id, userId);
        if (plot == null) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "情节不存在或无权访问"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "获取成功", "data", plot));
    }

    @PutMapping("/api/plotlines/{id}")
    public ResponseEntity<Map<String, Object>> updatePlot(
            @PathVariable Long id,
            @RequestBody Plot plot,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        if (!plotCrudService.updatePlot(id, userId, plot)) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "情节不存在或无权访问"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功"));
    }

    @DeleteMapping("/api/plotlines/{id}")
    public ResponseEntity<Map<String, Object>> deletePlot(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        if (!plotCrudService.deletePlot(id, userId)) {
            return ResponseEntity.status(404).body(Map.of("code", 404, "message", "情节不存在或无权访问"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }
}
