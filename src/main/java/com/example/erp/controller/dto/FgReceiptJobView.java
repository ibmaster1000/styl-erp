package com.example.erp.controller.dto;

import java.math.BigDecimal;

public class FgReceiptJobView {
	private final String stylesId;
	private final String styleCode;
	private final String prdAgreeCode;
	private final Long prdAgreeId;
	private final String colorCode;
	private final String factoryCode;
	private final BigDecimal quantity;

	public FgReceiptJobView(String stylesId, String styleCode, String prdAgreeCode, Long prdAgreeId, String colorCode,
			String factoryCode, BigDecimal quantity) {
		this.stylesId = stylesId;
		this.styleCode = styleCode;
		this.prdAgreeCode = prdAgreeCode;
		this.prdAgreeId = prdAgreeId;
		this.colorCode = colorCode;
		this.factoryCode = factoryCode;
		this.quantity = quantity;
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

	public Long getPrdAgreeId() {
		return prdAgreeId;
	}

	public String getColorCode() {
		return colorCode;
	}

	public String getFactoryCode() {
		return factoryCode;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}
}