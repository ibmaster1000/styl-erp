package com.example.erp.controller.dto;

import java.util.List;

public class MaterialOrderSearchResponse {
    private final List<MaterialOrderSupplierView> suppliers;
    private final List<MaterialOrderLineRow> materialsToOrder;
    private final MaterialSpecMatrixResponse colorSizeMatrix;
    private final List<AgreementQuantitySummary> agreementSummaryRows;

    public MaterialOrderSearchResponse(List<MaterialOrderSupplierView> suppliers,
            List<MaterialOrderLineRow> materialsToOrder,
            MaterialSpecMatrixResponse colorSizeMatrix,
            List<AgreementQuantitySummary> agreementSummaryRows) {
        this.suppliers = suppliers;
        this.materialsToOrder = materialsToOrder;
        this.colorSizeMatrix = colorSizeMatrix;
        this.agreementSummaryRows = agreementSummaryRows;
    }

    public List<MaterialOrderSupplierView> getSuppliers() {
        return suppliers;
    }

    public List<MaterialOrderLineRow> getMaterialsToOrder() {
        return materialsToOrder;
    }

    public MaterialSpecMatrixResponse getColorSizeMatrix() {
        return colorSizeMatrix;
    }

    public List<AgreementQuantitySummary> getAgreementSummaryRows() {
        return agreementSummaryRows;
    }
}
