package com.example.erp.controller.dto;

public class MaterialOrderStyleResult {
    private final String stylesId;
    private final String styleCode;
    private final String item;
    private final boolean active;

    public MaterialOrderStyleResult(String stylesId, String styleCode, String item, boolean active) {
        this.stylesId = stylesId;
        this.styleCode = styleCode;
        this.item = item;
        this.active = active;
    }

    public String getStylesId() {
        return stylesId;
    }

    public String getStyleCode() {
        return styleCode;
    }

    public String getItem() {
        return item;
    }

    public boolean isActive() {
        return active;
    }
}