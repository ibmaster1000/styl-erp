package com.example.erp.controller.dto;

public class AgreementLeftRow {
	private final String styleCode;
	private final String agreementCode;
	private final String color;
	private final String colorCode;
	private final int colorTotalQuantity;
	private final String managerName;
	private final String displayStyleCode;
	private final String displayAgreementCode;
	private final boolean completed;

	public AgreementLeftRow(String styleCode, String agreementCode, String color, String colorCode,
			int colorTotalQuantity, String managerName, String displayStyleCode, String displayAgreementCode,
			boolean completed) {
		this.styleCode = styleCode;
		this.agreementCode = agreementCode;
		this.color = color;
		this.colorCode = colorCode;
		this.colorTotalQuantity = colorTotalQuantity;
		this.managerName = managerName;
		this.displayStyleCode = displayStyleCode;
		this.displayAgreementCode = displayAgreementCode;
		this.completed = completed;
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

	public String getColorCode() {
		return colorCode;
	}

	public int getColorTotalQuantity() {
		return colorTotalQuantity;
	}

	public String getManagerName() {
		return managerName;
	}

	public String getDisplayStyleCode() {
		return displayStyleCode;
	}

	public String getDisplayAgreementCode() {
		return displayAgreementCode;
	}

	public boolean isCompleted() {
		return completed;
	}
}
