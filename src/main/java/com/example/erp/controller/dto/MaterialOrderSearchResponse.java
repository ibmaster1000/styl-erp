package com.example.erp.controller.dto;

import java.util.List;

public class MaterialOrderSearchResponse {
    private final List<MaterialOrderSupplierView> suppliers;
    private final List<MaterialOrderItemView> materials;
    private final MaterialSpecMatrixResponse colorSizeMatrix;

    public MaterialOrderSearchResponse(List<MaterialOrderSupplierView> suppliers,
            List<MaterialOrderItemView> materials,
            MaterialSpecMatrixResponse colorSizeMatrix) {
        this.suppliers = suppliers;
        this.materials = materials;
        this.colorSizeMatrix = colorSizeMatrix;
    }

    public List<MaterialOrderSupplierView> getSuppliers() {
        return suppliers;
    }

    public List<MaterialOrderItemView> getMaterials() {
        return materials;
    }

    public MaterialSpecMatrixResponse getColorSizeMatrix() {
        return colorSizeMatrix;
    }
}
