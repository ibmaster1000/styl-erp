package com.example.erp.repository;

import java.math.BigDecimal;

public interface AgreementDetailRowView {
	String getAgreementCode();

	String getStyleCode();

	String getColorName();

	String getColorCode();

	String getSizeName();

	String getSizeCode();

	Integer getQuantity();

	BigDecimal getSupplyPrice();

	BigDecimal getAmount();

	String getProductionManager();
}
