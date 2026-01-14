package com.example.erp.controller.dto;

import java.math.BigDecimal;

public class MaterialOrderLineRow {
    private final String colorCode;
    private final String category;
    private final String materialName;
    private final String materialUsage;
    private final String spec;
    private final String materialColor;
    private final String uom;
    private final BigDecimal qtyPerPiece;
    private final String supplierCode;
    private final String supplierName;
    private final BigDecimal lossRate;
    private final String orderUom;
    private final BigDecimal unitPrice;
    private final String remark;
    private final BigDecimal productionQty;
    private final BigDecimal orderQty;
    private final BigDecimal orderAmount;

    public MaterialOrderLineRow(String colorCode,
            String category,
            String materialName,
            String materialUsage,
            String spec,
            String materialColor,
            String uom,
            BigDecimal qtyPerPiece,
            String supplierCode,
            String supplierName,
            BigDecimal lossRate,
            String orderUom,
            BigDecimal unitPrice,
            String remark,
            BigDecimal productionQty,
            BigDecimal orderQty,
            BigDecimal orderAmount) {
        this.colorCode = colorCode;
        this.category = category;
        this.materialName = materialName;
        this.materialUsage = materialUsage;
        this.spec = spec;
        this.materialColor = materialColor;
        this.uom = uom;
        this.qtyPerPiece = qtyPerPiece;
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.lossRate = lossRate;
        this.orderUom = orderUom;
        this.unitPrice = unitPrice;
        this.remark = remark;
        this.productionQty = productionQty;
        this.orderQty = orderQty;
        this.orderAmount = orderAmount;
    }

    public String getColorCode() {
        return colorCode;
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

    public String getSupplierCode() {
        return supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
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

    public BigDecimal getProductionQty() {
        return productionQty;
    }

    public BigDecimal getOrderQty() {
        return orderQty;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }
}
