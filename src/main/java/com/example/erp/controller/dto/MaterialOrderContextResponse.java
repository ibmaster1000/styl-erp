package com.example.erp.controller.dto;

import java.util.List;

public class MaterialOrderContextResponse {
    private final boolean styleVerified;
    private final String message;
    private final List<String> colors;
    private final List<String> agreements;
    private final MaterialSpecMatrixResponse colorSizeMatrix;

    public MaterialOrderContextResponse(boolean styleVerified,
            String message,
            List<String> colors,
            List<String> agreements,
            MaterialSpecMatrixResponse colorSizeMatrix) {
        this.styleVerified = styleVerified;
        this.message = message;
        this.colors = colors;
        this.agreements = agreements;
        this.colorSizeMatrix = colorSizeMatrix;
    }

    public boolean isStyleVerified() {
        return styleVerified;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getColors() {
        return colors;
    }

    public List<String> getAgreements() {
        return agreements;
    }

    public MaterialSpecMatrixResponse getColorSizeMatrix() {
        return colorSizeMatrix;
    }
}
