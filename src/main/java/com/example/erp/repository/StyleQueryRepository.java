package com.example.erp.repository;

import com.example.erp.controller.dto.StyleSearchCondition;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class StyleQueryRepository {

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public StyleQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<StyleSnapshot> findStyles(StyleSearchCondition condition) {
		if (!hasTable("styles")) {
			return Collections.emptyList();
		}

		String styleIdColumn = findFirstExistingColumn("styles", List.of("styles_id", "style_id", "style_no"));
		String styleCodeColumn = findFirstExistingColumn("styles", List.of("style_code", "style_no"));
		if (styleCodeColumn == null) {
			return Collections.emptyList();
		}
		if (styleIdColumn == null) {
			styleIdColumn = styleCodeColumn;
		}

		String itemCodeColumn = findFirstExistingColumn("styles", List.of("item_code", "item"));
		String itemNameColumn = findFirstExistingColumn("styles", List.of("item_name"));
		String designerColumn = findFirstExistingColumn("styles", List.of("designer_emp_no", "designer"));
		String productColumn = findFirstExistingColumn("styles",
				List.of("product_emp_no", "production_emp_no", "production_manager"));
		String salesColumn = findFirstExistingColumn("styles", List.of("sales_emp_no", "sales_manager"));
		String logisticColumn = findFirstExistingColumn("styles", List.of("logistic_emp_no", "logistic_manager"));
		String startDateColumn = findFirstExistingColumn("styles", List.of("start_date"));
		String productionCostColumn = findFirstExistingColumn("styles", List.of("production_cost", "production_price"));
		String salesPriceColumn = findFirstExistingColumn("styles", List.of("sales_price"));
		String supplyPriceColumn = findFirstExistingColumn("styles", List.of("supply_price"));
		String activeColumn = findFirstExistingColumn("styles", List.of("is_active", "active"));

		boolean hasStyleRule = hasTable("styles_rule");
		String styleRuleStyleIdColumn = hasStyleRule
				? findFirstExistingColumn("styles_rule", List.of("styles_id", "style_id", "style_no"))
				: null;
		String codeTypeColumn = hasStyleRule ? findFirstExistingColumn("styles_rule", List.of("code_type")) : null;
		String codeColumn = hasStyleRule ? findFirstExistingColumn("styles_rule", List.of("code")) : null;
		boolean joinStyleRule = styleRuleStyleIdColumn != null && codeTypeColumn != null && codeColumn != null;

		String styleIdSelect = qualifyColumn("s", styleIdColumn);
		String styleCodeSelect = qualifyColumn("s", styleCodeColumn);
		String itemCodeSelect = qualifyColumn("s", itemCodeColumn);
		String itemNameSelect = qualifyColumn("s", itemNameColumn);
		String designerSelect = qualifyColumn("s", designerColumn);
		String productSelect = qualifyColumn("s", productColumn);
		String salesSelect = qualifyColumn("s", salesColumn);
		String logisticSelect = qualifyColumn("s", logisticColumn);
		String startDateSelect = qualifyColumn("s", startDateColumn);
		String productionCostSelect = qualifyColumn("s", productionCostColumn);
		String salesPriceSelect = qualifyColumn("s", salesPriceColumn);
		String supplyPriceSelect = qualifyColumn("s", supplyPriceColumn);
		String activeSelect = qualifyColumn("s", activeColumn);

		StringBuilder sql = new StringBuilder().append("select ").append(styleIdSelect).append(" as styles_id, ")
				.append(styleCodeSelect).append(" as style_code, ").append(selectOrNull(itemCodeSelect, "item_code"))
				.append(", ").append(selectOrNull(itemNameSelect, "item_name")).append(", ")
				.append(selectOrNull(designerSelect, "designer_emp_no")).append(", ")
				.append(selectOrNull(productSelect, "product_emp_no")).append(", ")
				.append(selectOrNull(salesSelect, "sales_emp_no")).append(", ")
				.append(selectOrNull(logisticSelect, "logistic_emp_no")).append(", ")
				.append(selectOrNull(startDateSelect, "start_date")).append(", ")
				.append(selectOrNull(productionCostSelect, "production_cost")).append(", ")
				.append(selectOrNull(salesPriceSelect, "sales_price")).append(", ")
				.append(selectOrNull(supplyPriceSelect, "supply_price")).append(", ")
				.append(selectOrNull(activeSelect, "is_active"));

		if (joinStyleRule) {
			String codeTypeSelect = qualifyColumn("sr", codeTypeColumn);
			String codeSelect = qualifyColumn("sr", codeColumn);
			sql.append(", group_concat(distinct case when ").append(codeTypeSelect)
					.append(" = 'COLOR' then ").append(codeSelect).append(" end ")
					.append("order by case when ").append(codeTypeSelect).append(" = 'COLOR' then ").append(codeSelect)
					.append(" end separator ', ') as colors");
			sql.append(", group_concat(distinct case when ").append(codeTypeSelect)
					.append(" = 'SIZE' then ").append(codeSelect).append(" end ")
					.append("order by case when ").append(codeTypeSelect).append(" = 'SIZE' then cast(")
					.append(codeSelect).append(" as unsigned) end separator ', ') as sizes");
			sql.append(" from styles s left join styles_rule sr on ").append(qualifyColumn("sr", styleRuleStyleIdColumn))
					.append(" = ").append(styleIdSelect).append(" ");
		} else {
			sql.append(", null as colors, null as sizes from styles s ");
		}

		MapSqlParameterSource params = new MapSqlParameterSource();
		if (StringUtils.hasText(condition.getStyleCodeLike())) {
			sql.append("where ").append(styleCodeSelect).append(" like concat('%', :styleCode, '%') ");
			params.addValue("styleCode", condition.getStyleCodeLike().trim());
		}
		String itemSearchColumn = itemNameSelect != null ? itemNameSelect : itemCodeSelect;
		if (StringUtils.hasText(condition.getItemLike()) && itemSearchColumn != null) {
			sql.append(params.hasValue("styleCode") ? "and " : "where ").append(itemSearchColumn)
					.append(" like concat('%', :item, '%') ");
			params.addValue("item", condition.getItemLike().trim());
		}
		if (StringUtils.hasText(condition.getDesignerNameLike()) && designerSelect != null) {
			sql.append(params.hasValue("styleCode") || params.hasValue("item") ? "and " : "where ")
					.append(designerSelect).append(" like concat('%', :designer, '%') ");
			params.addValue("designer", condition.getDesignerNameLike().trim());
		}
		if (condition.getIsActive() != null && activeColumn != null) {
			sql.append(params.hasValue("styleCode") || params.hasValue("item") || params.hasValue("designer") ? "and "
					: "where ").append(activeSelect).append(" = :isActive ");
			params.addValue("isActive", condition.getIsActive());
		}

		if (joinStyleRule) {
			List<String> groupByColumns = new ArrayList<>();
			addGroupByColumn(groupByColumns, styleIdSelect);
			addGroupByColumn(groupByColumns, styleCodeSelect);
			addGroupByColumn(groupByColumns, itemCodeSelect);
			addGroupByColumn(groupByColumns, itemNameSelect);
			addGroupByColumn(groupByColumns, designerSelect);
			addGroupByColumn(groupByColumns, productSelect);
			addGroupByColumn(groupByColumns, salesSelect);
			addGroupByColumn(groupByColumns, logisticSelect);
			addGroupByColumn(groupByColumns, startDateSelect);
			addGroupByColumn(groupByColumns, productionCostSelect);
			addGroupByColumn(groupByColumns, salesPriceSelect);
			addGroupByColumn(groupByColumns, supplyPriceSelect);
			addGroupByColumn(groupByColumns, activeSelect);
			if (!groupByColumns.isEmpty()) {
				sql.append("group by ").append(String.join(", ", groupByColumns)).append(" ");
			}
		}

		sql.append("order by ").append(styleCodeSelect).append(" asc");

		return jdbcTemplate.query(sql.toString(), params,
				(rs, rowNum) -> new StyleSnapshot(rs.getString("styles_id"), rs.getString("style_code"),
						rs.getString("item_code"), rs.getString("item_name"), rs.getString("designer_emp_no"),
						rs.getString("product_emp_no"), rs.getString("sales_emp_no"), rs.getString("logistic_emp_no"),
						toLocalDate(rs.getDate("start_date")), rs.getBigDecimal("production_cost"),
						rs.getBigDecimal("sales_price"), rs.getBigDecimal("supply_price"),
						readBoolean(rs.getObject("is_active")), rs.getString("colors"), rs.getString("sizes")));
	}

	public List<StyleRuleRow> findStyleRules(Set<String> styleIds) {
		if (styleIds == null || styleIds.isEmpty() || !hasTable("styles_rule")) {
			return Collections.emptyList();
		}

		String styleIdColumn = findFirstExistingColumn("styles_rule", List.of("styles_id", "style_id", "style_no"));
		String codeTypeColumn = findFirstExistingColumn("styles_rule", List.of("code_type"));
		String codeColumn = findFirstExistingColumn("styles_rule", List.of("code"));
		if (styleIdColumn == null || codeTypeColumn == null || codeColumn == null) {
			return Collections.emptyList();
		}

		String sql = """
				select %s as styles_id,
				       %s as code_type,
				       %s as code
				from styles_rule
				where %s in (:styleIds)
				""".formatted(styleIdColumn, codeTypeColumn, codeColumn, styleIdColumn);

		MapSqlParameterSource params = new MapSqlParameterSource("styleIds", styleIds);
		return jdbcTemplate.query(sql, params, (rs, rowNum) -> new StyleRuleRow(rs.getString("styles_id"),
				rs.getString("code_type"), rs.getString("code")));
	}

	public Map<CodeKey, String> findCodeNames(Set<CodeKey> keys) {
		if (keys == null || keys.isEmpty() || !hasTable("codes")) {
			return Collections.emptyMap();
		}

		String codeTypeColumn = findFirstExistingColumn("codes", List.of("code_type"));
		String codeColumn = findFirstExistingColumn("codes", List.of("code"));
		String codeNameColumn = findFirstExistingColumn("codes", List.of("code_name", "name"));
		if (codeTypeColumn == null || codeColumn == null || codeNameColumn == null) {
			return Collections.emptyMap();
		}

		Set<String> types = new LinkedHashSet<>();
		Set<String> codes = new LinkedHashSet<>();
		for (CodeKey key : keys) {
			if (StringUtils.hasText(key.codeType()) && StringUtils.hasText(key.code())) {
				types.add(key.codeType());
				codes.add(key.code());
			}
		}
		if (types.isEmpty() || codes.isEmpty()) {
			return Collections.emptyMap();
		}

		String sql = """
				select %s as code_type,
				       %s as code,
				       %s as code_name
				from codes
				where %s in (:types)
				  and %s in (:codes)
				""".formatted(codeTypeColumn, codeColumn, codeNameColumn, codeTypeColumn, codeColumn);

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("types", types).addValue("codes", codes);
		Map<CodeKey, String> result = new LinkedHashMap<>();
		jdbcTemplate.query(sql, params, (RowCallbackHandler) rs -> {
			CodeKey key = new CodeKey(rs.getString("code_type"), rs.getString("code"));
			result.put(key, rs.getString("code_name"));
		});
		return result;
	}

	public Map<String, String> findItemNames(Set<String> itemCodes) {
		if (itemCodes == null || itemCodes.isEmpty() || !hasTable("items")) {
			return Collections.emptyMap();
		}

		String itemCodeColumn = findFirstExistingColumn("items", List.of("item_code", "code"));
		String itemNameColumn = findFirstExistingColumn("items", List.of("item_name", "name"));
		if (itemCodeColumn == null || itemNameColumn == null) {
			return Collections.emptyMap();
		}

		String sql = """
				select %s as item_code,
				       %s as item_name
				from items
				where %s in (:codes)
				""".formatted(itemCodeColumn, itemNameColumn, itemCodeColumn);

		MapSqlParameterSource params = new MapSqlParameterSource("codes", itemCodes);
		Map<String, String> result = new LinkedHashMap<>();
		jdbcTemplate.query(sql, params,
				(RowCallbackHandler) rs -> result.put(rs.getString("item_code"), rs.getString("item_name")));
		return result;
	}

	public Map<String, String> findUserNamesByEmpNos(Set<String> empNos) {
		return findUserNamesByColumn("emp_no", empNos);
	}

	public Map<String, String> findUserNamesByUsernames(Set<String> usernames) {
		return findUserNamesByColumn("username", usernames);
	}

	public Map<String, String> findUserNamesByUserIds(Set<String> userIds) {
		return findUserNamesByColumn("user_id", userIds);
	}

	public Map<String, String> findUserNamesByEmpCodes(Set<String> empCodes) {
		return findUserNamesByColumn("emp_code", empCodes);
	}

	public boolean hasColumn(String tableName, String columnName) {
		if (!StringUtils.hasText(tableName) || !StringUtils.hasText(columnName)) {
			return false;
		}
		try {
			MapSqlParameterSource params = new MapSqlParameterSource().addValue("tableName", tableName)
					.addValue("columnName", columnName);
			Integer count = jdbcTemplate.queryForObject("""
					select count(*)
					from information_schema.columns
					where upper(table_name) = upper(:tableName)
					  and upper(column_name) = upper(:columnName)
					""", params, Integer.class);
			return count != null && count > 0;
		} catch (Exception e) {
			return false;
		}
	}

	private Map<String, String> findUserNamesByColumn(String columnName, Set<String> values) {
		if (values == null || values.isEmpty() || !hasTable("users") || !hasColumn("users", columnName)) {
			return Collections.emptyMap();
		}

		if (!hasColumn("users", "name")) {
			return Collections.emptyMap();
		}

		String sql = """
				select %s as user_key,
				       name
				from users
				where %s in (:values)
				""".formatted(columnName, columnName);

		MapSqlParameterSource params = new MapSqlParameterSource("values", values);
		Map<String, String> result = new LinkedHashMap<>();
		jdbcTemplate.query(sql, params, (RowCallbackHandler) rs -> result.put(rs.getString("user_key"),
                rs.getString("name")));
		return result;
	}

	private boolean hasTable(String tableName) {
		if (!StringUtils.hasText(tableName)) {
			return false;
		}
		try {
			MapSqlParameterSource params = new MapSqlParameterSource("tableName", tableName);
			Integer count = jdbcTemplate.queryForObject("""
					select count(*)
					from information_schema.tables
					where upper(table_name) = upper(:tableName)
					""", params, Integer.class);
			return count != null && count > 0;
		} catch (Exception e) {
			return false;
		}
	}

	private String findFirstExistingColumn(String tableName, List<String> candidates) {
		if (candidates == null) {
			return null;
		}
		for (String candidate : candidates) {
			if (hasColumn(tableName, candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private String selectOrNull(String columnName, String alias) {
		if (!StringUtils.hasText(columnName)) {
			return "null as " + alias;
		}
		if (StringUtils.hasText(alias)) {
			return columnName + " as " + alias;
		}
		return columnName;
	}

	private String qualifyColumn(String alias, String columnName) {
		if (!StringUtils.hasText(columnName)) {
			return null;
		}
		if (!StringUtils.hasText(alias)) {
			return columnName;
		}
		return alias + "." + columnName;
	}

	private void addGroupByColumn(List<String> groupByColumns, String column) {
		if (StringUtils.hasText(column)) {
			groupByColumns.add(column);
		}
	}

	private Boolean readBoolean(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Boolean value) {
			return value;
		}
		if (raw instanceof Number number) {
			return number.intValue() != 0;
		}
		return Boolean.parseBoolean(raw.toString());
	}

	private LocalDate toLocalDate(Date date) {
		if (date == null) {
			return null;
		}
		return date.toLocalDate();
	}

	public record StyleSnapshot(String stylesId, String styleCode, String itemCode, String itemName,
			String designerEmpNo, String productEmpNo, String salesEmpNo, String logisticEmpNo, LocalDate startDate, BigDecimal productionCost, 
			BigDecimal salesPrice, BigDecimal supplyPrice, Boolean active, String colors, String sizes) {
	}

	public record StyleRuleRow(String stylesId, String codeType, String code) {
	}

	public record CodeKey(String codeType, String code) {
	}
}
