package com.storyweaver.item.web;

import com.storyweaver.common.web.ApiResponse;
import com.storyweaver.controller.AuthHeaderSupport;
import com.storyweaver.item.application.InventoryApplicationService;
import com.storyweaver.item.application.ItemApplicationService;
import com.storyweaver.item.application.ItemGenerationApplicationService;
import com.storyweaver.item.domain.entity.CharacterInventoryItem;
import com.storyweaver.item.domain.entity.ItemDefinition;
import com.storyweaver.item.web.request.InventoryItemRequest;
import com.storyweaver.item.web.request.ItemGenerationRequest;
import com.storyweaver.item.web.response.CharacterInventoryItemResponse;
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
public class CharacterInventoryController {

    private final InventoryApplicationService inventoryApplicationService;
    private final ItemApplicationService itemApplicationService;
    private final ItemGenerationApplicationService itemGenerationApplicationService;

    public CharacterInventoryController(
            InventoryApplicationService inventoryApplicationService,
            ItemApplicationService itemApplicationService,
            ItemGenerationApplicationService itemGenerationApplicationService) {
        this.inventoryApplicationService = inventoryApplicationService;
        this.itemApplicationService = itemApplicationService;
        this.itemGenerationApplicationService = itemGenerationApplicationService;
    }

    @GetMapping("/api/projects/{projectId}/characters/{characterId}/inventory")
    public ResponseEntity<?> listInventory(
            @PathVariable Long projectId,
            @PathVariable Long characterId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        List<CharacterInventoryItemResponse> inventory = inventoryApplicationService.listInventory(projectId, characterId, userId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("获取成功", inventory));
    }

    @PostMapping("/api/projects/{projectId}/characters/{characterId}/inventory")
    public ResponseEntity<?> addInventoryItem(
            @PathVariable Long projectId,
            @PathVariable Long characterId,
            @RequestBody InventoryItemRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        CharacterInventoryItem inventoryItem = inventoryApplicationService.addInventoryItem(projectId, characterId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("加入背包成功", toResponse(inventoryItem)));
    }

    @PutMapping("/api/projects/{projectId}/characters/{characterId}/inventory/{inventoryItemId}")
    public ResponseEntity<?> updateInventoryItem(
            @PathVariable Long projectId,
            @PathVariable Long characterId,
            @PathVariable Long inventoryItemId,
            @RequestBody InventoryItemRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        CharacterInventoryItem inventoryItem = inventoryApplicationService.updateInventoryItem(
                projectId,
                characterId,
                inventoryItemId,
                userId,
                request
        );
        return ResponseEntity.ok(ApiResponse.success("更新成功", toResponse(inventoryItem)));
    }

    @DeleteMapping("/api/projects/{projectId}/characters/{characterId}/inventory/{inventoryItemId}")
    public ResponseEntity<?> deleteInventoryItem(
            @PathVariable Long projectId,
            @PathVariable Long characterId,
            @PathVariable Long inventoryItemId,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }
        Long userId = SecurityUtils.getCurrentUserId(authentication);
        inventoryApplicationService.deleteInventoryItem(projectId, characterId, inventoryItemId, userId);
        return ResponseEntity.ok(ApiResponse.success("移出背包成功"));
    }

    @PostMapping("/api/projects/{projectId}/characters/{characterId}/inventory/generate-items")
    public ResponseEntity<?> generateInventoryItems(
            @PathVariable Long projectId,
            @PathVariable Long characterId,
            @RequestBody ItemGenerationRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            Authentication authentication) {
        if (!AuthHeaderSupport.hasValidBearerToken(authorizationHeader)) {
            return AuthHeaderSupport.unauthorizedResponse();
        }

        Long userId = SecurityUtils.getCurrentUserId(authentication);
        ItemGenerationResultResponse result = itemGenerationApplicationService.generate(projectId, characterId, userId, request);
        List<CharacterInventoryItemResponse> inventoryItems = result.getItems().stream()
                .map(item -> {
                    ItemDefinition definition = itemApplicationService.createGeneratedItem(projectId, userId, item);
                    InventoryItemRequest inventoryItemRequest = new InventoryItemRequest();
                    inventoryItemRequest.setItemId(definition.getId());
                    inventoryItemRequest.setQuantity(item.getSuggestedQuantity());
                    inventoryItemRequest.setEquipped(false);
                    inventoryItemRequest.setDurability(100);
                    return inventoryApplicationService.addInventoryItem(projectId, characterId, userId, inventoryItemRequest);
                })
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("生成并加入背包成功", inventoryItems));
    }

    private CharacterInventoryItemResponse toResponse(CharacterInventoryItem inventoryItem) {
        ItemDefinition item = itemApplicationService.getItemById(inventoryItem.getItemId());
        if (item == null) {
            ItemResponse placeholder = new ItemResponse();
            placeholder.setId(inventoryItem.getItemId());
            placeholder.setName("物品已失效");
            placeholder.setCategory("prop");
            placeholder.setRarity("common");
            placeholder.setAttributesJson("{}");
            placeholder.setEffectJson("{}");
            return CharacterInventoryItemResponse.from(inventoryItem, placeholder);
        }
        return CharacterInventoryItemResponse.from(inventoryItem, ItemResponse.from(item));
    }
}
