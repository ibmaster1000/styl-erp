package com.example.erp.service;

import com.example.erp.controller.dto.MaterialTransactionLineView;
import com.example.erp.controller.dto.MaterialTransactionSaveLine;
import com.example.erp.controller.dto.MaterialTransactionSaveRequest;
import com.example.erp.controller.dto.SimpleCodeView;
import com.example.erp.domain.ProductionAgreement;
import com.example.erp.repository.ProductionAgreementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MaterialTransactionService {

	private static final Logger log = LoggerFactory.getLogger(MaterialTransactionService.class);
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
		if (request == null || CollectionUtils.isEmpty(request.getItems())) {
			return Map.of("success", false, "created", 0, "skipped", 0, "message", "입고 항목이 없습니다.");
		}

		final String producerType = "CUSTOMER";
		String requestProducerCode = null;
		try {
			org.springframework.beans.BeanWrapper wrapper = new org.springframework.beans.BeanWrapperImpl(request);
			if (wrapper.isReadableProperty("producerCode")) {
				Object value = wrapper.getPropertyValue("producerCode");
				if (value != null) {
					requestProducerCode = value.toString();
				}
			}
		} catch (Exception e) {
			log.debug("요청에서 producer 정보를 확인하지 못했습니다.", e);
		}
		String producerCodeLookupSql = """
				SELECT COALESCE(mo.vendor_code, ms.supplier_code) AS producer_code
				FROM material_orders mo
				JOIN material_specs ms ON mo.bom_id = ms.bom_id
				WHERE mo.m_order_id = :mOrderId
				""";

		String sql = """
				INSERT INTO material_inbounds (
				    inbound_datetime,
				    m_order_id,
				    styles_id,
				    prd_agree_id,
				    color_type,
				    color_code,
				    material_spec_id,
				    producer_type,
				    producer_code,
				    warehouse_type,
				    warehouse_code,
				    planned_qty,
				    received_qty,
				    order_uom,
				    unit_price,
				    created_by,
				    remark
				)
				SELECT
				    NOW(),
				    mo.m_order_id,
				    mo.styles_id,
				    mo.prd_agree_id,
				    mo.color_type,
				    mo.color_code,
				    mo.bom_id,
				    :producerType,
				    :producerCode,
				    mo.warehouse_type,
				    mo.warehouse_code,
				    mo.order_amount,
				    :receivedQty,
				    :orderUom,
				    mo.unit_price,
				    :createdBy,
				    :remark
				FROM material_orders mo
				WHERE mo.m_order_id = :mOrderId
				""";

		int created = 0;
		int skipped = 0;
		for (MaterialTransactionSaveLine line : request.getItems()) {
			if (line == null || line.getMOrderId() == null) {
				skipped++;
				continue;
			}

			BigDecimal receivedQty = safeDecimal(line.getQuantity());
			if (receivedQty.compareTo(BigDecimal.ZERO) <= 0) {
				skipped++;
				continue;
			}

			String orderUom = StringUtils.hasText(line.getOrderUom()) ? line.getOrderUom() : "EA";
			String producerCode = StringUtils.hasText(requestProducerCode) ? requestProducerCode : null;
			if (!StringUtils.hasText(producerCode)) {
				MapSqlParameterSource lookupParams = new MapSqlParameterSource().addValue("mOrderId", line.getMOrderId());
				producerCode = jdbcTemplate.query(producerCodeLookupSql, lookupParams,
						rs -> rs.next() ? rs.getString("producer_code") : null);
			}
			if (!StringUtils.hasText(producerCode)) {
				throw new IllegalArgumentException("producerCode 값을 찾을 수 없습니다. mOrderId=" + line.getMOrderId());
			}
			MapSqlParameterSource params = new MapSqlParameterSource().addValue("mOrderId", line.getMOrderId())
					.addValue("producerType", producerType).addValue("producerCode", producerCode)
					.addValue("receivedQty", receivedQty).addValue("orderUom", orderUom).addValue("createdBy", empNo)
					.addValue("remark", line.getRemark());

			try {
				int affected = jdbcTemplate.update(sql, params);
				if (affected > 0) {
					created++;
				} else {
					skipped++;
				}
			} catch (org.springframework.dao.DataIntegrityViolationException e) {
				throw new RuntimeException("입고 등록에 실패했습니다. (원인: " + e.getMostSpecificCause().getMessage() + ")", e);
			} catch (Exception e) {
				throw new RuntimeException("입고 등록에 실패했습니다. (원인: " + e.getMessage() + ")", e);
			}
		}

		return Map.of("success", created > 0, "created", created, "skipped", skipped, "message",
				created > 0 ? "입고가 등록되었습니다." : "입고 등록에 실패했습니다.");
	}

	@Transactional
	public Map<String, Object> saveOutbound(MaterialTransactionSaveRequest request, String empNo) {
		try {
			if (!hasTable("material_outbounds")) {
				return Map.of("success", false, "created", 0, "skipped", 0, "message", "출고 테이블이 없습니다.");
			}
			if (CollectionUtils.isEmpty(request.getItems())) {
				return Map.of("success", false, "created", 0, "skipped", 0, "message", "출고 항목이 없습니다.");
			}
			List<String> missingColumns = findMissingColumns("material_outbounds",
					List.of("m_order_id", "issued_out_qty", "receiver_code", "receiver_type"));
			if (!missingColumns.isEmpty()) {
				return Map.of("success", false, "created", 0, "skipped", 0, "message",
						"출고 컬럼이 부족합니다. missingColumns=" + missingColumns);
			}

			boolean hasPlannedOutQty = hasColumn("material_outbounds", "planned_out_qty");
			boolean hasOutboundDatetime = hasColumn("material_outbounds", "outbound_datetime");
			boolean hasCreatedBy = hasColumn("material_outbounds", "created_by");
			boolean hasRemark = hasColumn("material_outbounds", "remark");

			Long prdAgreeId = resolvePrdAgreeId(request.getPrdAgreeCode(), request.getColorCode());
			int created = 0;
			int skipped = 0;
			for (MaterialTransactionSaveLine line : request.getItems()) {
				if (line.getMOrderId() == null) {
					skipped++;
					continue;
				}
				BigDecimal issued = safeDecimal(line.getIssuedOutQuantity());
				if (issued.compareTo(BigDecimal.ZERO) <= 0) {
					skipped++;
					continue;
				}
				if (!StringUtils.hasText(line.getFactoryCode())) {
					skipped++;
					continue;
				}
				if (issued.compareTo(BigDecimal.ZERO) > 0) {
					BigDecimal available = sumInboundForOrder(line.getMOrderId())
							.subtract(sumIssuedForOrder(line.getMOrderId()));
					if (available.compareTo(issued) < 0) {
						skipped++;
						continue;
					}
				}

				MapSqlParameterSource params = new MapSqlParameterSource().addValue("mOrderId", line.getMOrderId())
						.addValue("plannedOutQty", BigDecimal.ZERO).addValue("issuedOutQty", issued)
						.addValue("receiverType", "CUSTOMER").addValue("receiverCode", line.getFactoryCode())
						.addValue("outboundDatetime", Timestamp.valueOf(LocalDateTime.now()))
						.addValue("createdBy", empNo).addValue("remark", line.getRemark());

				List<String> columns = new ArrayList<>();
				List<String> values = new ArrayList<>();
				columns.add("m_order_id");
				values.add(":mOrderId");
				if (hasPlannedOutQty) {
					columns.add("planned_out_qty");
					values.add(":plannedOutQty");
				}
				columns.add("issued_out_qty");
				values.add(":issuedOutQty");
				columns.add("receiver_type");
				values.add(":receiverType");
				columns.add("receiver_code");
				values.add(":receiverCode");
				if (hasOutboundDatetime) {
					columns.add("outbound_datetime");
					values.add(":outboundDatetime");
				}
				if (hasCreatedBy) {
					columns.add("created_by");
					values.add(":createdBy");
				}
				if (hasRemark) {
					columns.add("remark");
					values.add(":remark");
				}

				String sql = "insert into material_outbounds (" + String.join(", ", columns) + ") values ("
						+ String.join(", ", values) + ")";
				log.info("Executing outbound SQL: {} params: mOrderId={}, prdAgreeId={}, colorCode={}, stylesId={}",
						sql, line.getMOrderId(), prdAgreeId, request.getColorCode(), request.getStylesId());
				int affected = jdbcTemplate.update(sql, params);
				if (affected > 0) {
					created++;
				} else {
					skipped++;
				}
			}

			return Map.of("success", created > 0, "created", created, "skipped", skipped, "message",
					created > 0 ? "출고가 등록되었습니다." : "출고 등록에 실패했습니다.");
		} catch (Exception e) {
			String errorId = UUID.randomUUID().toString();
			log.error("출고 저장 중 오류가 발생했습니다. errorId={}", errorId, e);
			return Map.of("success", false, "created", 0, "skipped", 0, "message",
					"출고 등록 중 오류가 발생했습니다. errorId=" + errorId);
		}
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

		String agreementCodeCol = findFirstExistingColumn("production_agreements",
				List.of("agreement_code", "prd_agree_code", "prd_agree_no"));
		String agreementColorCol = findFirstExistingColumn("production_agreements", List.of("color_code", "color"));
		String agreementQtyCol = findFirstExistingColumn("production_agreements", List.of("quantity", "qty"));
		String lossRateCol = findFirstExistingColumn("material_specs", List.of("loss_rate", "loss"));
		String lossExpr = lossRateCol != null ? "coalesce(ms." + lossRateCol + ", 0)" : "0";

		String requiredJoin = "";
		String requiredSelect = "0 as required_qty, ";
		String requiredExpr = null;
		if (agreementCodeCol != null && agreementColorCol != null && agreementQtyCol != null) {
			requiredJoin = """
					left join (
						select coalesce(sum(%s), 0) as agreement_qty
						from production_agreements
						where %s = :prdAgreeCode
						  and %s = :colorCode
					) pa on 1=1
					""".formatted(agreementQtyCol, agreementCodeCol, agreementColorCol);
			requiredExpr = "coalesce(pa.agreement_qty, 0) * coalesce(ms.qty_per_piece, 0) " + "* (1 + (" + lossExpr
					+ " / 100.0))";
			requiredSelect = "round(" + requiredExpr + ", 3) as required_qty, ";
		}

		String inboundJoin = "";
		String inboundSelect = "0 as inbound_qty, ";
		if (includeOutbound) {
			if (hasTable("material_inbounds")) {
				inboundJoin = """
						left join (
							select m_order_id, round(coalesce(sum(received_qty), 0), 3) as inbound_qty
							from material_inbounds
							group by m_order_id
						) mi on mi.m_order_id = mo.m_order_id
						""";
				inboundSelect = "coalesce(mi.inbound_qty, 0) as inbound_qty, ";
			}
		} else if (requiredExpr != null) {
			inboundSelect = "round(" + requiredExpr + ", 3) as inbound_qty, ";
		}

		String outboundJoin = "";
		String outboundSelect = "0 as planned_out_qty, 0 as issued_out_qty, ";
		if (includeOutbound) {
			if (hasTable("material_outbounds")) {
				outboundJoin = """
						left join (
							select m_order_id,
							       round(coalesce(sum(issued_out_qty), 0), 3) as issued_qty
							from material_outbounds
							group by m_order_id
						) mo2 on mo2.m_order_id = mo.m_order_id
						""";
				outboundSelect = "0 as planned_out_qty, " + "coalesce(mo2.issued_qty, 0) as issued_out_qty, ";
			}
		}

		StringBuilder sql = new StringBuilder().append("select mo.m_order_id as m_order_id, ")
				.append("mo.bom_id as bom_id, ").append("mo.styles_id as styles_id, ")
				.append("s.style_code as style_code, ").append("coalesce(s.product_emp_no, '') as production_emp_no, ")
				.append("ms.category as category, ").append("ms.material_name as material_name, ")
				.append("ms.material_usage as material_usage, ").append("ms.spec as spec, ")
				.append("ms.material_color as material_color, ").append("ms.uom as uom, ")
				.append("ms.qty_per_piece as qty_per_piece, ")
				.append("coalesce(mo.vendor_code, ms.supplier_code) as supplier_code, ")
				.append("ms.order_uom as order_uom, ").append("mo.unit_price as unit_price, ")
				.append("mo.order_amount as order_amount, ").append(requiredSelect).append(inboundSelect)
				.append(includeOutbound ? outboundSelect : "0 as planned_out_qty, 0 as issued_out_qty, ")
				.append("ms.remark as remark ").append("from material_orders mo ")
				.append("join material_specs ms on mo.bom_id = ms.bom_id ")
				.append("left join styles s on s.styles_id = mo.styles_id ").append(requiredJoin).append(inboundJoin)
				.append(outboundJoin).append("where mo.prd_agree_id = :prdAgreeId ")
				.append("and mo.color_code = :colorCode ");

		String resolvedStyleId = resolveStyleId(stylesId, styleCode);
		if (StringUtils.hasText(resolvedStyleId)) {
			sql.append("and mo.styles_id = :stylesId ");
		} else if (StringUtils.hasText(styleCode)) {
			sql.append("and s.style_code = :styleCode ");
		}

		sql.append("order by mo.m_order_id asc");

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("prdAgreeId", prdAgreeId)
				.addValue("colorCode", colorCode).addValue("stylesId", resolvedStyleId)
				.addValue("prdAgreeCode", prdAgreeCode).addValue("styleCode", styleCode);
		log.info("Executing material list SQL: {} params: prdAgreeId={}, colorCode={}, stylesId={}", sql, prdAgreeId,
				colorCode, resolvedStyleId);

		List<MaterialTransactionLineView> rows = new ArrayList<>();
		List<String> supplierCodes = new ArrayList<>();
		jdbcTemplate.query(sql.toString(), params, rs -> {
			String supplierCode = rs.getString("supplier_code");
			if (StringUtils.hasText(supplierCode)) {
				supplierCodes.add(supplierCode);
			}
			MaterialTransactionLineView view = new MaterialTransactionLineView()
					.setMOrderId(rs.getObject("m_order_id") != null ? rs.getLong("m_order_id") : null)
					.setBomId(rs.getObject("bom_id") != null ? rs.getLong("bom_id") : null)
					.setStylesId(rs.getString("styles_id"))
					.setStyleCode(resolveStyleCodeValue(rs.getString("style_code"), styleCode))
					.setPrdAgreeCode(prdAgreeCode).setAgreementMonth(prdAgreeCode).setColorCode(colorCode)
					.setCategory(rs.getString("category")).setMaterialName(rs.getString("material_name"))
					.setMaterialUsage(rs.getString("material_usage")).setSpec(rs.getString("spec"))
					.setMaterialColor(rs.getString("material_color")).setUom(rs.getString("uom"))
					.setQtyPerPiece(rs.getBigDecimal("qty_per_piece")).setSupplierCode(supplierCode)
					.setSupplierName(null).setProductionManager(rs.getString("production_emp_no"))
					.setOrderUom(rs.getString("order_uom")).setUnitPrice(rs.getBigDecimal("unit_price"))
					.setOrderQuantity(rs.getBigDecimal("order_amount"))
					.setRequiredQuantity(rs.getBigDecimal("required_qty"))
					.setInboundQuantity(rs.getBigDecimal("inbound_qty"))
					.setPlannedOutboundQuantity(rs.getBigDecimal("planned_out_qty"))
					.setOutboundQuantity(rs.getBigDecimal("issued_out_qty")).setRemark(rs.getString("remark"));
			rows.add(view);
		});

		Map<String, String> supplierNames = findSupplierNames(new LinkedHashSet<>(supplierCodes));
		for (MaterialTransactionLineView view : rows) {
			view.setSupplierName(supplierNames.getOrDefault(view.getSupplierCode(), null));
		}
		return rows;
	}

	private List<String> loadColumnNames(String tableName) {
		try {
			return jdbcTemplate.query("""
					select column_name
					from information_schema.columns
					where upper(table_name) = upper(:tableName)
					order by ordinal_position
					""", new MapSqlParameterSource("tableName", tableName),
					(rs, rowNum) -> rs.getString("column_name"));
		} catch (Exception e) {
			log.warn("{} 컬럼 목록 조회에 실패했습니다.", tableName, e);
			return Collections.emptyList();
		}
	}

	private BigDecimal sumInboundForOrder(Long mOrderId) {
		if (!hasTable("material_inbounds") || mOrderId == null) {
			return BigDecimal.ZERO;
		}
		String sql = "select coalesce(sum(received_qty), 0) from material_inbounds where m_order_id = :mOrderId";
		MapSqlParameterSource params = new MapSqlParameterSource("mOrderId", mOrderId);
		BigDecimal result = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
		return result != null ? result : BigDecimal.ZERO;
	}

	private BigDecimal sumIssuedForOrder(Long mOrderId) {
		if (!hasTable("material_outbounds") || mOrderId == null) {
			return BigDecimal.ZERO;
		}
		String sql = "select coalesce(sum(issued_out_qty), 0) from material_outbounds where m_order_id = :mOrderId";
		MapSqlParameterSource params = new MapSqlParameterSource("mOrderId", mOrderId);
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

	private String selectOrNull(String column, String alias) {
		return column != null ? column + " as " + alias : "null as " + alias;
	}

	private String selectCoalesce(String primaryPrefix, String primaryColumn, String fallbackPrefix,
			String fallbackColumn, String alias) {
		if (primaryColumn == null && fallbackColumn == null) {
			return "null as " + alias;
		}
		if (primaryColumn != null && fallbackColumn != null) {
			return "coalesce(" + primaryPrefix + primaryColumn + ", " + fallbackPrefix + fallbackColumn + ") as "
					+ alias;
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

	private String resolveErrorDetail(Exception e) {
		if (e == null) {
			return "알 수 없는 오류";
		}
		Throwable root = e;
		while (root.getCause() != null && root.getCause() != root) {
			root = root.getCause();
		}
		String message = root.getMessage();
		String type = root.getClass().getSimpleName();
		if (StringUtils.hasText(message)) {
			return type + ": " + message;
		}
		return type;
	}

	private Map<String, Object> buildInboundError(MaterialTransactionSaveLine line, BigDecimal qty, String sql,
			Exception e) {
		Map<String, Object> error = new LinkedHashMap<>();
		error.put("mOrderId", line != null ? line.getMOrderId() : null);
		error.put("quantity", qty);
		error.put("sql", sql);
		error.put("errorMessage", e != null ? e.getMessage() : null);
		Throwable root = resolveRootCause(e);
		error.put("rootCause", root != null ? root.getMessage() : null);
		SQLException sqlException = findSqlException(e);
		if (sqlException != null) {
			error.put("sqlState", sqlException.getSQLState());
			error.put("errorCode", sqlException.getErrorCode());
		}
		return error;
	}

	private String buildInboundSummaryMessage(String prefix, List<Map<String, Object>> errors) {
		if (errors == null || errors.isEmpty()) {
			return prefix;
		}
		List<String> summaries = new ArrayList<>();
		int limit = Math.min(errors.size(), 3);
		for (int i = 0; i < limit; i++) {
			Map<String, Object> error = errors.get(i);
			summaries.add(formatInboundErrorSummary(error));
		}
		String suffix = errors.size() > limit ? " 외 " + (errors.size() - limit) + "건" : "";
		return prefix + " " + String.join(" | ", summaries) + suffix + " 자세한 내용은 서버 로그를 확인하세요.";
	}

	private String formatInboundErrorSummary(Map<String, Object> error) {
		if (error == null) {
			return "알 수 없는 오류";
		}
		Object mOrderId = error.get("mOrderId");
		Object quantity = error.get("quantity");
		Object sqlState = error.get("sqlState");
		Object errorCode = error.get("errorCode");
		Object rootCause = error.get("rootCause");
		Object errorMessage = error.get("errorMessage");
		StringBuilder summary = new StringBuilder();
		summary.append("mOrderId=").append(mOrderId);
		if (quantity != null) {
			summary.append(", qty=").append(quantity);
		}
		if (sqlState != null) {
			summary.append(", SQLState=").append(sqlState);
		}
		if (errorCode != null) {
			summary.append(", errorCode=").append(errorCode);
		}
		if (rootCause != null && StringUtils.hasText(rootCause.toString())) {
			summary.append(", rootCause=").append(rootCause);
		} else if (errorMessage != null && StringUtils.hasText(errorMessage.toString())) {
			summary.append(", error=").append(errorMessage);
		}
		return summary.toString();
	}

	private Throwable resolveRootCause(Throwable throwable) {
		if (throwable == null) {
			return null;
		}
		Throwable root = throwable;
		while (root.getCause() != null && root.getCause() != root) {
			root = root.getCause();
		}
		return root;
	}

	private SQLException findSqlException(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof SQLException sqlException) {
				return sqlException;
			}
			current = current.getCause();
		}
		return null;
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
		String sql = "select " + idColumn + " as styles_id from styles where " + codeColumn + " = :styleCode limit 1";
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

	private List<String> findMissingColumns(String tableName, List<String> columns) {
		List<String> missing = new ArrayList<>();
		for (String column : columns) {
			if (!hasColumn(tableName, column)) {
				missing.add(column);
			}
		}
		if (!missing.isEmpty()) {
			List<String> existing = loadColumnNames(tableName);
			log.warn("{} 누락 컬럼 감지. missingColumns={}, columns={}", tableName, missing, existing);
		}
		return missing;
	}
}
