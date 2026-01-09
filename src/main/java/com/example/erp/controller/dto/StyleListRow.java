package com.example.erp.controller.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StyleListRow {

	private final String stylesId;
	private final String styleCode;
	private final String itemCode;
	private final String item;
	private final String colors;
	private final String colorsFull;
	private final String colorsDisplay;
	private final String sizes;
	private final String sizesFull;
	private final String sizesDisplay;
	private final String designer;
	private final String productionManager;
	private final String salesManager;
	private final String logisticManager;
	private final BigDecimal productionCost;
	private final BigDecimal supplyPrice;
	private final BigDecimal salesPrice;
	private final LocalDate startDate;
	private final boolean active;

	public StyleListRow(String stylesId, String styleCode, String itemCode, String item, String colors,
			String colorsFull, String colorsDisplay, String sizes, String sizesFull, String sizesDisplay,
			String designer, String productionManager, String salesManager, String logisticManager,
			BigDecimal productionCost, BigDecimal supplyPrice, BigDecimal salesPrice, LocalDate startDate,
			boolean active) {
		this.stylesId = stylesId;
		this.styleCode = styleCode;
		this.itemCode = itemCode;
		this.item = item;
		this.colors = colors;
		this.colorsFull = colorsFull;
		this.colorsDisplay = colorsDisplay;
		this.sizes = sizes;
		this.sizesFull = sizesFull;
		this.sizesDisplay = sizesDisplay;
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

	public String getStylesId() {
		return stylesId;
	}

	public String getStyleCode() {
		return styleCode;
	}

	public String getItemCode() {
		return itemCode;
	}

	public String getItem() {
		return item;
	}

	public String getColors() {
		return colors;
	}

	public String getColorsFull() {
		return colorsFull;
	}

	public String getColorsDisplay() {
		return colorsDisplay;
	}

	public String getSizes() {
		return sizes;
	}

	public String getSizesFull() {
		return sizesFull;
	}

	public String getSizesDisplay() {
		return sizesDisplay;
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
