package com.example.erp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

import java.time.LocalDate;

@Entity
@Table(name = "styles")
public class Style {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "styles_id")
	private Long stylesId;

	@Column(name = "style_code", length = 50)
	private String styleCode;

	@Column(name = "item_code", length = 50)
	private String itemCode;

	@Column(name = "item_name", length = 100)
	private String itemName;

	@Column(name = "designer_emp_no", length = 50)
	private String designerEmpNo;

	@Column(name = "product_emp_no", length = 50)
	private String productEmpNo;

	@Column(name = "sales_emp_no", length = 50)
	private String salesEmpNo;

	@Column(name = "logistic_emp_no", length = 50)
	private String logisticEmpNo;

	@Column(name = "start_date")
	private LocalDate startDate;

	@Column(name = "cost_price")
	private BigDecimal costPrice;

	@Column(name = "production_cost")
	private BigDecimal productionCost;

	@Column(name = "supply_price")
	private BigDecimal supplyPrice;

	@Column(name = "sales_price")
	private BigDecimal salesPrice;

	@Column(name = "is_active")
	private Integer isActive;

	public Long getStylesId() {
		return stylesId;
	}

	public void setStylesId(Long stylesId) {
		this.stylesId = stylesId;
	}

	public String getStyleCode() {
		return styleCode;
	}

	public void setStyleCode(String styleCode) {
		this.styleCode = styleCode;
	}

	public String getItemCode() {
		return itemCode;
	}

	public void setItemCode(String itemCode) {
		this.itemCode = itemCode;
	}

	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public String getDesignerEmpNo() {
		return designerEmpNo;
	}

	public void setDesignerEmpNo(String designerEmpNo) {
		this.designerEmpNo = designerEmpNo;
	}

	public String getProductEmpNo() {
		return productEmpNo;
	}

	public void setProductEmpNo(String productEmpNo) {
		this.productEmpNo = productEmpNo;
	}

	public String getSalesEmpNo() {
		return salesEmpNo;
	}

	public void setSalesEmpNo(String salesEmpNo) {
		this.salesEmpNo = salesEmpNo;
	}

	public String getLogisticEmpNo() {
		return logisticEmpNo;
	}

	public void setLogisticEmpNo(String logisticEmpNo) {
		this.logisticEmpNo = logisticEmpNo;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public BigDecimal getCostPrice() {
		return costPrice;
	}

	public void setCostPrice(BigDecimal costPrice) {
		this.costPrice = costPrice;
	}

	public BigDecimal getProductionCost() {
		return productionCost;
	}

	public void setProductionCost(BigDecimal productionCost) {
		this.productionCost = productionCost;
	}

	public BigDecimal getSupplyPrice() {
		return supplyPrice;
	}

	public void setSupplyPrice(BigDecimal supplyPrice) {
		this.supplyPrice = supplyPrice;
	}

	public BigDecimal getSalesPrice() {
		return salesPrice;
	}

	public void setSalesPrice(BigDecimal salesPrice) {
		this.salesPrice = salesPrice;
	}

	public Integer getIsActive() {
		return isActive;
	}

	public void setIsActive(Integer isActive) {
		this.isActive = isActive;
	}
}