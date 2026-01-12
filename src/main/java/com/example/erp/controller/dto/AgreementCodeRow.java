package com.example.erp.controller.dto;

public class AgreementCodeRow {
	private final String styleCode;
	private final String agreementCode;
	private final String colorCode;
	private final String displayStyleCode;
	private final String displayAgreementCode;
	private final String color;
	private final int totalQuantity;
	private final String managerName;

	public AgreementCodeRow(String styleCode, String agreementCode, String displayStyleCode,
			String displayAgreementCode, String colorCode, String color, int totalQuantity,
			String managerName) {
		this.styleCode = styleCode;
		this.agreementCode = agreementCode;
		this.colorCode = colorCode;
		this.displayStyleCode = displayStyleCode;
		this.displayAgreementCode = displayAgreementCode;
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

	public String getColorCode() {
		return colorCode;
	}

	public String getDisplayStyleCode() {
		return displayStyleCode;
	}

	public String getDisplayAgreementCode() {
		return displayAgreementCode;
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
