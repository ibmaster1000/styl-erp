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
		try {
			if (!hasTable("material_inbounds")) {
				return Map.of("success", false, "created", 0, "skipped", 0, "message", "입고 테이블이 없습니다.");
			}
			if (!hasTable("material_orders")) {
				return Map.of("success", false, "created", 0, "skipped", 0, "message", "발주 테이블이 없습니다.");
			}
			if (CollectionUtils.isEmpty(request.getItems())) {
				return Map.of("success", false, "created", 0, "skipped", 0, "message", "입고 항목이 없습니다.");
			}

			List<String> missingColumns = findMissingColumns("material_inbounds",
					List.of("m_order_id", "received_qty"));
			if (!missingColumns.isEmpty()) {
				return Map.of("success", false, "created", 0, "skipped", 0,
						"message", "입고 컬럼이 부족합니다. missingColumns=" + missingColumns);
			}

			boolean hasSpecs = hasTable("material_specs");
			boolean hasInboundDatetime = hasColumn("material_inbounds", "inbound_datetime");
			boolean hasStylesId = hasColumn("material_inbounds", "styles_id");
			boolean hasPrdAgreeId = hasColumn("material_inbounds", "prd_agree_id");
			boolean hasColorType = hasColumn("material_inbounds", "color_type");
			boolean hasColorCode = hasColumn("material_inbounds", "color_code");
			boolean hasOrderUom = hasColumn("material_inbounds", "order_uom");
			boolean hasUnitPrice = hasColumn("material_inbounds", "unit_price");
			boolean hasProducerType = hasColumn("material_inbounds", "producer_type");
			boolean hasProducerCode = hasColumn("material_inbounds", "producer_code");
			boolean hasWarehouseType = hasColumn("material_inbounds", "warehouse_type");
			boolean hasWarehouseCode = hasColumn("material_inbounds", "warehouse_code");
			boolean hasCreatedBy = hasColumn("material_inbounds", "created_by");
			boolean hasRemark = hasColumn("material_inbounds", "remark");
			boolean hasPlannedQty = hasColumn("material_inbounds", "planned_qty");

			String agreementCodeCol = findFirstExistingColumn("production_agreements",
					List.of("agreement_code", "prd_agree_code", "prd_agree_no"));
			String agreementColorCol = findFirstExistingColumn("production_agreements",
					List.of("color_code", "color"));
			String agreementQtyCol = findFirstExistingColumn("production_agreements", List.of("quantity", "qty"));
			String lossRateCol = findFirstExistingColumn("material_specs", List.of("loss_rate", "loss"));
			String lossExpr = lossRateCol != null ? "coalesce(ms." + lossRateCol + ", 0)" : "0";
			boolean hasAgreementCols = agreementCodeCol != null && agreementColorCol != null && agreementQtyCol != null;
			String plannedQtyExpr;
			if (hasAgreementCols) {
				String agreementSumExpr = "(select coalesce(sum(" + agreementQtyCol + "), 0)"
						+ " from production_agreements"
						+ " where " + agreementCodeCol + " = :prdAgreeCode"
						+ " and " + agreementColorCol + " = :colorCode)";
				plannedQtyExpr = "round(" + agreementSumExpr
						+ " * coalesce(ms.qty_per_piece, 0) * (1 + (" + lossExpr + " / 100.0)), 3)";
			} else {
				if (hasPlannedQty) {
					log.warn("planned_qty 계산에 필요한 production_agreements 컬럼을 찾지 못했습니다.");
				}
				plannedQtyExpr = "0";
			}

			Long prdAgreeId = resolvePrdAgreeId(request.getPrdAgreeCode(), request.getColorCode());
			int created = 0;
			int skipped = 0;
			for (MaterialTransactionSaveLine line : request.getItems()) {
				BigDecimal qty = safeDecimal(line.getQuantity());
				if (qty.compareTo(BigDecimal.ZERO) <= 0 || line.getMOrderId() == null) {
					skipped++;
					continue;
				}

				MapSqlParameterSource params = new MapSqlParameterSource()
						.addValue("mOrderId", line.getMOrderId())
						.addValue("receivedQty", qty)
						.addValue("inboundDatetime", Timestamp.valueOf(LocalDateTime.now()))
						.addValue("createdBy", empNo)
						.addValue("remark", line.getRemark())
						.addValue("prdAgreeCode", request.getPrdAgreeCode())
						.addValue("colorCode", request.getColorCode());

				List<String> columns = new ArrayList<>();
				List<String> values = new ArrayList<>();
				columns.add("m_order_id");
				values.add("mo.m_order_id");
				columns.add("received_qty");
				values.add(":receivedQty");
				if (hasInboundDatetime) {
					columns.add("inbound_datetime");
					values.add(":inboundDatetime");
				}
				if (hasStylesId) {
					columns.add("styles_id");
					values.add("mo.styles_id");
				}
				if (hasPrdAgreeId) {
					columns.add("prd_agree_id");
					values.add("mo.prd_agree_id");
				}
				if (hasColorType) {
					columns.add("color_type");
					values.add("'COLOR'");
				}
				if (hasColorCode) {
					columns.add("color_code");
					values.add("mo.color_code");
				}
				if (hasOrderUom) {
					columns.add("order_uom");
					values.add(hasSpecs ? "ms.order_uom" : "null");
				}
				if (hasUnitPrice) {
					columns.add("unit_price");
					values.add("mo.unit_price");
				}
				if (hasProducerType) {
					columns.add("producer_type");
					values.add("'CUSTOMER'");
				}
				if (hasProducerCode) {
					columns.add("producer_code");
					values.add("mo.vendor_code");
				}
				if (hasWarehouseType) {
					columns.add("warehouse_type");
					values.add("'WAREHOUSE'");
				}
				if (hasWarehouseCode) {
					columns.add("warehouse_code");
					values.add("mo.warehouse_code");
				}
				if (hasCreatedBy) {
					columns.add("created_by");
					values.add(":createdBy");
				}
				if (hasRemark) {
					columns.add("remark");
					values.add(":remark");
				}
				if (hasPlannedQty) {
					columns.add("planned_qty");
					values.add(plannedQtyExpr);
				}

				String sql = "insert into material_inbounds (" + String.join(", ", columns) + ") select "
						+ String.join(", ", values) + " from material_orders mo "
						+ (hasSpecs ? "left join material_specs ms on ms.bom_id = mo.bom_id " : "")
						+ "where mo.m_order_id = :mOrderId";
				log.info("Executing inbound SQL: {} params: mOrderId={}, prdAgreeId={}, colorCode={}, stylesId={}",
						sql, line.getMOrderId(), prdAgreeId, request.getColorCode(), request.getStylesId());
				int affected = jdbcTemplate.update(sql, params);
				if (affected > 0) {
					created++;
				} else {
					skipped++;
				}
			}

			return Map.of("success", created > 0, "created", created, "skipped", skipped,
					"message", created > 0 ? "입고가 등록되었습니다." : "입고 등록에 실패했습니다.");
		} catch (Exception e) {
			String errorId = UUID.randomUUID().toString();
			log.error("입고 저장 중 오류가 발생했습니다. errorId={}", errorId, e);
			return Map.of("success", false, "created", 0, "skipped", 0,
					"message", "입고 등록 중 오류가 발생했습니다. errorId=" + errorId);
		}
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
				return Map.of("success", false, "created", 0, "skipped", 0,
						"message", "출고 컬럼이 부족합니다. missingColumns=" + missingColumns);
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

				MapSqlParameterSource params = new MapSqlParameterSource()
						.addValue("mOrderId", line.getMOrderId())
						.addValue("plannedOutQty", BigDecimal.ZERO)
						.addValue("issuedOutQty", issued)
						.addValue("receiverType", "CUSTOMER")
						.addValue("receiverCode", line.getFactoryCode())
						.addValue("outboundDatetime", Timestamp.valueOf(LocalDateTime.now()))
						.addValue("createdBy", empNo)
						.addValue("remark", line.getRemark());

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

			return Map.of("success", created > 0, "created", created, "skipped", skipped,
					"message", created > 0 ? "출고가 등록되었습니다." : "출고 등록에 실패했습니다.");
		} catch (Exception e) {
			String errorId = UUID.randomUUID().toString();
			log.error("출고 저장 중 오류가 발생했습니다. errorId={}", errorId, e);
			return Map.of("success", false, "created", 0, "skipped", 0,
					"message", "출고 등록 중 오류가 발생했습니다. errorId=" + errorId);
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
		String agreementColorCol = findFirstExistingColumn("production_agreements",
				List.of("color_code", "color"));
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
			requiredExpr = "coalesce(pa.agreement_qty, 0) * coalesce(ms.qty_per_piece, 0) "
					+ "* (1 + (" + lossExpr + " / 100.0))";
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
				outboundSelect = "0 as planned_out_qty, "
						+ "coalesce(mo2.issued_qty, 0) as issued_out_qty, ";
			}
		}

		StringBuilder sql = new StringBuilder()
				.append("select mo.m_order_id as m_order_id, ")
				.append("mo.bom_id as bom_id, ")
				.append("mo.styles_id as styles_id, ")
				.append("s.style_code as style_code, ")
				.append("coalesce(s.product_emp_no, '') as production_emp_no, ")
				.append("ms.category as category, ")
				.append("ms.material_name as material_name, ")
				.append("ms.material_usage as material_usage, ")
				.append("ms.spec as spec, ")
				.append("ms.material_color as material_color, ")
				.append("ms.uom as uom, ")
				.append("ms.qty_per_piece as qty_per_piece, ")
				.append("coalesce(mo.vendor_code, ms.supplier_code) as supplier_code, ")
				.append("ms.order_uom as order_uom, ")
				.append("mo.unit_price as unit_price, ")
				.append("mo.order_amount as order_amount, ")
				.append(requiredSelect)
				.append(inboundSelect)
				.append(includeOutbound ? outboundSelect : "0 as planned_out_qty, 0 as issued_out_qty, ")
				.append("ms.remark as remark ")
				.append("from material_orders mo ")
				.append("join material_specs ms on mo.bom_id = ms.bom_id ")
				.append("left join styles s on s.styles_id = mo.styles_id ")
				.append(requiredJoin)
				.append(inboundJoin)
				.append(outboundJoin)
				.append("where mo.prd_agree_id = :prdAgreeId ")
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
				.addValue("prdAgreeCode", prdAgreeCode)
				.addValue("styleCode", styleCode);
		log.info("Executing material list SQL: {} params: prdAgreeId={}, colorCode={}, stylesId={}",
				sql, prdAgreeId, colorCode, resolvedStyleId);

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
					.setProductionManager(rs.getString("production_emp_no"))
					.setOrderUom(rs.getString("order_uom"))
					.setUnitPrice(rs.getBigDecimal("unit_price"))
					.setOrderQuantity(rs.getBigDecimal("order_amount"))
					.setRequiredQuantity(rs.getBigDecimal("required_qty"))
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
