package com.example.erp.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StyleListRow {

	private final String styleNo;
	private final String item;
	private final String colors;
	private final String sizes;
	private final String designer;
	private final String productionManager;
	private final String salesManager;
	private final String logisticManager;
	private final BigDecimal productionCost;
	private final BigDecimal supplyPrice;
	private final BigDecimal salesPrice;
	private final LocalDate startDate;
	private final boolean active;

	public StyleListRow(String styleNo, String item, String colors, String sizes, String designer,
			String productionManager, String salesManager, String logisticManager,
			BigDecimal productionCost, BigDecimal supplyPrice, BigDecimal salesPrice, LocalDate startDate,
			boolean active) {
		this.styleNo = styleNo;
		this.item = item;
		this.colors = colors;
		this.sizes = sizes;
		this.designer = designer;
		this.productionManager = productionManager;
		this.salesManager = salesManager;

		this.logisticManager = logisticManager;
		this.productionCost = productionCost;
		this.supplyPrice = supplyPrice;
		this.salesPrice = salesPrice;
		this.startDate = startDate;
		this.active = active;
	}

	public String getStyleNo() {
		return styleNo;
	}

	public String getItem() {
		return item;
	}

	public String getColors() {
		return colors;
	}

	public String getSizes() {
		return sizes;
	}

	public String getDesigner() {
		return designer;
	}

	public String getProductionManager() {
		return productionManager;
	}

	public String getSalesManager() {
		return salesManager;
	}

	public String getLogisticManager() {
		return logisticManager;
	}

	public BigDecimal getProductionCost() {
		return productionCost;
	}

	public BigDecimal getSupplyPrice() {
		return supplyPrice;
	}

	public BigDecimal getSalesPrice() {
		return salesPrice;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public boolean isActive() {
		return active;
	}
}