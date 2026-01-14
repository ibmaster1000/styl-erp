package com.example.erp.service;

import com.example.erp.controller.dto.MaterialOrderItemView;
import com.example.erp.controller.dto.MaterialOrderLineRow;
import com.example.erp.controller.dto.MaterialOrderRequest;
import com.example.erp.controller.dto.MaterialOrderSelection;
import com.example.erp.controller.dto.MaterialOrderStyleResult;
import com.example.erp.controller.dto.MaterialOrderSupplierView;
import com.example.erp.domain.ProductionAgreement;
import com.example.erp.repository.ProductionAgreementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MaterialOrderService {

	private static final Logger log = LoggerFactory.getLogger(MaterialOrderService.class);

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final StyleRuleService styleRuleService;
	private final ProductionAgreementRepository productionAgreementRepository;

	public MaterialOrderService(NamedParameterJdbcTemplate jdbcTemplate,
			StyleRuleService styleRuleService,
			ProductionAgreementRepository productionAgreementRepository) {
		this.jdbcTemplate = jdbcTemplate;
		this.styleRuleService = styleRuleService;
		this.productionAgreementRepository = productionAgreementRepository;
	}

	public List<MaterialOrderStyleResult> searchStyles(String keyword) {
		if (!hasTable("styles")) {
			return Collections.emptyList();
		}

		String idColumn = resolveStyleIdColumn();
		String codeColumn = resolveStyleCodeColumn();
		if (idColumn == null || codeColumn == null) {
			return Collections.emptyList();
		}

		String itemColumn = hasColumn("styles", "item") ? "item" : null;
		String activeColumn = findFirstExistingColumn("styles", List.of("is_active", "active"));

		StringBuilder sql = new StringBuilder().append("select ").append(idColumn).append(" as styles_id, ")
				.append(codeColumn).append(" as style_code, ").append(itemColumn != null ? itemColumn : "null")
				.append(" as item, ").append(activeColumn != null ? activeColumn : "false").append(" as is_active ")
				.append("from styles ");

		MapSqlParameterSource params = new MapSqlParameterSource();
		if (StringUtils.hasText(keyword)) {
			sql.append("where (").append(codeColumn).append(" like concat('%', :kw, '%') ");
			if (itemColumn != null) {
				sql.append("or ").append(itemColumn).append(" like concat('%', :kw, '%') ");
			}
			sql.append(") ");
			params.addValue("kw", keyword.trim());
		}
		sql.append("order by ").append(codeColumn).append(" asc limit 50");

		return jdbcTemplate.query(sql.toString(), params,
				(rs, rowNum) -> new MaterialOrderStyleResult(rs.getString("styles_id"), rs.getString("style_code"),
						rs.getString("item"), rs.getBoolean("is_active")));
	}

	public MaterialOrderSelection loadSelection(String stylesId, String styleCode) {
		String effectiveStyleCode = StringUtils.hasText(styleCode) ? styleCode : resolveStyleCode(stylesId);
		Map<String, List<String>> styleRule = Collections.emptyMap();
		if (StringUtils.hasText(effectiveStyleCode)) {
			styleRule = styleRuleService.findRulesByStyleCodes(Set.of(effectiveStyleCode))
					.getOrDefault(effectiveStyleCode, Collections.emptyMap());
		}

		List<String> colors = !styleRule.isEmpty() ? new ArrayList<>(styleRule.keySet())
				: findColorsFromSpecs(stylesId, effectiveStyleCode);
		colors.sort(String::compareToIgnoreCase);
		List<String> agreements = findAgreements(stylesId, effectiveStyleCode);

		return new MaterialOrderSelection(stylesId, effectiveStyleCode, colors, agreements, styleRule);
	}

	public List<MaterialOrderSupplierView> findSuppliers(String stylesId, String agreementCode, String colorCode) {
		Long prdAgreeId = resolvePrdAgreeId(agreementCode, colorCode);
		if (prdAgreeId == null || !StringUtils.hasText(colorCode) || !hasTable("material_specs")) {
			return Collections.emptyList();
		}

		String supplierColumn = findFirstExistingColumn("material_specs", List.of("supplier_code"));
		String prdColumn = findFirstExistingColumn("material_specs", List.of("prd_agree_id"));
		String colorColumn = findFirstExistingColumn("material_specs", List.of("color_code"));
		String styleIdColumn = findFirstExistingColumn("material_specs", List.of("styles_id", "style_id"));

		if (supplierColumn == null || prdColumn == null || colorColumn == null) {
			return Collections.emptyList();
		}

		StringBuilder sql = new StringBuilder().append("select distinct ").append(supplierColumn)
				.append(" as supplier_code ").append("from material_specs ").append("where ").append(prdColumn)
				.append(" = :prdAgreeId ").append("and ").append(colorColumn).append(" = :colorCode ")
				.append("and ").append(supplierColumn).append(" is not null ")
				.append("and ").append(supplierColumn).append(" <> '' ");
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("prdAgreeId", prdAgreeId)
				.addValue("colorCode", colorCode);

		if (StringUtils.hasText(stylesId) && styleIdColumn != null) {
			sql.append("and ").append(styleIdColumn).append(" = :stylesId ");
			params.addValue("stylesId", stylesId);
		}

		List<String> supplierCodes = jdbcTemplate.query(sql.toString(), params,
				(rs, rowNum) -> rs.getString("supplier_code"));
		if (supplierCodes.isEmpty()) {
			return Collections.emptyList();
		}

		Map<String, String> supplierNames = findSupplierNames(new LinkedHashSet<>(supplierCodes));
		List<MaterialOrderSupplierView> result = new ArrayList<>();
		for (String code : supplierCodes) {
			boolean ordered = isSupplierAlreadyOrdered(stylesId, prdAgreeId, colorCode, code);
			result.add(new MaterialOrderSupplierView(code, supplierNames.getOrDefault(code, "-"), ordered));
		}
		return result;
	}

	public List<MaterialOrderItemView> findMaterials(String stylesId, String agreementCode, String colorCode,
			String supplierCode) {
		Long prdAgreeId = resolvePrdAgreeId(agreementCode, colorCode);
		if (prdAgreeId == null || !hasTable("material_specs")) {
			return Collections.emptyList();
		}

		String prdColumn = findFirstExistingColumn("material_specs", List.of("prd_agree_id"));
		String colorColumn = findFirstExistingColumn("material_specs", List.of("color_code"));
		String supplierColumn = findFirstExistingColumn("material_specs", List.of("supplier_code"));
		String styleIdColumn = findFirstExistingColumn("material_specs", List.of("styles_id", "style_id"));
		String bomIdColumn = findFirstExistingColumn("material_specs", List.of("bom_id"));

		if (prdColumn == null || colorColumn == null || supplierColumn == null) {
			return Collections.emptyList();
		}

		String sql = """
				select %s as bom_id,
				       %s as color_code,
				       %s as category,
				       %s as material_name,
				       %s as material_usage,
				       %s as spec,
				       %s as material_color,
				       %s as uom,
				       %s as qty_per_piece,
				       %s as supplier_code,
				       %s as loss_rate,
				       %s as order_uom,
				       %s as unit_price,
				       %s as remark
				from material_specs
				where %s = :prdAgreeId
				  and %s = :colorCode
				  and %s = :supplierCode
				%s
				order by coalesce(%s, 0) asc
				""".formatted(selectOrNull(bomIdColumn, "bom_id"), selectOrNull(colorColumn, "color_code"),
				selectOrNull("category"), selectOrNull("material_name"), selectOrNull("material_usage"),
				selectOrNull("spec"), selectOrNull("material_color"), selectOrNull("uom"),
				selectOrNull("qty_per_piece"), selectOrNull(supplierColumn, "supplier_code"), selectOrNull("loss_rate"),
				selectOrNull("order_uom"), selectOrNull("unit_price"), selectOrNull("remark"), prdColumn, colorColumn,
				supplierColumn,
				StringUtils.hasText(stylesId) && styleIdColumn != null ? "and " + styleIdColumn + " = :stylesId" : "",
				bomIdColumn != null ? bomIdColumn : prdColumn);

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("prdAgreeId", prdAgreeId)
				.addValue("colorCode", colorCode).addValue("supplierCode", supplierCode);
		if (StringUtils.hasText(stylesId) && styleIdColumn != null) {
			params.addValue("stylesId", stylesId);
		}

		return jdbcTemplate.query(sql, params,
				(rs, rowNum) -> new MaterialOrderItemView(rs.getObject("bom_id") != null ? rs.getLong("bom_id") : null,
						rs.getString("color_code"), rs.getString("category"), rs.getString("material_name"),
						rs.getString("material_usage"), rs.getString("spec"), rs.getString("material_color"),
						rs.getString("uom"), rs.getBigDecimal("qty_per_piece"), rs.getString("supplier_code"),
						rs.getBigDecimal("loss_rate"), rs.getString("order_uom"), rs.getBigDecimal("unit_price"),
						rs.getString("remark")));
	}

	@Transactional
	public Map<String, Object> submitOrders(MaterialOrderRequest request, String orderedBy) {
		if (!hasTable("material_orders")) {
			return Map.of("success", true, "created", 0, "skipped", 0, "message", "발주 요청이 접수되었습니다.");
		}

		Long prdAgreeId = resolvePrdAgreeId(request.getPrdAgreeCode(), request.getColorCode());
		if (prdAgreeId == null) {
			throw new IllegalArgumentException("생산합의코드가 올바르지 않습니다.");
		}

		String orderCodeColumn = findFirstExistingColumn("material_orders", List.of("m_order_code", "order_code"));
		String prdColumn = findFirstExistingColumn("material_orders", List.of("prd_agree_id"));
		String colorColumn = findFirstExistingColumn("material_orders", List.of("color_code"));
		String bomIdColumn = findFirstExistingColumn("material_orders", List.of("bom_id"));
		String supplierColumn = findFirstExistingColumn("material_orders", List.of("supplier_code"));
		String styleIdColumn = findFirstExistingColumn("material_orders", List.of("styles_id", "style_id"));
		String orderAmountColumn = findFirstExistingColumn("material_orders", List.of("order_amount"));
		String orderPriceColumn = findFirstExistingColumn("material_orders", List.of("order_price"));
		String orderedByColumn = findFirstExistingColumn("material_orders", List.of("ordered_by", "created_by"));
		String orderDateColumn = findFirstExistingColumn("material_orders", List.of("order_date"));
		String dueDateColumn = findFirstExistingColumn("material_orders", List.of("due_date"));
		String deliveryColumn = findFirstExistingColumn("material_orders", List.of("delivery_place"));
		String remarkColumn = findFirstExistingColumn("material_orders", List.of("remark"));

		if (orderCodeColumn == null || prdColumn == null || colorColumn == null || bomIdColumn == null
				|| supplierColumn == null || orderAmountColumn == null || orderPriceColumn == null
				|| orderedByColumn == null) {
			return Map.of("success", false, "created", 0, "skipped", 0);
		}

		List<MaterialOrderItemView> specs = findMaterials(request.getStylesId(), request.getPrdAgreeCode(),
				request.getColorCode(), request.getSupplierCode());

		int created = 0;
		int skipped = 0;
		String deliveryPlace = StringUtils.hasText(request.getWarehouseCode())
				? request.getWarehouseCode()
				: request.getDeliveryPlace();

		for (MaterialOrderItemView spec : specs) {
			if (spec.getBomId() == null) {
				skipped++;
				continue;
			}
			if (isOrderExists(request.getStylesId(), prdAgreeId, request.getColorCode(),
					request.getSupplierCode(), spec.getBomId())) {
				skipped++;
				continue;
			}

			MapSqlParameterSource params = new MapSqlParameterSource()
					.addValue("mOrderCode", UUID.randomUUID().toString()).addValue("stylesId", request.getStylesId())
					.addValue("styleCode", request.getStyleCode()).addValue("prdAgreeId", prdAgreeId)
					.addValue("colorCode", request.getColorCode()).addValue("bomId", spec.getBomId())
					.addValue("supplierCode", request.getSupplierCode()).addValue("orderAmount", BigDecimal.ZERO)
					.addValue("orderPrice", BigDecimal.ZERO).addValue("orderedBy", orderedBy)
					.addValue("orderDate", toSqlDate(request.getOrderDate()))
					.addValue("dueDate", toSqlDate(request.getDueDate()))
					.addValue("deliveryPlace", deliveryPlace)
					.addValue("remark", request.getRemark());

			List<String> columns = new ArrayList<>();
			List<String> values = new ArrayList<>();
			columns.add(orderCodeColumn);
			values.add(":mOrderCode");
			if (styleIdColumn != null) {
				columns.add(styleIdColumn);
				values.add(":stylesId");
			}
			columns.add(prdColumn);
			values.add(":prdAgreeId");
			columns.add(colorColumn);
			values.add(":colorCode");
			columns.add(bomIdColumn);
			values.add(":bomId");
			columns.add(supplierColumn);
			values.add(":supplierCode");
			columns.add(orderAmountColumn);
			values.add(":orderAmount");
			columns.add(orderPriceColumn);
			values.add(":orderPrice");
			columns.add(orderedByColumn);
			values.add(":orderedBy");
			if (orderDateColumn != null) {
				columns.add(orderDateColumn);
				values.add(":orderDate");
			}
			if (dueDateColumn != null) {
				columns.add(dueDateColumn);
				values.add(":dueDate");
			}
			if (deliveryColumn != null) {
				columns.add(deliveryColumn);
				values.add(":deliveryPlace");
			}
			if (remarkColumn != null) {
				columns.add(remarkColumn);
				values.add(":remark");
			}

			String sql = "insert into material_orders (" + String.join(", ", columns) + ") values ("
					+ String.join(", ", values) + ")";

			jdbcTemplate.update(sql.toString(), params);
			created++;
		}

		return Map.of("success", created > 0 || skipped == specs.size(), "created", created, "skipped", skipped);
	}

	public List<MaterialOrderSupplierView> findSuppliersByStyleCode(String styleCode, String agreementCode,
			String colorCode) {
		Long prdAgreeId = resolvePrdAgreeId(agreementCode, colorCode);
		if (!StringUtils.hasText(styleCode) || prdAgreeId == null || !StringUtils.hasText(colorCode)
				|| !hasTable("material_specs")) {
			return Collections.emptyList();
		}
		return findSuppliersByStyleCode(styleCode, prdAgreeId, colorCode);
	}

	public MaterialOrderSearchResult searchOrders(String styleCode, String agreementCode, String colorCode) {
		Long prdAgreeId = resolvePrdAgreeId(agreementCode, colorCode);
		log.info("Material order search agreementCode={}, colorCode={}, resolvedPrdAgreeId={}",
				agreementCode, colorCode, prdAgreeId);
		if (prdAgreeId == null) {
			log.info("Material order search result suppliers=0 materials=0");
			return new MaterialOrderSearchResult(Collections.emptyList(), Collections.emptyList());
		}
		BigDecimal productionQty = findProductionQty(agreementCode, colorCode);
		List<MaterialOrderSupplierView> suppliers = findSuppliersByStyleCode(styleCode, prdAgreeId, colorCode);
		List<MaterialOrderItemView> materials = findMaterialsByStyleCode(styleCode, prdAgreeId, colorCode);
		List<MaterialOrderLineRow> materialsToOrder = buildMaterialsToOrder(materials, colorCode, productionQty);
		log.info("Material order search productionQty sum={}, material_specs rows={}",
				productionQty, materials.size());
		logMaterialOrderSample(materialsToOrder);
		log.info("Material order search result suppliers={} materials={}", suppliers.size(), materialsToOrder.size());
		return new MaterialOrderSearchResult(suppliers, materialsToOrder);
	}

	private List<MaterialOrderSupplierView> findSuppliersByStyleCode(String styleCode, Long prdAgreeId,
			String colorCode) {
		String supplierColumn = findFirstExistingColumn("material_specs", List.of("supplier_code"));
		String prdColumn = findFirstExistingColumn("material_specs", List.of("prd_agree_id"));
		String colorColumn = findFirstExistingColumn("material_specs", List.of("color_code"));
		String styleCodeColumn = findFirstExistingColumn("material_specs", List.of("style_code"));
		String styleIdColumn = findFirstExistingColumn("material_specs", List.of("styles_id", "style_id"));
		if (supplierColumn == null || prdColumn == null || colorColumn == null) {
			return Collections.emptyList();
		}
		Long stylesId = null;
		if (styleCodeColumn == null && styleIdColumn != null) {
			stylesId = resolveStylesIdByCode(styleCode);
		}
		StringBuilder sql = new StringBuilder().append("select distinct ").append(supplierColumn)
				.append(" as supplier_code ").append("from material_specs ").append("where ")
				.append(prdColumn).append(" = :prdAgreeId ").append("and ").append(colorColumn)
				.append(" = :colorCode ").append("and ").append(supplierColumn).append(" is not null ")
				.append("and ").append(supplierColumn).append(" <> '' ");
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("prdAgreeId", prdAgreeId)
				.addValue("colorCode", colorCode);
		if (styleCodeColumn != null) {
			sql.append("and ").append(styleCodeColumn).append(" = :styleCode ");
			params.addValue("styleCode", styleCode);
		} else if (stylesId != null && styleIdColumn != null) {
			sql.append("and ").append(styleIdColumn).append(" = :stylesId ");
			params.addValue("stylesId", stylesId);
		}

		List<String> supplierCodes = jdbcTemplate.query(sql.toString(), params,
				(rs, rowNum) -> rs.getString("supplier_code"));
		if (supplierCodes.isEmpty()) {
			return Collections.emptyList();
		}

		Map<String, String> supplierNames = findSupplierNames(new LinkedHashSet<>(supplierCodes));
		List<MaterialOrderSupplierView> result = new ArrayList<>();
		for (String code : supplierCodes) {
			result.add(new MaterialOrderSupplierView(code, supplierNames.getOrDefault(code, "-"), false));
		}
		return result;
	}

	public List<MaterialOrderItemView> findMaterialsByStyleCode(String styleCode, String prdAgreeCode, String colorCode) {
		Long prdAgreeId = resolvePrdAgreeId(prdAgreeCode, colorCode);
		if (!StringUtils.hasText(styleCode) || prdAgreeId == null || !StringUtils.hasText(colorCode)
				|| !hasTable("material_specs")) {
			return Collections.emptyList();
		}
		return findMaterialsByStyleCode(styleCode, prdAgreeId, colorCode);
	}

	private List<MaterialOrderItemView> findMaterialsByStyleCode(String styleCode, Long prdAgreeId, String colorCode) {
		String prdColumn = findFirstExistingColumn("material_specs", List.of("prd_agree_id"));
		String colorColumn = findFirstExistingColumn("material_specs", List.of("color_code"));
		String supplierColumn = findFirstExistingColumn("material_specs", List.of("supplier_code"));
		String styleCodeColumn = findFirstExistingColumn("material_specs", List.of("style_code"));
		String styleIdColumn = findFirstExistingColumn("material_specs", List.of("styles_id", "style_id"));
		String bomIdColumn = findFirstExistingColumn("material_specs", List.of("bom_id"));
		if (prdColumn == null || colorColumn == null) {
			return Collections.emptyList();
		}
		Long stylesId = null;
		if (styleCodeColumn == null && styleIdColumn != null) {
			stylesId = resolveStylesIdByCode(styleCode);
		}
		String sql = """
				select %s as bom_id,
				       %s as color_code,
				       %s as category,
				       %s as material_name,
				       %s as material_usage,
				       %s as spec,
				       %s as material_color,
				       %s as uom,
				       %s as qty_per_piece,
				       %s as supplier_code,
				       %s as loss_rate,
				       %s as order_uom,
				       %s as unit_price,
				       %s as remark
				from material_specs
				where %s = :prdAgreeId
				  and %s = :colorCode
				  %s
				order by coalesce(%s, 0) asc
				""".formatted(selectOrNull(bomIdColumn, "bom_id"), selectOrNull(colorColumn, "color_code"),
				selectOrNull("category"), selectOrNull("material_name"), selectOrNull("material_usage"),
				selectOrNull("spec"), selectOrNull("material_color"), selectOrNull("uom"),
				selectOrNull("qty_per_piece"), selectOrNull(supplierColumn, "supplier_code"), selectOrNull("loss_rate"),
				selectOrNull("order_uom"), selectOrNull("unit_price"), selectOrNull("remark"), prdColumn, colorColumn,
				buildStyleFilter(styleCodeColumn, styleIdColumn, stylesId),
				bomIdColumn != null ? bomIdColumn : prdColumn);

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("prdAgreeId", prdAgreeId)
				.addValue("colorCode", colorCode);
		if (styleCodeColumn != null) {
			params.addValue("styleCode", styleCode);
		} else if (stylesId != null && styleIdColumn != null) {
			params.addValue("stylesId", stylesId);
		}

		return jdbcTemplate.query(sql, params,
				(rs, rowNum) -> new MaterialOrderItemView(rs.getObject("bom_id") != null ? rs.getLong("bom_id") : null,
						rs.getString("color_code"), rs.getString("category"), rs.getString("material_name"),
						rs.getString("material_usage"), rs.getString("spec"), rs.getString("material_color"),
						rs.getString("uom"), rs.getBigDecimal("qty_per_piece"), rs.getString("supplier_code"),
						rs.getBigDecimal("loss_rate"), rs.getString("order_uom"), rs.getBigDecimal("unit_price"),
						rs.getString("remark")));
	}

	private boolean isSupplierAlreadyOrdered(String stylesId, Long prdAgreeId, String colorCode,
			String supplierCode) {
		if (!hasTable("material_orders")) {
			return false;
		}
		String prdColumn = findFirstExistingColumn("material_orders", List.of("prd_agree_id"));
		String colorColumn = findFirstExistingColumn("material_orders", List.of("color_code"));
		String supplierColumn = findFirstExistingColumn("material_orders", List.of("supplier_code"));
		String styleIdColumn = findFirstExistingColumn("material_orders", List.of("styles_id", "style_id"));

		if (prdColumn == null || colorColumn == null || supplierColumn == null) {
			return false;
		}

		StringBuilder sql = new StringBuilder().append("select count(*) from material_orders ").append("where ")
				.append(prdColumn).append(" = :prdAgreeId ").append("and ").append(colorColumn)
				.append(" = :colorCode ").append("and ").append(supplierColumn).append(" = :supplierCode ");

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("prdAgreeId", prdAgreeId)
				.addValue("colorCode", colorCode).addValue("supplierCode", supplierCode);

		if (StringUtils.hasText(stylesId) && styleIdColumn != null) {
			sql.append("and ").append(styleIdColumn).append(" = :stylesId ");
			params.addValue("stylesId", stylesId);
		}

		Integer count = jdbcTemplate.queryForObject(sql.toString(), params, Integer.class);
		return count != null && count > 0;
	}

	private boolean isOrderExists(String stylesId, Long prdAgreeId, String colorCode, String supplierCode,
			Long bomId) {
		if (!hasTable("material_orders")) {
			return false;
		}

		String prdColumn = findFirstExistingColumn("material_orders", List.of("prd_agree_id"));
		String colorColumn = findFirstExistingColumn("material_orders", List.of("color_code"));
		String supplierColumn = findFirstExistingColumn("material_orders", List.of("supplier_code"));
		String styleIdColumn = findFirstExistingColumn("material_orders", List.of("styles_id", "style_id"));
		String bomIdColumn = findFirstExistingColumn("material_orders", List.of("bom_id"));

		if (prdColumn == null || colorColumn == null || supplierColumn == null || bomIdColumn == null) {
			return false;
		}

		StringBuilder sql = new StringBuilder().append("select count(*) from material_orders ").append("where ")
				.append(prdColumn).append(" = :prdAgreeId ").append("and ").append(colorColumn)
				.append(" = :colorCode ").append("and ").append(supplierColumn).append(" = :supplierCode ")
				.append("and ").append(bomIdColumn).append(" = :bomId ");

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("prdAgreeId", prdAgreeId)
				.addValue("colorCode", colorCode).addValue("supplierCode", supplierCode).addValue("bomId", bomId);

		if (StringUtils.hasText(stylesId) && styleIdColumn != null) {
			sql.append("and ").append(styleIdColumn).append(" = :stylesId ");
			params.addValue("stylesId", stylesId);
		}

		Integer count = jdbcTemplate.queryForObject(sql.toString(), params, Integer.class);
		return count != null && count > 0;
	}

	private List<String> findAgreements(String stylesId, String styleCode) {
		if (!hasTable("production_agreements")) {
			return Collections.emptyList();
		}
		String prdColumn = findFirstExistingColumn("production_agreements",
				List.of("prd_agree_code", "agreement_code"));
		String styleIdColumn = findFirstExistingColumn("production_agreements", List.of("styles_id", "style_id"));
		String styleCodeColumn = findFirstExistingColumn("production_agreements", List.of("style_code"));

		if (prdColumn == null || (styleIdColumn == null && styleCodeColumn == null)) {
			return Collections.emptyList();
		}

		boolean hasStyleId = StringUtils.hasText(stylesId) && styleIdColumn != null;
		boolean hasStyleCode = StringUtils.hasText(styleCode) && styleCodeColumn != null;
		if (!hasStyleId && !hasStyleCode) {
			return Collections.emptyList();
		}
		
		String referenceColumn = hasStyleId ? styleIdColumn : styleCodeColumn;
		String referenceValue = hasStyleId ? stylesId : styleCode;

		StringBuilder sql = new StringBuilder().append("select distinct ").append(prdColumn)
				.append(" as prd_agree_code ").append("from production_agreements where ").append(referenceColumn)
				.append(" = :styleRef ").append("order by ").append(prdColumn).append(" asc");

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("styleRef", referenceValue);

		return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> rs.getString("prd_agree_code"));
	}

	private List<String> findColorsFromSpecs(String stylesId, String styleCode) {
		if (!hasTable("material_specs")) {
			return Collections.emptyList();
		}

		String colorColumn = findFirstExistingColumn("material_specs",
				List.of("color_code", "material_color", "color"));
		String styleIdColumn = findFirstExistingColumn("material_specs", List.of("styles_id", "style_id"));
		String styleCodeColumn = findFirstExistingColumn("material_specs", List.of("style_code"));
		if (colorColumn == null) {
			return Collections.emptyList();
		}

		StringBuilder sql = new StringBuilder().append("select distinct ").append(colorColumn).append(" as color_code ")
				.append("from material_specs ");

		MapSqlParameterSource params = new MapSqlParameterSource();
		if (StringUtils.hasText(stylesId) && styleIdColumn != null) {
			sql.append("where ").append(styleIdColumn).append(" = :stylesId ");
			params.addValue("stylesId", stylesId);
		} else if (StringUtils.hasText(styleCode) && styleCodeColumn != null) {
			sql.append("where ").append(styleCodeColumn).append(" = :styleCode ");
			params.addValue("styleCode", styleCode);
		}
		sql.append("order by ").append(colorColumn).append(" asc");

		return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> rs.getString("color_code"));
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

	private String resolveStyleIdColumn() {
		return findFirstExistingColumn("styles", List.of("styles_id", "style_id", "id"));
	}

	private String resolveStyleCodeColumn() {
		return findFirstExistingColumn("styles", List.of("style_code", "styles_code"));
	}

	private String resolveStyleCode(String stylesId) {
		if (!StringUtils.hasText(stylesId) || !hasTable("styles")) {
			return null;
		}
		String idColumn = resolveStyleIdColumn();
		String codeColumn = resolveStyleCodeColumn();
		if (idColumn == null || codeColumn == null) {
			return null;
		}

		String sql = "select " + codeColumn + " as style_code from styles where " + idColumn + " = :id limit 1";
		MapSqlParameterSource params = new MapSqlParameterSource("id", stylesId);
		List<String> codes = jdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getString("style_code"));
		return codes.isEmpty() ? null : codes.get(0);
	}

	private Long resolveStylesIdByCode(String styleCode) {
		if (!StringUtils.hasText(styleCode) || !hasTable("styles")) {
			return null;
		}
		String idColumn = resolveStyleIdColumn();
		String codeColumn = resolveStyleCodeColumn();
		if (idColumn == null || codeColumn == null) {
			return null;
		}
		String sql = "select " + idColumn + " as styles_id from styles where " + codeColumn
				+ " = :styleCode limit 1";
		MapSqlParameterSource params = new MapSqlParameterSource("styleCode", styleCode);
		List<Long> ids = jdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getLong("styles_id"));
		return ids.isEmpty() ? null : ids.get(0);
	}

	private String buildStyleFilter(String styleCodeColumn, String styleIdColumn, Long stylesId) {
		if (styleCodeColumn != null) {
			return "and " + styleCodeColumn + " = :styleCode";
		}
		if (stylesId != null && styleIdColumn != null) {
			return "and " + styleIdColumn + " = :stylesId";
		}
		return "";
	}

	private String findFirstExistingColumn(String table, List<String> candidates) {
		for (String candidate : candidates) {
			if (hasColumn(table, candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private String selectOrNull(String column) {
		return selectOrNull(column, null);
	}

	private String selectOrNull(String column, String alias) {
		if (column == null) {
			return "null";
		}
		return column;
	}

	private Long resolvePrdAgreeId(String agreementCode, String colorCode) {
		if (!StringUtils.hasText(agreementCode) || !StringUtils.hasText(colorCode)
				|| !hasTable("production_agreements")) {
			return null;
		}
		return productionAgreementRepository
				.findTopByAgreementCodeAndColorCodeOrderByPrdAgreeIdDesc(agreementCode.trim(), colorCode.trim())	
				.map(ProductionAgreement::getPrdAgreeId)
				.orElseThrow(() -> new IllegalArgumentException(
						"생산합의 정보를 찾을 수 없습니다. agreementCode=%s, colorCode=%s"
								.formatted(agreementCode, colorCode)));
	}

	public static class MaterialOrderSearchResult {
		private final List<MaterialOrderSupplierView> suppliers;
		private final List<MaterialOrderLineRow> materialsToOrder;

		public MaterialOrderSearchResult(List<MaterialOrderSupplierView> suppliers,
				List<MaterialOrderLineRow> materialsToOrder) {
			this.suppliers = suppliers;
			this.materialsToOrder = materialsToOrder;
		}

		public List<MaterialOrderSupplierView> getSuppliers() {
			return suppliers;
		}

		public List<MaterialOrderLineRow> getMaterialsToOrder() {
			return materialsToOrder;
		}
	}

	private BigDecimal findProductionQty(String agreementCode, String colorCode) {
		if (!StringUtils.hasText(agreementCode) || !StringUtils.hasText(colorCode)
				|| !hasTable("production_agreements")) {
			return BigDecimal.ZERO;
		}
		String agreementColumn = findFirstExistingColumn("production_agreements",
				List.of("agreement_code", "prd_agree_code"));
		String colorColumn = findFirstExistingColumn("production_agreements", List.of("color_code"));
		String quantityColumn = findFirstExistingColumn("production_agreements", List.of("quantity"));
		if (agreementColumn == null || colorColumn == null || quantityColumn == null) {
			return BigDecimal.ZERO;
		}
		String sql = """
				select coalesce(sum(%s), 0) as production_qty
				from production_agreements
				where %s = :agreementCode
				  and %s = :colorCode
				""".formatted(quantityColumn, agreementColumn, colorColumn);
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("agreementCode", agreementCode)
				.addValue("colorCode", colorCode);
		BigDecimal result = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
		return result != null ? result : BigDecimal.ZERO;
	}

	private List<MaterialOrderLineRow> buildMaterialsToOrder(List<MaterialOrderItemView> materials,
			String colorCode,
			BigDecimal productionQty) {
		if (materials == null || materials.isEmpty()) {
			return Collections.emptyList();
		}
		Set<String> supplierCodes = new LinkedHashSet<>();
		for (MaterialOrderItemView item : materials) {
			if (StringUtils.hasText(item.getSupplierCode())) {
				supplierCodes.add(item.getSupplierCode());
			}
		}
		Map<String, String> supplierNames = findSupplierNames(supplierCodes);
		List<MaterialOrderLineRow> rows = new ArrayList<>();
		for (MaterialOrderItemView item : materials) {
			BigDecimal qtyPerPiece = safeDecimal(item.getQtyPerPiece());
			BigDecimal lossRate = safeDecimal(item.getLossRate());
			BigDecimal normalizedLossRate = normalizeLossRate(lossRate);
			BigDecimal unitPrice = safeDecimal(item.getUnitPrice());
			BigDecimal orderAmount = productionQty.multiply(qtyPerPiece)
					.multiply(BigDecimal.ONE.add(normalizedLossRate))
					.setScale(3, RoundingMode.HALF_UP);
			BigDecimal orderPrice = orderAmount.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
			String supplierCode = item.getSupplierCode();
			rows.add(new MaterialOrderLineRow(
					colorCode,
					item.getCategory(),
					item.getMaterialName(),
					item.getMaterialUsage(),
					item.getSpec(),
					item.getMaterialColor(),
					item.getUom(),
					qtyPerPiece,
					supplierCode,
					supplierNames.getOrDefault(supplierCode, "-"),
					lossRate,
					item.getOrderUom(),
					unitPrice,
					item.getRemark(),
					productionQty,
					orderAmount,
					orderPrice));
		}
		return rows;
	}

	private BigDecimal safeDecimal(BigDecimal value) {
		return value != null ? value : BigDecimal.ZERO;
	}

	private BigDecimal normalizeLossRate(BigDecimal lossRate) {
		if (lossRate == null) {
			return BigDecimal.ZERO;
		}
		return lossRate.compareTo(BigDecimal.ONE) > 0
				? lossRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
				: lossRate;
	}
	
	private void logMaterialOrderSample(List<MaterialOrderLineRow> rows) {
		if (rows == null || rows.isEmpty()) {
			log.info("Material order search sample row not available");
			return;
		}
		MaterialOrderLineRow sample = rows.get(0);
		log.info("Material order sample qtyPerPiece={} lossRate={} unitPrice={} orderAmount={} orderPrice={}",
				sample.getQtyPerPiece(),
				sample.getLossRate(),
				sample.getUnitPrice(),
				sample.getOrderAmount(),
				sample.getOrderPrice());
	}

	private Date toSqlDate(String value) {
		LocalDate date = null;
		try {
			if (StringUtils.hasText(value)) {
				date = LocalDate.parse(value);
			}
		} catch (Exception ignored) {
			date = null;
		}
		return date != null ? Date.valueOf(date) : null;
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
}
