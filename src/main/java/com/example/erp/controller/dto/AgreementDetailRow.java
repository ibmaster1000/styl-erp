package com.example.erp.controller.dto;

import java.math.BigDecimal;

public class AgreementDetailRow {
	private final String color;
	private final String size;
	private final int quantity;
	private final BigDecimal supplyPrice;
	private final BigDecimal amount;

	public AgreementDetailRow(String color, String size, int quantity, BigDecimal supplyPrice, BigDecimal amount) {
		this.color = color;
		this.size = size;
		this.quantity = quantity;
		this.supplyPrice = supplyPrice;
		this.amount = amount;
	}

	public String getColor() {
		return color;
	}

	public String getSize() {
		return size;
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
