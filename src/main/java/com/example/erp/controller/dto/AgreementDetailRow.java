package com.example.erp.controller.dto;

import java.math.BigDecimal;

public class AgreementDetailRow {
	private final String color;
	private final String colorCode;
	private final String size;
	private final String sizeCode;
	private final int quantity;
	private final BigDecimal supplyPrice;
	private final BigDecimal amount;

	public AgreementDetailRow(String color, String colorCode, String size, String sizeCode, int quantity,
			BigDecimal supplyPrice, BigDecimal amount) {
		this.color = color;
		this.colorCode = colorCode;
		this.size = size;
		this.sizeCode = sizeCode;
		this.quantity = quantity;
		this.supplyPrice = supplyPrice;
		this.amount = amount;
	}

	public String getColor() {
		return color;
	}

	public String getColorCode() {
		return colorCode;
	}

	public String getSize() {
		return size;
	}

	public String getSizeCode() {
		return sizeCode;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getSupplyPrice() {
		return supplyPrice;
	}

	public BigDecimal getAmount() {
		return amount;
	}
}
