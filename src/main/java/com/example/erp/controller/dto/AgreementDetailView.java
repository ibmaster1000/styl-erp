package com.example.erp.controller.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AgreementDetailView {
	private final String styleCode;
	private final String agreementCode;
	private final List<AgreementDetailRow> rows;
	private final List<AgreementDetailColorGroup> colorGroups;
	private final List<ProductionAgreementColorTotal> colorTotals;
	private final Map<String, ProductionAgreementColorTotal> colorTotalsByColor;
	private final int grandQuantity;
	private final BigDecimal grandAmount;

	public AgreementDetailView(String styleCode, String agreementCode, List<AgreementDetailRow> rows,
			List<AgreementDetailColorGroup> colorGroups,
			List<ProductionAgreementColorTotal> colorTotals,
			Map<String, ProductionAgreementColorTotal> colorTotalsByColor,
			int grandQuantity, BigDecimal grandAmount) {
		this.styleCode = styleCode;
		this.agreementCode = agreementCode;
		this.rows = rows;
		this.colorGroups = colorGroups;
		this.colorTotals = colorTotals;
		this.colorTotalsByColor = colorTotalsByColor;
		this.grandQuantity = grandQuantity;
		this.grandAmount = grandAmount;
	}

	public String getStyleCode() {
		return styleCode;
	}

	public String getAgreementCode() {
		return agreementCode;
	}

	public List<AgreementDetailRow> getRows() {
		return rows;
	}

	public List<AgreementDetailColorGroup> getColorGroups() {
		return colorGroups;
	}

	public List<ProductionAgreementColorTotal> getColorTotals() {
		return colorTotals;
	}

	public Map<String, ProductionAgreementColorTotal> getColorTotalsByColor() {
		return colorTotalsByColor;
	}

	public int getGrandQuantity() {
		return grandQuantity;
	}

	public BigDecimal getGrandAmount() {
		return grandAmount;
	}
}
