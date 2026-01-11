package com.example.erp.controller.dto;

public class AgreementCodeRow {
	private final String styleCode;
	private final String agreementCode;
	private final String color;
	private final int totalQuantity;
	private final String managerName;

	public AgreementCodeRow(String styleCode, String agreementCode, String color, int totalQuantity,
			String managerName) {
		this.styleCode = styleCode;
		this.agreementCode = agreementCode;
		this.color = color;
		this.totalQuantity = totalQuantity;
		this.managerName = managerName;
	}

	public String getStyleCode() {
		return styleCode;
	}

	public String getAgreementCode() {
		return agreementCode;
	}

	public String getColor() {
		return color;
	}

	public int getTotalQuantity() {
		return totalQuantity;
	}

	public String getManagerName() {
		return managerName;
	}
}
