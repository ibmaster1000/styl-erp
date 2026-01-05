package com.example.erp.controller.dto;

import java.math.BigDecimal;

public class MaterialTransactionLineView {
    private Long bomId;
    private String stylesId;
    private String styleCode;
    private String prdAgreeCode;
    private String agreementMonth;
    private String colorCode;
    private String category;
    private String materialName;
    private String materialUsage;
    private String spec;
    private String materialColor;
    private String uom;
    private BigDecimal qtyPerPiece;
    private String supplierCode;
    private String supplierName;
    private String productionManager;
    private String orderUom;
    private BigDecimal unitPrice;
    private BigDecimal orderQuantity;
    private BigDecimal inboundQuantity;
    private BigDecimal outboundQuantity;
    private String remark;

    public MaterialTransactionLineView(Long bomId,
            String stylesId,
            String styleCode,
            String prdAgreeCode,
            String agreementMonth,
            String colorCode,
            String category,
            String materialName,
            String materialUsage,
            String spec,
            String materialColor,
            String uom,
            BigDecimal qtyPerPiece,
            String supplierCode,
            String supplierName,
            String productionManager,
            String orderUom,
            BigDecimal unitPrice,
            BigDecimal orderQuantity,
            BigDecimal inboundQuantity,
            BigDecimal outboundQuantity,
            String remark) {
        this.bomId = bomId;
        this.stylesId = stylesId;
        this.styleCode = styleCode;
        this.prdAgreeCode = prdAgreeCode;
        this.agreementMonth = agreementMonth;
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
        this.productionManager = productionManager;
        this.orderUom = orderUom;
        this.unitPrice = unitPrice;
        this.orderQuantity = orderQuantity;
        this.inboundQuantity = inboundQuantity;
        this.outboundQuantity = outboundQuantity;
        this.remark = remark;
    }

    public MaterialTransactionLineView setSupplierName(String supplierName) {
        this.supplierName = supplierName;
        return this;
    }

    public MaterialTransactionLineView setOrderQuantity(BigDecimal orderQuantity) {
        this.orderQuantity = orderQuantity;
        return this;
    }

    public MaterialTransactionLineView setInboundQuantity(BigDecimal inboundQuantity) {
        this.inboundQuantity = inboundQuantity;
        return this;
    }

    public MaterialTransactionLineView setOutboundQuantity(BigDecimal outboundQuantity) {
        this.outboundQuantity = outboundQuantity;
        return this;
    }

    public Long getBomId() {
        return bomId;
    }

    public String getStylesId() {
        return stylesId;
    }

    public String getStyleCode() {
        return styleCode;
    }

    public String getPrdAgreeCode() {
        return prdAgreeCode;
    }

    public String getAgreementMonth() {
        return agreementMonth;
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

    public String getProductionManager() {
        return productionManager;
    }

    public String getOrderUom() {
        return orderUom;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getOrderQuantity() {
        return orderQuantity;
    }

    public BigDecimal getInboundQuantity() {
        return inboundQuantity;
    }

    public BigDecimal getOutboundQuantity() {
        return outboundQuantity;
    }

    public String getRemark() {
        return remark;
    }
}