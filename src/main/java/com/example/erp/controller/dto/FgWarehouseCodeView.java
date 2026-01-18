package com.example.erp.controller.dto;

public class FgWarehouseCodeView {
	private final String code;
	private final String codeName;
	private final String remark;

	public FgWarehouseCodeView(String code, String codeName, String remark) {
		this.code = code;
		this.codeName = codeName;
		this.remark = remark;
	}

	public String getCode() {
		return code;
	}

	public String getCodeName() {
		return codeName;
	}

	public String getRemark() {
		return remark;
	}
}