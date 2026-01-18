package com.example.erp.controller.dto;

import java.util.List;

public class FgReceiptListResponse {
	private final List<FgReceiptJobView> items;

	public FgReceiptListResponse(List<FgReceiptJobView> items) {
		this.items = items;
	}

	public List<FgReceiptJobView> getItems() {
		return items;
	}
}