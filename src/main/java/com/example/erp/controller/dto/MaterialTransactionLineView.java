package com.example.erp.controller.dto;

import java.math.BigDecimal;

public class MaterialTransactionLineView {
	private Long mOrderId;
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
	private BigDecimal requiredQuantity;
	private BigDecimal inboundQuantity;
	private BigDecimal inventoryQuantity;
	private BigDecimal plannedOutboundQuantity;
	private BigDecimal outboundQuantity;
	private String remark;

	public MaterialTransactionLineView() {
	}

	public MaterialTransactionLineView(Long bomId, String stylesId, String styleCode, String prdAgreeCode,
			String agreementMonth, String colorCode, String category, String materialName, String materialUsage,
			String spec, String materialColor, String uom, BigDecimal qtyPerPiece, String supplierCode,
			String supplierName, String productionManager, String orderUom, BigDecimal unitPrice,
			BigDecimal orderQuantity, BigDecimal inboundQuantity, BigDecimal plannedOutboundQuantity,
			BigDecimal outboundQuantity, String remark) {
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
		this.plannedOutboundQuantity = plannedOutboundQuantity;
		this.outboundQuantity = outboundQuantity;
		this.remark = remark;
	}

	public MaterialTransactionLineView setMOrderId(Long mOrderId) {
		this.mOrderId = mOrderId;
		return this;
	}

	public MaterialTransactionLineView setBomId(Long bomId) {
		this.bomId = bomId;
		return this;
	}

	public MaterialTransactionLineView setStylesId(String stylesId) {
		this.stylesId = stylesId;
		return this;
	}

	public MaterialTransactionLineView setStyleCode(String styleCode) {
		this.styleCode = styleCode;
		return this;
	}

	public MaterialTransactionLineView setPrdAgreeCode(String prdAgreeCode) {
		this.prdAgreeCode = prdAgreeCode;
		return this;
	}

	public MaterialTransactionLineView setAgreementMonth(String agreementMonth) {
		this.agreementMonth = agreementMonth;
		return this;
	}

	public MaterialTransactionLineView setColorCode(String colorCode) {
		this.colorCode = colorCode;
		return this;
	}

	public MaterialTransactionLineView setCategory(String category) {
		this.category = category;
		return this;
	}

	public MaterialTransactionLineView setMaterialName(String materialName) {
		this.materialName = materialName;
		return this;
	}

	public MaterialTransactionLineView setMaterialUsage(String materialUsage) {
		this.materialUsage = materialUsage;
		return this;
	}

	public MaterialTransactionLineView setSpec(String spec) {
		this.spec = spec;
		return this;
	}

	public MaterialTransactionLineView setMaterialColor(String materialColor) {
		this.materialColor = materialColor;
		return this;
	}

	public MaterialTransactionLineView setUom(String uom) {
		this.uom = uom;
		return this;
	}

	public MaterialTransactionLineView setQtyPerPiece(BigDecimal qtyPerPiece) {
		this.qtyPerPiece = qtyPerPiece;
		return this;
	}

	public MaterialTransactionLineView setSupplierCode(String supplierCode) {
		this.supplierCode = supplierCode;
		return this;
	}

	public MaterialTransactionLineView setSupplierName(String supplierName) {
		this.supplierName = supplierName;
		return this;
	}

	public MaterialTransactionLineView setProductionManager(String productionManager) {
		this.productionManager = productionManager;
		return this;
	}

	public MaterialTransactionLineView setOrderUom(String orderUom) {
		this.orderUom = orderUom;
		return this;
	}

	public MaterialTransactionLineView setUnitPrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice;
		return this;
	}

	public MaterialTransactionLineView setOrderQuantity(BigDecimal orderQuantity) {
		this.orderQuantity = orderQuantity;
		return this;
	}

	public MaterialTransactionLineView setRequiredQuantity(BigDecimal requiredQuantity) {
		this.requiredQuantity = requiredQuantity;
		return this;
	}

	public MaterialTransactionLineView setInboundQuantity(BigDecimal inboundQuantity) {
		this.inboundQuantity = inboundQuantity;
		return this;
	}
	
	public MaterialTransactionLineView setInventoryQuantity(BigDecimal inventoryQuantity) {
		this.inventoryQuantity = inventoryQuantity;
		return this;
	}

	public MaterialTransactionLineView setPlannedOutboundQuantity(BigDecimal plannedOutboundQuantity) {
		this.plannedOutboundQuantity = plannedOutboundQuantity;
		return this;
	}

	public MaterialTransactionLineView setOutboundQuantity(BigDecimal outboundQuantity) {
		this.outboundQuantity = outboundQuantity;
		return this;
	}

	public MaterialTransactionLineView setRemark(String remark) {
		this.remark = remark;
		return this;
	}

	public Long getMOrderId() {
		return mOrderId;
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

	public BigDecimal getRequiredQuantity() {
		return requiredQuantity;
	}

	public BigDecimal getInboundQuantity() {
		return inboundQuantity;
	}

	public BigDecimal getInventoryQuantity() {
		return inventoryQuantity;
	}
	
	public BigDecimal getPlannedOutboundQuantity() {
		return plannedOutboundQuantity;
	}

	public BigDecimal getOutboundQuantity() {
		return outboundQuantity;
	}

	public String getRemark() {
		return remark;
	}
}
