package com.example.erp.controller.dto;

import java.math.BigDecimal;

public class MaterialSpecSaveItem {
    private Long bomId;
    private String category;
    private String materialName;
    private String materialUsage;
    private String spec;
    private String materialColor;
    private String uom;
    private BigDecimal qtyPerPiece;
    private String materialCode;
    private String supplierCode;
    private BigDecimal lossRate;
    private String orderUom;
    private BigDecimal unitPrice;
    private String remark;

    public Long getBomId() {
        return bomId;
    }

    public void setBomId(Long bomId) {
        this.bomId = bomId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getMaterialUsage() {
        return materialUsage;
    }

    public void setMaterialUsage(String materialUsage) {
        this.materialUsage = materialUsage;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getMaterialColor() {
        return materialColor;
    }

    public void setMaterialColor(String materialColor) {
        this.materialColor = materialColor;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }

    public BigDecimal getQtyPerPiece() {
        return qtyPerPiece;
    }

    public void setQtyPerPiece(BigDecimal qtyPerPiece) {
        this.qtyPerPiece = qtyPerPiece;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public BigDecimal getLossRate() {
        return lossRate;
    }

    public void setLossRate(BigDecimal lossRate) {
        this.lossRate = lossRate;
    }

    public String getOrderUom() {
        return orderUom;
    }

    public void setOrderUom(String orderUom) {
        this.orderUom = orderUom;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
