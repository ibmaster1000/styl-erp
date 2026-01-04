package com.example.erp.controller.dto;

public class MaterialOrderSupplierView {
    private final String supplierCode;
    private final String supplierName;
    private final boolean ordered;

    public MaterialOrderSupplierView(String supplierCode, String supplierName, boolean ordered) {
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.ordered = ordered;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public boolean isOrdered() {
        return ordered;
    }
}