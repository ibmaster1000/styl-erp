package com.example.erp.controller.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductionAgreementDetailView {
	private final String styleCode;
	private final String agreementCode;
	private final List<ProductionAgreementDetailLine> lines;
	private final List<ProductionAgreementColorTotal> colorTotals;
	private final int grandQuantity;
	private final BigDecimal grandAmount;

	public ProductionAgreementDetailView(String styleCode, String agreementCode,
			List<ProductionAgreementDetailLine> lines, List<ProductionAgreementColorTotal> colorTotals,
			int grandQuantity, BigDecimal grandAmount) {
		this.styleCode = styleCode;
		this.agreementCode = agreementCode;
		this.lines = (lines != null) ? new ArrayList<>(lines) : new ArrayList<>();
		this.colorTotals = (colorTotals != null) ? new ArrayList<>(colorTotals) : new ArrayList<>();
		this.grandQuantity = grandQuantity;
		this.grandAmount = (grandAmount != null) ? grandAmount : BigDecimal.ZERO;
	}

	public String getStyleCode() {
		return styleCode;
	}

	public String getAgreementCode() {
		return agreementCode;
	}

	public List<ProductionAgreementDetailLine> getLines() {
		return lines;
	}

	public List<ProductionAgreementColorTotal> getColorTotals() {
		return colorTotals;
	}

	public int getGrandQuantity() {
		return grandQuantity;
	}

	public BigDecimal getGrandAmount() {
		return grandAmount;
	}
}