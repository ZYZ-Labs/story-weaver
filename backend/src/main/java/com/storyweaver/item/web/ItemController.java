package com.storyweaver.item.web;

import com.storyweaver.common.web.ApiResponse;
import com.storyweaver.controller.AuthHeaderSupport;
import com.storyweaver.item.application.InventoryApplicationService;
import com.storyweaver.item.application.ItemApplicationService;
import com.storyweaver.item.application.ItemGenerationApplicationService;
import com.storyweaver.item.domain.entity.ItemDefinition;
import com.storyweaver.item.web.request.ItemGenerationRequest;
import com.storyweaver.item.web.request.ItemRequest;
import com.storyweaver.item.web.response.ItemGenerationResultResponse;
import com.storyweaver.item.web.response.ItemResponse;
import com.storyweaver.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ItemController {

    private final ItemApplicationService itemApplicationService;
    private final ItemGenerationApplicationService itemGenerationApplicationService;
    private final InventoryApplicationService inventoryApplicationService;

    public ItemController(
            ItemApplicationService itemApplicationService,
            ItemGenerationApplicationService itemGenerationApplicationService,
            InventoryApplicationService inventoryApplicationService) {
        this.itemApplicationService = itemApplicationService;
        this.itemGenerationApplicationService = itemGenerationApplicationService;
        this.inventoryApplicationService = inventoryApplicationService;
    }

    @GetMapping("/api/projects/{projectId}/items")
    public ResponseEntity<?> listProjectItems(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        List<ItemResponse> items = itemApplicationService.listProjectItems(projectId, userId).stream()
                .map(ItemResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("获取成功", items));
    }

    @PostMapping("/api/projects/{projectId}/items")
    public ResponseEntity<?> createItem(
            @PathVariable Long projectId,
            @RequestBody ItemRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        ItemDefinition item = itemApplicationService.createItem(projectId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("创建成功", ItemResponse.from(item)));
    }

    @PutMapping("/api/projects/{projectId}/items/{itemId}")
    public ResponseEntity<?> updateItem(
            @PathVariable Long projectId,
            @PathVariable Long itemId,
            @RequestBody ItemRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        ItemDefinition item = itemApplicationService.updateItem(projectId, itemId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("更新成功", ItemResponse.from(item)));
    }

    @DeleteMapping("/api/projects/{projectId}/items/{itemId}")
    public ResponseEntity<?> deleteItem(
            @PathVariable Long projectId,
            @PathVariable Long itemId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        itemApplicationService.deleteItem(projectId, itemId, userId);
        inventoryApplicationService.deleteByItemId(itemId);
        return ResponseEntity.ok(ApiResponse.success("删除成功"));
    }

    @PostMapping("/api/projects/{projectId}/items/generate")
    public ResponseEntity<?> generateItems(
            @PathVariable Long projectId,
            @RequestBody ItemGenerationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        ItemGenerationResultResponse result = itemGenerationApplicationService.generate(projectId, null, userId, request);
        List<ItemResponse> createdItems = result.getItems().stream()
                .map(item -> itemApplicationService.createGeneratedItem(projectId, userId, item))
                .map(ItemResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("生成成功", createdItems));
    }
}
