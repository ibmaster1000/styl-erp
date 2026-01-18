package com.example.erp.controller.dto;

import java.math.BigDecimal;

public class FgReceiptSaveLine {
	private String stylesId;
	private Long prdAgreeId;
	private String prdAgreeCode;
	private String colorCode;
	private String factoryCode;
	private String styleCode;
	private String warehouseCode;
	private String aisle;
	private String section;
	private String level;
	private BigDecimal inboundQty;

	public String getStylesId() {
		return stylesId;
	}

	public void setStylesId(String stylesId) {
		this.stylesId = stylesId;
	}

	public Long getPrdAgreeId() {
		return prdAgreeId;
	}

	public void setPrdAgreeId(Long prdAgreeId) {
		this.prdAgreeId = prdAgreeId;
	}

	public String getPrdAgreeCode() {
		return prdAgreeCode;
	}

	public void setPrdAgreeCode(String prdAgreeCode) {
		this.prdAgreeCode = prdAgreeCode;
	}

	public String getColorCode() {
		return colorCode;
	}

	public void setColorCode(String colorCode) {
		this.colorCode = colorCode;
	}

	public String getFactoryCode() {
		return factoryCode;
	}

	public void setFactoryCode(String factoryCode) {
		this.factoryCode = factoryCode;
	}

	public String getStyleCode() {
		return styleCode;
	}

	public void setStyleCode(String styleCode) {
		this.styleCode = styleCode;
	}

	public String getWarehouseCode() {
		return warehouseCode;
	}

	public void setWarehouseCode(String warehouseCode) {
		this.warehouseCode = warehouseCode;
	}

	public String getAisle() {
		return aisle;
	}

	public void setAisle(String aisle) {
		this.aisle = aisle;
	}

	public String getSection() {
		return section;
	}

	public void setSection(String section) {
		this.section = section;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public BigDecimal getInboundQty() {
		return inboundQty;
	}

	public void setInboundQty(BigDecimal inboundQty) {
		this.inboundQty = inboundQty;
	}
}