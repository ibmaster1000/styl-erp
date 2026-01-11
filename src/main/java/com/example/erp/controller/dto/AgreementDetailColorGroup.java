package com.example.erp.controller.dto;

import java.math.BigDecimal;
import java.util.List;

public class AgreementDetailColorGroup {
	private final String color;
	private final List<AgreementDetailRow> rows;
	private final int totalQuantity;
	private final BigDecimal totalAmount;

	public AgreementDetailColorGroup(String color, List<AgreementDetailRow> rows, int totalQuantity,
			BigDecimal totalAmount) {
		this.color = color;
		this.rows = rows;
		this.totalQuantity = totalQuantity;
		this.totalAmount = totalAmount;
	}

	public String getColor() {
		return color;
	}

	public List<AgreementDetailRow> getRows() {
		return rows;
	}

	public int getTotalQuantity() {
		return totalQuantity;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}
}
