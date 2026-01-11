package com.example.erp.controller.dto;

public class PageInfo {
	private final int page;
	private final int size;
	private final long totalCount;
	private final int totalPages;

	public PageInfo(int page, int size, long totalCount, int totalPages) {
		this.page = page;
		this.size = size;
		this.totalCount = totalCount;
		this.totalPages = totalPages;
	}

	public int getPage() {
		return page;
	}

	public int getSize() {
		return size;
	}

	public long getTotalCount() {
		return totalCount;
	}

	public int getTotalPages() {
		return totalPages;
	}
}