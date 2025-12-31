package com.example.erp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "codes")
public class Code extends BaseAuditEntity {

	@EmbeddedId
	private CodeId id;

	@Column(name = "code_name", length = 100)
	private String codeName;

	@Column(name = "remark", length = 255)
	private String remark;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	@Column(length = 255)
	private String description;

	@Column(name = "group_code", length = 50)
	private String groupCode;

	@Column(name = "name", length = 100)
	private String name;

	@Column(name = "sort_order")
	private Integer sortOrder = 0;

	@Column(nullable = false)
	private boolean deleted = false;

	public CodeId getId() {
		return id;
	}

	public void setId(CodeId id) {
		this.id = id;
	}

	public String getCodeType() {
		return id != null ? id.getCodeType() : null;
	}

	public String getCode() {
		return id != null ? id.getCode() : null;
	}

	public String getCodeName() {
		return codeName;
	}

	public void setCodeName(String codeName) {
		this.codeName = codeName;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getGroupCode() {
		return groupCode;
	}

	public void setGroupCode(String groupCode) {
		this.groupCode = groupCode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
}