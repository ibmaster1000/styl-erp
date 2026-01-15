package com.example.erp.service;

import com.example.erp.controller.dto.MaterialTransactionLineView;
import com.example.erp.controller.dto.MaterialTransactionSaveLine;
import com.example.erp.controller.dto.MaterialTransactionSaveRequest;
import com.example.erp.controller.dto.SimpleCodeView;
import com.example.erp.domain.ProductionAgreement;
import com.example.erp.repository.ProductionAgreementRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MaterialTransactionService {

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final ProductionAgreementRepository productionAgreementRepository;

	public MaterialTransactionService(NamedParameterJdbcTemplate jdbcTemplate,
			ProductionAgreementRepository productionAgreementRepository) {
		this.jdbcTemplate = jdbcTemplate;
		this.productionAgreementRepository = productionAgreementRepository;
	}

	public List<MaterialTransactionLineView> findMaterials(String stylesId, String styleCode, String prdAgreeCode,
			String colorCode) {
		return findOrderBasedMaterials(stylesId, styleCode, prdAgreeCode, colorCode, true);
	}

	public List<MaterialTransactionLineView> findInboundMaterials(String stylesId, String styleCode,
			String prdAgreeCode, String colorCode) {
		return findOrderBasedMaterials(stylesId, styleCode, prdAgreeCode, colorCode, false);
	}

	@Transactional
	public Map<String, Object> saveInbound(MaterialTransactionSaveRequest request, String empNo) {
		if (!hasTable("material_inbounds")) {
			return Map.of("success", false, "created", 0, "skipped", 0, "message", "입고 테이블이 없습니다.");
		}
		if (CollectionUtils.isEmpty(request.getItems())) {
			return Map.of("success", false, "created", 0, "skipped", 0, "message", "입고 항목이 없습니다.");
		}

		String mOrderColumn = findFirstExistingColumn("material_inbounds", List.of("m_order_code"));
		String qtyColumn = findFirstExistingColumn("material_inbounds", List.of("received_qty"));
		String datetimeColumn = findFirstExistingColumn("material_inbounds",
				List.of("received_datetime", "received_at"));
		String createdByColumn = findFirstExistingColumn("material_inbounds", List.of("created_by"));
		String remarkColumn = findFirstExistingColumn("material_inbounds", List.of("remark"));

		if (mOrderColumn == null || qtyColumn == null) {
			return Map.of("success", false, "created", 0, "skipped", 0, "message", "입고 컬럼이 부족합니다.");
		}

		int created = 0;
		int skipped = 0;
		for (MaterialTransactionSaveLine line : request.getItems()) {
			BigDecimal qty = safeDecimal(line.getQuantity());
			if (qty.compareTo(BigDecimal.ZERO) <= 0 || !StringUtils.hasText(line.getMOrderCode())) {
				skipped++;
				continue;
			}

			MapSqlParameterSource params = new MapSqlParameterSource()
					.addValue("mOrderCode", line.getMOrderCode())
					.addValue("receivedQty", qty)
					.addValue("receivedDatetime", Timestamp.valueOf(LocalDateTime.now()))
					.addValue("createdBy", empNo)
					.addValue("remark", line.getRemark());

			List<String> columns = new ArrayList<>();
			List<String> values = new ArrayList<>();
			columns.add(mOrderColumn);
			values.add(":mOrderCode");
			columns.add(qtyColumn);
			values.add(":receivedQty");
			if (datetimeColumn != null) {
				columns.add(datetimeColumn);
				values.add(":receivedDatetime");
			}
			if (createdByColumn != null) {
				columns.add(createdByColumn);
				values.add(":createdBy");
			}
			if (remarkColumn != null) {
				columns.add(remarkColumn);
				values.add(":remark");
			}

			String sql = "insert into material_inbounds (" + String.join(", ", columns) + ") values ("
					+ String.join(", ", values) + ")";
			jdbcTemplate.update(sql, params);
			created++;
		}

		return Map.of("success", created > 0, "created", created, "skipped", skipped,
				"message", created > 0 ? "입고가 등록되었습니다." : "입고 등록에 실패했습니다.");
	}

	@Transactional
	public Map<String, Object> saveOutbound(MaterialTransactionSaveRequest request, String empNo) {
		if (!hasTable("material_outbounds")) {
			return Map.of("success", false, "created", 0, "skipped", 0, "message", "출고 테이블이 없습니다.");
		}
		if (CollectionUtils.isEmpty(request.getItems())) {
			return Map.of("success", false, "created", 0, "skipped", 0, "message", "출고 항목이 없습니다.");
		}

		String mOrderColumn = findFirstExistingColumn("material_outbounds", List.of("m_order_code"));
		String plannedColumn = findFirstExistingColumn("material_outbounds", List.of("planned_out_qty"));
		String issuedColumn = findFirstExistingColumn("material_outbounds", List.of("issued_out_qty"));
		String producerTypeColumn = findFirstExistingColumn("material_outbounds", List.of("producer_type"));
		String producerCodeColumn = findFirstExistingColumn("material_outbounds", List.of("producer_code"));
		String datetimeColumn = findFirstExistingColumn("material_outbounds",
				List.of("outbound_datetime", "issued_datetime"));
		String remarkColumn = findFirstExistingColumn("material_outbounds", List.of("remark"));

		if (mOrderColumn == null || plannedColumn == null || issuedColumn == null || producerCodeColumn == null) {
			return Map.of("success", false, "created", 0, "skipped", 0, "message", "출고 컬럼이 부족합니다.");
		}

		int created = 0;
		int skipped = 0;
		for (MaterialTransactionSaveLine line : request.getItems()) {
			if (!StringUtils.hasText(line.getMOrderCode())) {
				skipped++;
				continue;
			}
			BigDecimal planned = safeDecimal(line.getPlannedOutQuantity());
			BigDecimal issued = safeDecimal(line.getIssuedOutQuantity());
			if (planned.compareTo(BigDecimal.ZERO) <= 0 && issued.compareTo(BigDecimal.ZERO) <= 0) {
				skipped++;
				continue;
			}
			if (!StringUtils.hasText(line.getFactoryCode())) {
				skipped++;
				continue;
			}
			if (issued.compareTo(BigDecimal.ZERO) > 0) {
				BigDecimal available = sumInboundForOrder(line.getMOrderCode())
						.subtract(sumIssuedForOrder(line.getMOrderCode()));
				if (available.compareTo(issued) < 0) {
					skipped++;
					continue;
				}
			}

			MapSqlParameterSource params = new MapSqlParameterSource()
					.addValue("mOrderCode", line.getMOrderCode())
					.addValue("plannedOutQty", planned)
					.addValue("issuedOutQty", issued)
					.addValue("producerType", "CUSTOMER")
					.addValue("producerCode", line.getFactoryCode())
					.addValue("outboundDatetime", Timestamp.valueOf(LocalDateTime.now()))
					.addValue("remark", line.getRemark());

			List<String> columns = new ArrayList<>();
			List<String> values = new ArrayList<>();
			columns.add(mOrderColumn);
			values.add(":mOrderCode");
			columns.add(plannedColumn);
			values.add(":plannedOutQty");
			columns.add(issuedColumn);
			values.add(":issuedOutQty");
			if (producerTypeColumn != null) {
				columns.add(producerTypeColumn);
				values.add(":producerType");
			}
			columns.add(producerCodeColumn);
			values.add(":producerCode");
			if (datetimeColumn != null) {
				columns.add(datetimeColumn);
				values.add(":outboundDatetime");
			}
			if (remarkColumn != null) {
				columns.add(remarkColumn);
				values.add(":remark");
			}

			String sql = "insert into material_outbounds (" + String.join(", ", columns) + ") values ("
					+ String.join(", ", values) + ")";
			jdbcTemplate.update(sql, params);
			created++;
		}

		return Map.of("success", created > 0, "created", created, "skipped", skipped,
				"message", created > 0 ? "출고가 등록되었습니다." : "출고 등록에 실패했습니다.");
	}

	public List<SimpleCodeView> findFactoriesOrCustomers() {
		if (!hasTable("codes")) {
			return Collections.emptyList();
		}
		String codeColumn = findFirstExistingColumn("codes", List.of("code", "id_code"));
		String codeTypeColumn = findFirstExistingColumn("codes", List.of("code_type", "id_code_type"));
		String nameColumn = findFirstExistingColumn("codes", List.of("code_name", "name"));
		String activeColumn = findFirstExistingColumn("codes", List.of("is_active", "active"));
		String deletedColumn = findFirstExistingColumn("codes", List.of("deleted", "is_deleted"));
		if (codeColumn == null || codeTypeColumn == null) {
			return Collections.emptyList();
		}

		String sql = """
				select %s as code_value,
				       %s as code_type,
				       %s as code_name
				from codes
				where %s = 'CUSTOMER'
				%s
				%s
				order by %s asc
				""".formatted(codeColumn, codeTypeColumn, nameColumn != null ? nameColumn : "null", codeTypeColumn,
				activeColumn != null ? "and " + activeColumn + " = true" : "",
				deletedColumn != null ? "and " + deletedColumn + " = false" : "", codeColumn);

		MapSqlParameterSource params = new MapSqlParameterSource();
		List<SimpleCodeView> list = new ArrayList<>();
		jdbcTemplate.query(sql, params, rs -> {
			list.add(new SimpleCodeView(rs.getString("code_value"), rs.getString("code_name")));
		});
		return list;
	}

	private List<MaterialTransactionLineView> findOrderBasedMaterials(String stylesId, String styleCode,
			String prdAgreeCode, String colorCode, boolean includeOutbound) {
		if (!StringUtils.hasText(prdAgreeCode) || !StringUtils.hasText(colorCode)) {
			return Collections.emptyList();
		}
		if (!hasTable("material_orders") || !hasTable("material_specs")) {
			return Collections.emptyList();
		}

		Long prdAgreeId = resolvePrdAgreeId(prdAgreeCode, colorCode);
		if (prdAgreeId == null) {
			return Collections.emptyList();
		}

		String orderCodeColumn = findFirstExistingColumn("material_orders", List.of("m_order_code", "order_code"));
		String prdColumn = findFirstExistingColumn("material_orders", List.of("prd_agree_id"));
		String colorColumn = findFirstExistingColumn("material_orders", List.of("color_code"));
		String orderBomColumn = findFirstExistingColumn("material_orders", List.of("bom_id"));
		String orderStyleIdColumn = findFirstExistingColumn("material_orders", List.of("styles_id", "style_id"));
		String orderAmountColumn = findFirstExistingColumn("material_orders",
				List.of("order_amount", "amount", "qty", "quantity"));
		String orderVendorColumn = findFirstExistingColumn("material_orders",
				List.of("vendor_code", "supplier_code"));
		String orderUnitPriceColumn = findFirstExistingColumn("material_orders", List.of("unit_price"));

		String specBomColumn = findFirstExistingColumn("material_specs", List.of("bom_id", "material_spec_id"));
		String specStyleIdColumn = findFirstExistingColumn("material_specs", List.of("styles_id", "style_id"));
		String specStyleCodeColumn = findFirstExistingColumn("material_specs", List.of("style_code"));
		String categoryColumn = findFirstExistingColumn("material_specs", List.of("category"));
		String materialNameColumn = findFirstExistingColumn("material_specs", List.of("material_name"));
		String materialUsageColumn = findFirstExistingColumn("material_specs", List.of("material_usage"));
		String specColumn = findFirstExistingColumn("material_specs", List.of("spec"));
		String materialColorColumn = findFirstExistingColumn("material_specs", List.of("material_color"));
		String uomColumn = findFirstExistingColumn("material_specs", List.of("uom"));
		String qtyPerPieceColumn = findFirstExistingColumn("material_specs", List.of("qty_per_piece"));
		String supplierColumn = findFirstExistingColumn("material_specs", List.of("supplier_code"));
		String orderUomColumn = findFirstExistingColumn("material_specs", List.of("order_uom"));
		String unitPriceColumn = findFirstExistingColumn("material_specs", List.of("unit_price"));
		String remarkColumn = findFirstExistingColumn("material_specs", List.of("remark"));

		if (orderCodeColumn == null || prdColumn == null || colorColumn == null || orderBomColumn == null
				|| orderAmountColumn == null || specBomColumn == null) {
			return Collections.emptyList();
		}

		String styleIdExpression = selectStyleIdExpression(orderStyleIdColumn, specStyleIdColumn);
		String supplierExpression = selectCoalesce("mo.", orderVendorColumn, "ms.", supplierColumn, "supplier_code");
		String unitPriceExpression = selectCoalesce("mo.", orderUnitPriceColumn, "ms.", unitPriceColumn,
				"unit_price");

		String inboundJoin = "left join (select null as m_order_code, 0 as inbound_qty) mi on 1 = 0 ";
		if (hasTable("material_inbounds")) {
			inboundJoin = """
					left join (
						select m_order_code, coalesce(sum(received_qty), 0) as inbound_qty
						from material_inbounds
						group by m_order_code
					) mi on mi.m_order_code = mo.%s
					""".formatted(orderCodeColumn);
		}

		String outboundJoin = "";
		if (includeOutbound) {
			outboundJoin = "left join (select null as m_order_code, 0 as planned_qty, 0 as issued_qty) mo2 on 1 = 0 ";
			if (hasTable("material_outbounds")) {
				outboundJoin = """
					left join (
						select m_order_code,
						       coalesce(sum(planned_out_qty), 0) as planned_qty,
						       coalesce(sum(issued_out_qty), 0) as issued_qty
						from material_outbounds
						group by m_order_code
					) mo2 on mo2.m_order_code = mo.%s
					""".formatted(orderCodeColumn);
			}
		}

		StringBuilder sql = new StringBuilder()
				.append("select mo.").append(orderCodeColumn).append(" as m_order_code, ")
				.append("mo.").append(orderBomColumn).append(" as bom_id, ")
				.append(styleIdExpression).append(" as styles_id, ")
				.append(selectOrNull(qualifyColumn("ms.", specStyleCodeColumn), "style_code")).append(", ")
				.append(selectOrNull(qualifyColumn("ms.", categoryColumn), "category")).append(", ")
				.append(selectOrNull(qualifyColumn("ms.", materialNameColumn), "material_name")).append(", ")
				.append(selectOrNull(qualifyColumn("ms.", materialUsageColumn), "material_usage")).append(", ")
				.append(selectOrNull(qualifyColumn("ms.", specColumn), "spec")).append(", ")
				.append(selectOrNull(qualifyColumn("ms.", materialColorColumn), "material_color")).append(", ")
				.append(selectOrNull(qualifyColumn("ms.", uomColumn), "uom")).append(", ")
				.append(selectOrNull(qualifyColumn("ms.", qtyPerPieceColumn), "qty_per_piece")).append(", ")
				.append(supplierExpression).append(", ")
				.append(selectOrNull(qualifyColumn("ms.", orderUomColumn), "order_uom")).append(", ")
				.append(unitPriceExpression).append(", ")
				.append("mo.").append(orderAmountColumn).append(" as order_amount, ")
				.append("coalesce(mi.inbound_qty, 0) as inbound_qty, ")
				.append(includeOutbound ? "coalesce(mo2.planned_qty, 0) as planned_out_qty, "
						+ "coalesce(mo2.issued_qty, 0) as issued_out_qty, "
						: "0 as planned_out_qty, 0 as issued_out_qty, ")
				.append(selectOrNull(qualifyColumn("ms.", remarkColumn), "remark"))
				.append(" from material_orders mo join material_specs ms on mo.")
				.append(orderBomColumn).append(" = ms.").append(specBomColumn).append(" ")
				.append(inboundJoin)
				.append(outboundJoin)
				.append("where mo.").append(prdColumn).append(" = :prdAgreeId ")
				.append("and mo.").append(colorColumn).append(" = :colorCode ");

		String resolvedStyleId = resolveStyleId(stylesId, styleCode);
		if (StringUtils.hasText(resolvedStyleId) && orderStyleIdColumn != null) {
			sql.append("and mo.").append(orderStyleIdColumn).append(" = :stylesId ");
		} else if (StringUtils.hasText(styleCode) && specStyleCodeColumn != null) {
			sql.append("and ms.").append(specStyleCodeColumn).append(" = :styleCode ");
		} else if (StringUtils.hasText(resolvedStyleId) && specStyleIdColumn != null) {
			sql.append("and ms.").append(specStyleIdColumn).append(" = :stylesId ");
		}

		sql.append("order by mo.").append(orderCodeColumn).append(" asc");

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("prdAgreeId", prdAgreeId)
				.addValue("colorCode", colorCode).addValue("stylesId", resolvedStyleId)
				.addValue("styleCode", styleCode);

		List<MaterialTransactionLineView> rows = new ArrayList<>();
		List<String> supplierCodes = new ArrayList<>();
		jdbcTemplate.query(sql.toString(), params, rs -> {
			String supplierCode = rs.getString("supplier_code");
			if (StringUtils.hasText(supplierCode)) {
				supplierCodes.add(supplierCode);
			}
			MaterialTransactionLineView view = new MaterialTransactionLineView()
					.setMOrderCode(rs.getString("m_order_code"))
					.setBomId(rs.getObject("bom_id") != null ? rs.getLong("bom_id") : null)
					.setStylesId(rs.getString("styles_id"))
					.setStyleCode(resolveStyleCodeValue(rs.getString("style_code"), styleCode))
					.setPrdAgreeCode(prdAgreeCode)
					.setAgreementMonth(prdAgreeCode)
					.setColorCode(colorCode)
					.setCategory(rs.getString("category"))
					.setMaterialName(rs.getString("material_name"))
					.setMaterialUsage(rs.getString("material_usage"))
					.setSpec(rs.getString("spec"))
					.setMaterialColor(rs.getString("material_color"))
					.setUom(rs.getString("uom"))
					.setQtyPerPiece(rs.getBigDecimal("qty_per_piece"))
					.setSupplierCode(supplierCode)
					.setSupplierName(null)
					.setProductionManager(null)
					.setOrderUom(rs.getString("order_uom"))
					.setUnitPrice(rs.getBigDecimal("unit_price"))
					.setOrderQuantity(rs.getBigDecimal("order_amount"))
					.setInboundQuantity(rs.getBigDecimal("inbound_qty"))
					.setPlannedOutboundQuantity(rs.getBigDecimal("planned_out_qty"))
					.setOutboundQuantity(rs.getBigDecimal("issued_out_qty"))
					.setRemark(rs.getString("remark"));
			rows.add(view);
		});

		Map<String, String> supplierNames = findSupplierNames(new LinkedHashSet<>(supplierCodes));
		for (MaterialTransactionLineView view : rows) {
			view.setSupplierName(supplierNames.getOrDefault(view.getSupplierCode(), null));
		}
		return rows;
	}

	private BigDecimal sumInboundForOrder(String mOrderCode) {
		if (!hasTable("material_inbounds") || !StringUtils.hasText(mOrderCode)) {
			return BigDecimal.ZERO;
		}
		String qtyColumn = findFirstExistingColumn("material_inbounds", List.of("received_qty"));
		String mOrderColumn = findFirstExistingColumn("material_inbounds", List.of("m_order_code"));
		if (qtyColumn == null || mOrderColumn == null) {
			return BigDecimal.ZERO;
		}
		String sql = "select coalesce(sum(" + qtyColumn + "), 0) from material_inbounds where " + mOrderColumn
				+ " = :mOrderCode";
		MapSqlParameterSource params = new MapSqlParameterSource("mOrderCode", mOrderCode);
		BigDecimal result = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
		return result != null ? result : BigDecimal.ZERO;
	}

	private BigDecimal sumIssuedForOrder(String mOrderCode) {
		if (!hasTable("material_outbounds") || !StringUtils.hasText(mOrderCode)) {
			return BigDecimal.ZERO;
		}
		String qtyColumn = findFirstExistingColumn("material_outbounds", List.of("issued_out_qty"));
		String mOrderColumn = findFirstExistingColumn("material_outbounds", List.of("m_order_code"));
		if (qtyColumn == null || mOrderColumn == null) {
			return BigDecimal.ZERO;
		}
		String sql = "select coalesce(sum(" + qtyColumn + "), 0) from material_outbounds where " + mOrderColumn
				+ " = :mOrderCode";
		MapSqlParameterSource params = new MapSqlParameterSource("mOrderCode", mOrderCode);
		BigDecimal result = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
		return result != null ? result : BigDecimal.ZERO;
	}

	private BigDecimal safeDecimal(BigDecimal value) {
		return value != null ? value : BigDecimal.ZERO;
	}

	private Map<String, String> findSupplierNames(Set<String> supplierCodes) {
		if (supplierCodes == null || supplierCodes.isEmpty() || !hasTable("codes")) {
			return Collections.emptyMap();
		}

		String codeColumn = findFirstExistingColumn("codes", List.of("code", "id_code"));
		String codeTypeColumn = findFirstExistingColumn("codes", List.of("code_type", "id_code_type"));
		if (codeColumn == null) {
			return Collections.emptyMap();
		}

		String nameColumn = findFirstExistingColumn("codes", List.of("code_name", "name"));
		String activeColumn = findFirstExistingColumn("codes", List.of("is_active", "active"));
		String deletedColumn = findFirstExistingColumn("codes", List.of("deleted", "is_deleted"));

		StringBuilder sql = new StringBuilder().append("select ").append(codeColumn).append(" as code_value, ")
				.append(nameColumn != null ? nameColumn : "null").append(" as code_name ")
				.append(codeTypeColumn != null ? ", " + codeTypeColumn + " as code_type " : "")
				.append("from codes ").append("where ").append(codeColumn).append(" in (:codes) ");
		if (codeTypeColumn != null) {
			sql.append("and ").append(codeTypeColumn).append(" = 'CUSTOMER' ");
		}

		if (activeColumn != null) {
			sql.append("and ").append(activeColumn).append(" = true ");
		}
		if (deletedColumn != null) {
			sql.append("and ").append(deletedColumn).append(" = false ");
		}

		MapSqlParameterSource params = new MapSqlParameterSource("codes", supplierCodes);
		return jdbcTemplate.query(sql.toString(), params, rs -> {
			Map<String, String> result = new LinkedHashMap<>();
			while (rs.next()) {
				result.put(rs.getString("code_value"), rs.getString("code_name"));
			}
			return result;
		});
	}

	private String selectOrNull(String column, String alias) {
		return column != null ? column + " as " + alias : "null as " + alias;
	}

	private String selectCoalesce(String primaryPrefix, String primaryColumn, String fallbackPrefix,
			String fallbackColumn, String alias) {
		if (primaryColumn == null && fallbackColumn == null) {
			return "null as " + alias;
		}
		if (primaryColumn != null && fallbackColumn != null) {
			return "coalesce(" + primaryPrefix + primaryColumn + ", " + fallbackPrefix + fallbackColumn + ") as " + alias;
		}
		if (primaryColumn != null) {
			return primaryPrefix + primaryColumn + " as " + alias;
		}
		return fallbackPrefix + fallbackColumn + " as " + alias;
	}

	private String resolveStyleCodeValue(String resolved, String fallback) {
		if (StringUtils.hasText(resolved)) {
			return resolved;
		}
		return StringUtils.hasText(fallback) ? fallback : null;
	}

	private String selectStyleIdExpression(String orderStyleIdColumn, String specStyleIdColumn) {
		if (orderStyleIdColumn != null) {
			return "mo." + orderStyleIdColumn;
		}
		if (specStyleIdColumn != null) {
			return "ms." + specStyleIdColumn;
		}
		return "null";
	}

	private String qualifyColumn(String prefix, String column) {
		if (column == null) {
			return null;
		}
		return prefix + column;
	}

	private String resolveStyleId(String stylesId, String styleCode) {
		if (StringUtils.hasText(stylesId)) {
			return stylesId;
		}
		if (!StringUtils.hasText(styleCode) || !hasTable("styles")) {
			return null;
		}
		String idColumn = findFirstExistingColumn("styles", List.of("styles_id", "style_id", "id"));
		String codeColumn = findFirstExistingColumn("styles", List.of("style_code", "styles_code"));
		if (idColumn == null || codeColumn == null) {
			return null;
		}
		String sql = "select " + idColumn + " as styles_id from styles where " + codeColumn
				+ " = :styleCode limit 1";
		MapSqlParameterSource params = new MapSqlParameterSource("styleCode", styleCode);
		List<String> ids = jdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getString("styles_id"));
		return ids.isEmpty() ? null : ids.get(0);
	}

	private Long resolvePrdAgreeId(String agreementCode, String colorCode) {
		if (!StringUtils.hasText(agreementCode) || !StringUtils.hasText(colorCode)) {
			return null;
		}
		return productionAgreementRepository
				.findTopByAgreementCodeAndColorCodeOrderByPrdAgreeIdDesc(agreementCode.trim(), colorCode.trim())
				.map(ProductionAgreement::getPrdAgreeId).orElse(null);
	}

	private boolean hasTable(String tableName) {
		try {
			Integer count = jdbcTemplate.queryForObject("""
					select count(*)
					from information_schema.tables
					where upper(table_name) = upper(:tableName)
					""", new MapSqlParameterSource("tableName", tableName), Integer.class);
			return count != null && count > 0;
		} catch (Exception e) {
			return false;
		}
	}

	private boolean hasColumn(String tableName, String columnName) {
		try {
			Integer count = jdbcTemplate.queryForObject("""
					select count(*)
					from information_schema.columns
					where upper(table_name) = upper(:tableName)
					  and upper(column_name) = upper(:columnName)
					""",
					new MapSqlParameterSource().addValue("tableName", tableName).addValue("columnName", columnName),
					Integer.class);
			return count != null && count > 0;
		} catch (Exception e) {
			return false;
		}
	}

	private String findFirstExistingColumn(String tableName, List<String> candidates) {
		for (String candidate : candidates) {
			if (hasColumn(tableName, candidate)) {
				return candidate;
			}
		}
		return null;
	}
}
