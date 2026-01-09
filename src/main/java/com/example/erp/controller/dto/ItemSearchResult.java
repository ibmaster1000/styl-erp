package com.example.erp.controller.dto;

public class ItemSearchResult {

    private final String itemCode;
    private final String itemName;

    public ItemSearchResult(String itemCode, String itemName) {
        this.itemCode = itemCode;
        this.itemName = itemName;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }
}
