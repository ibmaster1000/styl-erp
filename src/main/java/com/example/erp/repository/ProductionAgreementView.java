package com.example.erp.repository;

public interface ProductionAgreementView {
	Long getPrdAgreeId();

	String getAgreementCode();

	Long getStylesId();

	String getStyleCode();

	String getProductEmpNo();

	String getProductEmpName();

	String getColorType();

	String getColorCode();

	String getColorName();

	String getSizeType();

	String getSizeCode();

	String getSizeName();

	Integer getQuantity();

	String getStatus();

	String getRemark();

	String getProductionManager();
}
