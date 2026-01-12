package com.example.erp.controller.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MaterialSpecContextView {
    private final String styleCode;
    private final String colorCode;
    private final String prdAgreeCode;
    private final Long stylesId;
    private final Long prdAgreeId;
    private final List<String> sizes;
    private final Map<String, Integer> quantities;
    private final List<MaterialSpecItemView> materials;

    public MaterialSpecContextView(String styleCode,
            String colorCode,
            String prdAgreeCode,
            Long stylesId,
            Long prdAgreeId,
            List<String> sizes,
            Map<String, Integer> quantities,
            List<MaterialSpecItemView> materials) {
        this.styleCode = styleCode;
        this.colorCode = colorCode;
        this.prdAgreeCode = prdAgreeCode;
        this.stylesId = stylesId;
        this.prdAgreeId = prdAgreeId;
        this.sizes = sizes != null ? sizes : Collections.emptyList();
        this.quantities = quantities != null ? quantities : Collections.emptyMap();
        this.materials = materials != null ? materials : Collections.emptyList();
    }

    public String getStyleCode() {
        return styleCode;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getPrdAgreeCode() {
        return prdAgreeCode;
    }

    public Long getStylesId() {
        return stylesId;
    }

    public Long getPrdAgreeId() {
        return prdAgreeId;
    }

    public List<String> getSizes() {
        return sizes;
    }

    public Map<String, Integer> getQuantities() {
        return quantities;
    }

    public List<MaterialSpecItemView> getMaterials() {
        return materials;
    }
}
