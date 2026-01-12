package com.example.erp.controller.dto;

import java.math.BigDecimal;

public class MaterialSpecItemView {
    private final Long bomId;
    private final String category;
    private final String materialName;
    private final String materialUsage;
    private final String spec;
    private final String materialColor;
    private final String uom;
    private final BigDecimal qtyPerPiece;
    private final String materialCode;
    private final String supplierCode;
    private final BigDecimal lossRate;
    private final String orderUom;
    private final BigDecimal unitPrice;
    private final String remark;

    public MaterialSpecItemView(Long bomId,
            String category,
            String materialName,
            String materialUsage,
            String spec,
            String materialColor,
            String uom,
            BigDecimal qtyPerPiece,
            String materialCode,
            String supplierCode,
            BigDecimal lossRate,
            String orderUom,
            BigDecimal unitPrice,
            String remark) {
        this.bomId = bomId;
        this.category = category;
        this.materialName = materialName;
        this.materialUsage = materialUsage;
        this.spec = spec;
        this.materialColor = materialColor;
        this.uom = uom;
        this.qtyPerPiece = qtyPerPiece;
        this.materialCode = materialCode;
        this.supplierCode = supplierCode;
        this.lossRate = lossRate;
        this.orderUom = orderUom;
        this.unitPrice = unitPrice;
        this.remark = remark;
    }

    public Long getBomId() {
        return bomId;
    }

    public String getCategory() {
        return category;
    }

    public String getMaterialName() {
        return materialName;
    }

    public String getMaterialUsage() {
        return materialUsage;
    }

    public String getSpec() {
        return spec;
    }

    public String getMaterialColor() {
        return materialColor;
    }

    public String getUom() {
        return uom;
    }

    public BigDecimal getQtyPerPiece() {
        return qtyPerPiece;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public BigDecimal getLossRate() {
        return lossRate;
    }

    public String getOrderUom() {
        return orderUom;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getRemark() {
        return remark;
    }
}
