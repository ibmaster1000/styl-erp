package com.example.erp.controller.dto;

import java.util.List;

public class FgReceiptSaveRequest {
	private List<FgReceiptSaveLine> items;

	public List<FgReceiptSaveLine> getItems() {
		return items;
	}

	public void setItems(List<FgReceiptSaveLine> items) {
		this.items = items;
	}
}