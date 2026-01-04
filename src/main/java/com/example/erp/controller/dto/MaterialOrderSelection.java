package com.example.erp.controller.dto;

import java.util.List;
import java.util.Map;

public class MaterialOrderSelection {
    private final String stylesId;
    private final String styleCode;
    private final List<String> colors;
    private final List<String> agreements;
    private final Map<String, List<String>> styleRule;

    public MaterialOrderSelection(String stylesId,
            String styleCode,
            List<String> colors,
            List<String> agreements,
            Map<String, List<String>> styleRule) {
        this.stylesId = stylesId;
        this.styleCode = styleCode;
        this.colors = colors;
        this.agreements = agreements;
        this.styleRule = styleRule;
    }

    public String getStylesId() {
        return stylesId;
    }

    public String getStyleCode() {
        return styleCode;
    }

    public List<String> getColors() {
        return colors;
    }

    public List<String> getAgreements() {
        return agreements;
    }

    public Map<String, List<String>> getStyleRule() {
        return styleRule;
    }
}