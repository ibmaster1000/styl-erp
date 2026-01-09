package com.example.erp.controller.dto;

public class StyleSaveResponse {

    private final Long stylesId;
    private final String styleCode;

    public StyleSaveResponse(Long stylesId, String styleCode) {
        this.stylesId = stylesId;
        this.styleCode = styleCode;
    }

    public Long getStylesId() {
        return stylesId;
    }

    public String getStyleCode() {
        return styleCode;
    }
}
