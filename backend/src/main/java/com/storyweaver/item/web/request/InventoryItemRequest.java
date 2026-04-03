package com.storyweaver.item.web.request;

import lombok.Data;

@Data
public class InventoryItemRequest {

    private Long itemId;

    private Integer quantity;

    private Boolean equipped;

    private Integer durability;

    private String customName;

    private String notes;

    private Integer sortOrder;
}
