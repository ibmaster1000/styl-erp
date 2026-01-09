package com.example.erp.service;

import com.example.erp.controller.dto.MaterialTransactionLineView;
import com.example.erp.controller.dto.MaterialTransactionSaveLine;
import com.example.erp.controller.dto.MaterialTransactionSaveRequest;
import com.example.erp.controller.dto.SimpleCodeView;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
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

	public MaterialTransactionService(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<MaterialTransactionLineView> findMaterials(String stylesId, String styleCode, String prdAgreeCode,
			String colorCode) {
		if (!hasTable("material_specs")) {
			return Collections.emptyList();
		}

		String prdColumn = findFirstExistingColumn("material_specs", List.of("prd_agree_code", "agreement_code"));
		String colorColumn = findFirstExistingColumn("material_specs",
				List.of("color_code", "material_color", "color"));
		String styleIdColumn = findFirstExistingColumn("material_specs", List.of("styles_id", "style_id"));
		String styleCodeColumn = findFirstExistingColumn("material_specs", List.of("style_code"));
		String categoryColumn = findFirstExistingColumn("material_specs", List.of("category"));
		String materialNameColumn = findFirstExistingColumn("material_specs", List.of("material_name"));
		String materialUsageColumn = findFirstExistingColumn("material_specs", List.of("material_usage"));
		String specColumn = findFirstExistingColumn("material_specs", List.of("spec"));
		String bomIdColumn = findFirstExistingColumn("material_specs", List.of("bom_id", "material_spec_id"));
		String materialColorColumn = findFirstExistingColumn("material_specs", List.of("material_color"));
		String uomColumn = findFirstExistingColumn("material_specs", List.of("uom"));
		String qtyPerPieceColumn = findFirstExistingColumn("material_specs", List.of("qty_per_piece"));
		String supplierColumn = findFirstExistingColumn("material_specs", List.of("supplier_code"));
		String orderUomColumn = findFirstExistingColumn("material_specs", List.of("order_uom"));
		String unitPriceColumn = findFirstExistingColumn("material_specs", List.of("unit_price"));
		String remarkColumn = findFirstExistingColumn("material_specs", List.of("remark"));

		if (prdColumn == null || colorColumn == null) {
			return Collections.emptyList();
		}

		String sql = """
				select %s as bom_id,
				       %s as styles_id,
				       %s as prd_agree_code,
				       %s as color_code,
				       %s as category,
				       %s as material_name,
				       %s as material_usage,
				       %s as spec,
				       %s as material_color,
				       %s as uom,
				       %s as qty_per_piece,
				       %s as supplier_code,
				       %s as order_uom,
				       %s as unit_price,
				       %s as remark
				from material_specs
				where %s = :prdAgreeCode
				  and %s = :colorCode
				%s
				order by coalesce(%s, 0) asc
				""".formatted(selectOrNull(bomIdColumn, "bom_id"),
				selectOrNull(styleIdColumn != null ? styleIdColumn : styleCodeColumn, "styles_id"),
				selectOrNull(prdColumn, "prd_agree_code"), selectOrNull(colorColumn, "color_code"),
				selectOrNull(categoryColumn, "category"), selectOrNull(materialNameColumn, "material_name"),
				selectOrNull(materialUsageColumn, "material_usage"), selectOrNull(specColumn, "spec"),
				selectOrNull(materialColorColumn, "material_color"), selectOrNull(uomColumn, "uom"),
				selectOrNull(qtyPerPieceColumn, "qty_per_piece"), selectOrNull(supplierColumn, "supplier_code"),
				selectOrNull(orderUomColumn, "order_uom"), selectOrNull(unitPriceColumn, "unit_price"),
				selectOrNull(remarkColumn, "remark"), prdColumn, colorColumn,
				buildStyleCondition(styleIdColumn, styleCodeColumn, stylesId, styleCode),
				bomIdColumn != null ? bomIdColumn : prdColumn);

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("prdAgreeCode", prdAgreeCode)
				.addValue("colorCode", colorCode).addValue("stylesId", resolveStyleId(stylesId, styleCode))
				.addValue("styleCode", styleCode);

		List<MaterialTransactionLineView> rows = new ArrayList<>();
		List<String> supplierCodes = new ArrayList<>();
		jdbcTemplate.query(sql, params, rs -> {
			Long bomId = rs.getObject("bom_id") != null ? rs.getLong("bom_id") : null;
			String supCode = rs.getString("supplier_code");
			if (StringUtils.hasText(supCode)) {
				supplierCodes.add(supCode);
			}
			rows.add(new MaterialTransactionLineView().setBomId(bomId).setStylesId(rs.getString("styles_id"))
					.setStyleCode(StringUtils.hasText(styleCode) ? styleCode : rs.getString("styles_id"))
					.setPrdAgreeCode(rs.getString("prd_agree_code")).setAgreementMonth(rs.getString("prd_agree_code"))
					.setColorCode(rs.getString("color_code")).setCategory(rs.getString("category"))
					.setMaterialName(rs.getString("material_name")).setMaterialUsage(rs.getString("material_usage"))
					.setSpec(rs.getString("spec")).setMaterialColor(rs.getString("material_color"))
					.setUom(rs.getString("uom")).setQtyPerPiece(rs.getBigDecimal("qty_per_piece"))
					.setSupplierCode(supCode).setSupplierName(null).setProductionManager(null)
					.setOrderUom(rs.getString("order_uom")).setUnitPrice(rs.getBigDecimal("unit_price"))
					.setOrderQuantity(null).setInboundQuantity(BigDecimal.ZERO).setOutboundQuantity(BigDecimal.ZERO)
					.setRemark(rs.getString("remark")));
		});

		Map<String, String> supplierNames = findSupplierNames(new LinkedHashSet<>(supplierCodes));
		for (MaterialTransactionLineView view : rows) {
			BigDecimal inbound = sumTransaction(view, "IN");
			BigDecimal outbound = sumTransaction(view, "OUT");
			BigDecimal orderQty = findOrderQuantity(view);
			view.setSupplierName(supplierNames.getOrDefault(view.getSupplierCode(), null)).setOrderQuantity(orderQty)
					.setInboundQuantity(inbound).setOutboundQuantity(outbound);
		}
		return rows;
	}

	@Transactional
	public Map<String, Object> saveInbound(MaterialTransactionSaveRequest request, String empNo) {
		return saveTransactions(request, empNo, "IN", false);
	}

	@Transactional
	public Map<String, Object> saveOutbound(MaterialTransactionSaveRequest request, String empNo) {
		return saveTransactions(request, empNo, "OUT", true);
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
				where %s in ('FACTORY', 'CUSTOMER')
				%s
				%s
				order by %s asc, %s asc
				""".formatted(codeColumn, codeTypeColumn, nameColumn != null ? nameColumn : "null", codeTypeColumn,
				activeColumn != null ? "and " + activeColumn + " = true" : "",
				deletedColumn != null ? "and " + deletedColumn + " = false" : "", codeTypeColumn, codeColumn);

		MapSqlParameterSource params = new MapSqlParameterSource();
		List<SimpleCodeView> list = new ArrayList<>();
		jdbcTemplate.query(sql, params, rs -> {
			list.add(new SimpleCodeView(rs.getString("code_value"), rs.getString("code_name")));
		});
		return list;
	}

	private Map<String, Object> saveTransactions(MaterialTransactionSaveRequest request, String empNo, String tranType,
			boolean requireFactory) {
		if (!hasTable("material_transactions")) {
			return Map.of("success", false, "created", 0, "skipped", 0);
		}

		String quantityColumn = findFirstExistingColumn("material_transactions", List.of("quantity", "qty"));
		String tranTypeColumn = findFirstExistingColumn("material_transactions", List.of("tran_type", "type"));
		String tranDatetimeColumn = findFirstExistingColumn("material_transactions",
				List.of("tran_datetime", "tran_date", "datetime"));
		String styleIdColumn = findFirstExistingColumn("material_transactions", List.of("styles_id", "style_id"));
		String prdColumn = findFirstExistingColumn("material_transactions",
				List.of("prd_agree_code", "agreement_code"));
		String colorColumn = findFirstExistingColumn("material_transactions", List.of("color_code"));
		String specColumn = findFirstExistingColumn("material_transactions", List.of("material_spec_id", "bom_id"));
		String orderUomColumn = findFirstExistingColumn("material_transactions", List.of("order_uom"));
		String unitPriceColumn = findFirstExistingColumn("material_transactions", List.of("unit_price"));
		String factoryColumn = findFirstExistingColumn("material_transactions",
				List.of("factory_code", "target_factory"));
		String createdByColumn = findFirstExistingColumn("material_transactions",
				List.of("created_by", "created_user"));
		String remarkColumn = findFirstExistingColumn("material_transactions",
				List.of("remark", "memo", "description"));
		String tranDateColumn = findFirstExistingColumn("material_transactions",
				List.of("tran_date", "transaction_date"));

		if (quantityColumn == null || tranTypeColumn == null || specColumn == null) {
			return Map.of("success", false, "created", 0, "skipped", 0);
		}
		List<MaterialTransactionSaveLine> items = request.getItems();
		if (CollectionUtils.isEmpty(items)) {
			return Map.of("success", false, "created", 0, "skipped", 0);
		}

		int created = 0;
		int skipped = 0;
		for (MaterialTransactionSaveLine line : items) {
			BigDecimal qty = line.getQuantity();
			if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
				skipped++;
				continue;
			}
			if (requireFactory && !StringUtils.hasText(line.getFactoryCode())) {
				skipped++;
				continue;
			}
			if ("OUT".equalsIgnoreCase(tranType)) {
				BigDecimal available = sumAvailableForOutbound(request, line);
				if (available != null && available.compareTo(qty) < 0) {
					skipped++;
					continue;
				}
			}

			MapSqlParameterSource params = new MapSqlParameterSource()
					.addValue("stylesId", resolveStyleId(request.getStylesId(), request.getStyleCode()))
					.addValue("styleCode", request.getStyleCode()).addValue("prdAgreeCode", request.getPrdAgreeCode())
					.addValue("colorCode", request.getColorCode()).addValue("bomId", line.getBomId())
					.addValue("quantity", qty).addValue("orderUom", line.getOrderUom())
					.addValue("unitPrice", line.getUnitPrice()).addValue("factoryCode", line.getFactoryCode())
					.addValue("createdBy", empNo).addValue("tranDateOnly", toSqlDate(request.getTranDate()))
					.addValue("tranDatetime", Timestamp.valueOf(LocalDateTime.now()))
					.addValue("remark", line.getRemark());

			List<String> columns = new ArrayList<>();
			List<String> values = new ArrayList<>();
			columns.add(tranTypeColumn);
			values.add(":tranType");
			params.addValue("tranType", tranType);
			if (tranDatetimeColumn != null) {
				columns.add(tranDatetimeColumn);
				values.add(":tranDatetime");
			}
			if (styleIdColumn != null) {
				columns.add(styleIdColumn);
				values.add(":stylesId");
			}
			if (prdColumn != null) {
				columns.add(prdColumn);
				values.add(":prdAgreeCode");
			}
			if (colorColumn != null) {
				columns.add(colorColumn);
				values.add(":colorCode");
			}
			columns.add(specColumn);
			values.add(":bomId");
			columns.add(quantityColumn);
			values.add(":quantity");
			if (orderUomColumn != null) {
				columns.add(orderUomColumn);
				values.add(":orderUom");
			}
			if (unitPriceColumn != null) {
				columns.add(unitPriceColumn);
				values.add(":unitPrice");
			}
			if (factoryColumn != null && StringUtils.hasText(line.getFactoryCode())) {
				columns.add(factoryColumn);
				values.add(":factoryCode");
			}
			if (createdByColumn != null) {
				columns.add(createdByColumn);
				values.add(":createdBy");
			}
			if (remarkColumn != null) {
				columns.add(remarkColumn);
				values.add(":remark");
			}
			if (tranDateColumn != null && params.getValue("tranDateOnly") != null) {
				columns.add(tranDateColumn);
				values.add(":tranDateOnly");
			}

			String sql = "insert into material_transactions (" + String.join(", ", columns) + ") values ("
					+ String.join(", ", values) + ")";
			jdbcTemplate.update(sql, params);
			created++;
		}

		return Map.of("success", created > 0, "created", created, "skipped", skipped);
	}

	private BigDecimal findOrderQuantity(MaterialTransactionLineView view) {
		if (!hasTable("material_orders")) {
			return BigDecimal.ZERO;
		}
		String prdColumn = findFirstExistingColumn("material_orders", List.of("prd_agree_code", "agreement_code"));
		String colorColumn = findFirstExistingColumn("material_orders", List.of("color_code"));
		String bomIdColumn = findFirstExistingColumn("material_orders", List.of("bom_id"));
		String styleIdColumn = findFirstExistingColumn("material_orders", List.of("styles_id", "style_id"));
		String amountColumn = findFirstExistingColumn("material_orders",
				List.of("order_amount", "amount", "qty", "quantity"));
		if (prdColumn == null || colorColumn == null || bomIdColumn == null || amountColumn == null) {
			return BigDecimal.ZERO;
		}

		StringBuilder sql = new StringBuilder().append("select coalesce(sum(").append(amountColumn).append("), 0) ")
				.append("from material_orders where ").append(prdColumn).append(" = :prdAgreeCode ").append("and ")
				.append(colorColumn).append(" = :colorCode ").append("and ").append(bomIdColumn).append(" = :bomId ");
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("prdAgreeCode", view.getPrdAgreeCode())
				.addValue("colorCode", view.getColorCode()).addValue("bomId", view.getBomId());
		if (styleIdColumn != null && StringUtils.hasText(view.getStylesId())) {
			sql.append("and ").append(styleIdColumn).append(" = :stylesId ");
			params.addValue("stylesId", view.getStylesId());
		}
		BigDecimal sum = jdbcTemplate.queryForObject(sql.toString(), params, BigDecimal.class);
		return sum != null ? sum : BigDecimal.ZERO;
	}

	private BigDecimal sumTransaction(MaterialTransactionLineView view, String tranType) {
		if (!hasTable("material_transactions")) {
			return BigDecimal.ZERO;
		}
		String tranTypeColumn = findFirstExistingColumn("material_transactions", List.of("tran_type", "type"));
		String quantityColumn = findFirstExistingColumn("material_transactions", List.of("quantity", "qty"));
		String prdColumn = findFirstExistingColumn("material_transactions",
				List.of("prd_agree_code", "agreement_code"));
		String colorColumn = findFirstExistingColumn("material_transactions", List.of("color_code"));
		String specColumn = findFirstExistingColumn("material_transactions", List.of("material_spec_id", "bom_id"));
		String styleIdColumn = findFirstExistingColumn("material_transactions", List.of("styles_id", "style_id"));

		if (tranTypeColumn == null || quantityColumn == null || specColumn == null) {
			return BigDecimal.ZERO;
		}

		StringBuilder sql = new StringBuilder().append("select coalesce(sum(").append(quantityColumn)
				.append("), 0) from material_transactions ").append("where ").append(tranTypeColumn)
				.append(" = :tranType ").append("and ").append(specColumn).append(" = :bomId ");
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("tranType", tranType).addValue("bomId",
				view.getBomId());
		if (prdColumn != null && StringUtils.hasText(view.getPrdAgreeCode())) {
			sql.append("and ").append(prdColumn).append(" = :prdAgreeCode ");
			params.addValue("prdAgreeCode", view.getPrdAgreeCode());
		}
		if (colorColumn != null && StringUtils.hasText(view.getColorCode())) {
			sql.append("and ").append(colorColumn).append(" = :colorCode ");
			params.addValue("colorCode", view.getColorCode());
		}
		if (styleIdColumn != null && StringUtils.hasText(view.getStylesId())) {
			sql.append("and ").append(styleIdColumn).append(" = :stylesId ");
			params.addValue("stylesId", view.getStylesId());
		}

		BigDecimal result = jdbcTemplate.queryForObject(sql.toString(), params, BigDecimal.class);
		return result != null ? result : BigDecimal.ZERO;
	}

	private BigDecimal sumAvailableForOutbound(MaterialTransactionSaveRequest request,
			MaterialTransactionSaveLine line) {
		MaterialTransactionLineView mockView = new MaterialTransactionLineView().setBomId(line.getBomId())
				.setStylesId(resolveStyleId(request.getStylesId(), request.getStyleCode()))
				.setStyleCode(request.getStyleCode()).setPrdAgreeCode(request.getPrdAgreeCode())
				.setAgreementMonth(request.getPrdAgreeCode()).setColorCode(request.getColorCode());
		BigDecimal inbound = sumTransaction(mockView, "IN");
		BigDecimal outbound = sumTransaction(mockView, "OUT");
		return inbound.subtract(outbound);
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
				.append(codeTypeColumn != null ? ", " + codeTypeColumn + " as code_type " : "").append("from codes ")
				.append("where ").append(codeColumn).append(" in (:codes) ");

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

	private String buildStyleCondition(String styleIdColumn, String styleCodeColumn, String stylesId,
			String styleCode) {
		if (styleIdColumn == null && styleCodeColumn == null) {
			return "";
		}
		if (StringUtils.hasText(stylesId) && styleIdColumn != null) {
			return "and " + styleIdColumn + " = :stylesId";
		}
		if (StringUtils.hasText(styleCode) && styleCodeColumn != null) {
			return "and " + styleCodeColumn + " = :styleCode";
		}
		return "";
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
		String sql = "select " + idColumn + " as styles_id from styles where " + codeColumn + " = :styleCode limit 1";
		MapSqlParameterSource params = new MapSqlParameterSource("styleCode", styleCode);
		List<String> ids = jdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getString("styles_id"));
		return ids.isEmpty() ? null : ids.get(0);
	}

	private Date toSqlDate(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			return Date.valueOf(LocalDate.parse(raw));
		} catch (Exception e) {
			return null;
		}
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