package com.storyweaver.item.web.response;

import com.storyweaver.item.domain.entity.CharacterInventoryItem;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CharacterInventoryItemResponse {

    private Long id;

    private Long projectId;

    private Long characterId;

    private Long itemId;

    private Integer quantity;

    private boolean equipped;

    private Integer durability;

    private String customName;

    private String notes;

    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private ItemResponse item;

    public static CharacterInventoryItemResponse from(CharacterInventoryItem inventoryItem, ItemResponse itemResponse) {
        CharacterInventoryItemResponse response = new CharacterInventoryItemResponse();
        response.setId(inventoryItem.getId());
        response.setProjectId(inventoryItem.getProjectId());
        response.setCharacterId(inventoryItem.getCharacterId());
        response.setItemId(inventoryItem.getItemId());
        response.setQuantity(inventoryItem.getQuantity());
        response.setEquipped(Integer.valueOf(1).equals(inventoryItem.getEquipped()));
        response.setDurability(inventoryItem.getDurability());
        response.setCustomName(inventoryItem.getCustomName());
        response.setNotes(inventoryItem.getNotes());
        response.setSortOrder(inventoryItem.getSortOrder());
        response.setCreateTime(inventoryItem.getCreateTime());
        response.setUpdateTime(inventoryItem.getUpdateTime());
        response.setItem(itemResponse);
        return response;
    }
}
