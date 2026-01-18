package com.example.erp.service;

import com.example.erp.controller.dto.FgReceiptJobView;
import com.example.erp.controller.dto.FgReceiptListResponse;
import com.example.erp.controller.dto.FgReceiptSaveLine;
import com.example.erp.controller.dto.FgReceiptSaveRequest;
import com.example.erp.controller.dto.FgWarehouseCodeView;
import com.example.erp.domain.FgReceipt;
import com.example.erp.repository.FgReceiptRepository;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class FgReceiptService {

    private static final Logger log = LoggerFactory.getLogger(FgReceiptService.class);
    private final FgReceiptRepository fgReceiptRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public FgReceiptService(FgReceiptRepository fgReceiptRepository, NamedParameterJdbcTemplate jdbcTemplate) {
        this.fgReceiptRepository = fgReceiptRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FgReceipt> findAll() {
        return fgReceiptRepository.findAll();
    }

    public FgReceiptListResponse findReceiptTargets(String styleCode) {
        if (!hasTable("production_jobs")) {
            return new FgReceiptListResponse(List.of());
        }
        String agreementColumn = findFirstExistingColumn("production_jobs", List.of("agreement_code", "prd_agree_code"));
        String colorColumn = findFirstExistingColumn("production_jobs", List.of("color_code"));
        String styleColumn = findFirstExistingColumn("production_jobs", List.of("style_code", "styles_code"));
        String styleIdColumn = findFirstExistingColumn("production_jobs", List.of("styles_id", "style_id"));
        String qtyColumn = findFirstExistingColumn("production_jobs", List.of("quantity", "order_qty", "agree_qty"));
        String producerColumn = findFirstExistingColumn("production_jobs", List.of("factory_code", "producer_code"));
        String prdAgreeIdColumn = findFirstExistingColumn("production_jobs", List.of("prd_agree_id"));

        String styleJoin = "";
        String styleCodeColumn = null;
        if (styleColumn == null && styleIdColumn != null && hasTable("styles")) {
            String stylesIdColumn = findFirstExistingColumn("styles", List.of("styles_id", "style_id", "id"));
            styleCodeColumn = findFirstExistingColumn("styles", List.of("style_code", "styles_code"));
            if (stylesIdColumn != null && styleCodeColumn != null) {
                styleJoin = " left join styles s on s." + stylesIdColumn + " = pj." + styleIdColumn + " ";
            }
        }

        List<String> selectColumns = new ArrayList<>();
        if (styleIdColumn != null) {
            selectColumns.add("pj." + styleIdColumn + " as styles_id");
        }
        if (styleColumn != null) {
            selectColumns.add("pj." + styleColumn + " as style_code");
        } else if (styleCodeColumn != null) {
            selectColumns.add("s." + styleCodeColumn + " as style_code");
        }
        if (agreementColumn != null) {
            selectColumns.add("pj." + agreementColumn + " as prd_agree_code");
        }
        if (prdAgreeIdColumn != null) {
            selectColumns.add("pj." + prdAgreeIdColumn + " as prd_agree_id");
        }
        if (colorColumn != null) {
            selectColumns.add("pj." + colorColumn + " as color_code");
        }
        if (producerColumn != null) {
            selectColumns.add("pj." + producerColumn + " as factory_code");
        }
        if (qtyColumn != null) {
            selectColumns.add("pj." + qtyColumn + " as quantity");
        }

        if (selectColumns.isEmpty()) {
            return new FgReceiptListResponse(List.of());
        }

        StringBuilder sql = new StringBuilder()
                .append("select ")
                .append(String.join(", ", selectColumns))
                .append(" from production_jobs pj")
                .append(styleJoin);

        MapSqlParameterSource params = new MapSqlParameterSource();
        if (StringUtils.hasText(styleCode)) {
            String keyword = "%" + styleCode.trim().toLowerCase(Locale.getDefault()) + "%";
            if (styleColumn != null) {
                sql.append(" where lower(pj.").append(styleColumn).append(") like :styleCode ");
                params.addValue("styleCode", keyword);
            } else if (styleCodeColumn != null) {
                sql.append(" where lower(s.").append(styleCodeColumn).append(") like :styleCode ");
                params.addValue("styleCode", keyword);
            }
        }

        List<FgReceiptJobView> items = jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            String stylesId = rs.getString("styles_id");
            String styleCodeValue = rs.getString("style_code");
            String prdAgreeCode = rs.getString("prd_agree_code");
            Long prdAgreeId = rs.getObject("prd_agree_id") != null ? rs.getLong("prd_agree_id") : null;
            String colorCode = rs.getString("color_code");
            String factoryCode = rs.getString("factory_code");
            BigDecimal quantity = rs.getBigDecimal("quantity");
            return new FgReceiptJobView(stylesId, styleCodeValue, prdAgreeCode, prdAgreeId, colorCode, factoryCode,
                    quantity);
        });

        return new FgReceiptListResponse(items);
    }

    public List<FgWarehouseCodeView> findWarehouses(String keyword) {
        if (!hasTable("codes")) {
            return List.of();
        }
        String codeColumn = findFirstExistingColumn("codes", List.of("code"));
        String codeTypeColumn = findFirstExistingColumn("codes", List.of("code_type", "id_code_type"));
        String nameColumn = findFirstExistingColumn("codes", List.of("code_name", "name"));
        String remarkColumn = findFirstExistingColumn("codes", List.of("remark"));
        if (codeColumn == null || codeTypeColumn == null) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder()
                .append("select ")
                .append(codeColumn).append(" as code, ")
                .append(nameColumn != null ? nameColumn : "null").append(" as code_name, ")
                .append(remarkColumn != null ? remarkColumn : "null").append(" as remark ")
                .append("from codes where ").append(codeTypeColumn).append(" = :codeType ");

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("codeType", "WAREHOUSE");
        if (StringUtils.hasText(keyword)) {
            sql.append("and (")
                    .append(codeColumn).append(" like :keyword ");
            if (nameColumn != null) {
                sql.append("or ").append(nameColumn).append(" like :keyword ");
            }
            if (remarkColumn != null) {
                sql.append("or ").append(remarkColumn).append(" like :keyword ");
            }
            sql.append(") ");
            params.addValue("keyword", "%" + keyword.trim() + "%");
        }

        return jdbcTemplate.query(sql.toString(), params,
                (rs, rowNum) -> new FgWarehouseCodeView(rs.getString("code"), rs.getString("code_name"),
                        rs.getString("remark")));
    }

    @Transactional
    public Map<String, Object> saveInbound(FgReceiptSaveRequest request, String empNo) {
        Set<String> missingFields = new LinkedHashSet<>();
        List<Map<String, Object>> lineErrors = new ArrayList<>();

        if (!hasTable("fg_inbounds")) {
            return Map.of("success", false, "message", "입고 등록 실패 (fg_inbounds 테이블 없음)",
                    "missingFields", List.of("fg_inbounds"));
        }
        if (request == null || CollectionUtils.isEmpty(request.getItems())) {
            return Map.of("success", false, "message", "입고 등록 실패 (입고 데이터 없음)",
                    "missingFields", List.of("items"));
        }
        if (!StringUtils.hasText(empNo)) {
            missingFields.add("empNo");
        }

        String stylesIdColumn = findFirstExistingColumn("fg_inbounds", List.of("styles_id", "style_id"));
        String prdAgreeIdColumn = findFirstExistingColumn("fg_inbounds", List.of("prd_agree_id"));
        String colorTypeColumn = findFirstExistingColumn("fg_inbounds", List.of("color_type"));
        String colorCodeColumn = findFirstExistingColumn("fg_inbounds", List.of("color_code"));
        String factoryTypeColumn = findFirstExistingColumn("fg_inbounds", List.of("factory_type"));
        String factoryCodeColumn = findFirstExistingColumn("fg_inbounds", List.of("factory_code", "producer_code"));
        String warehouseTypeColumn = findFirstExistingColumn("fg_inbounds", List.of("warehouse_type"));
        String warehouseCodeColumn = findFirstExistingColumn("fg_inbounds", List.of("warehouse_code"));
        String aisleColumn = findFirstExistingColumn("fg_inbounds", List.of("aisle"));
        String sectionColumn = findFirstExistingColumn("fg_inbounds", List.of("section"));
        String levelColumn = findFirstExistingColumn("fg_inbounds", List.of("level"));
        String lcCodeColumn = findFirstExistingColumn("fg_inbounds", List.of("lc_code"));
        String inboundQtyColumn = findFirstExistingColumn("fg_inbounds", List.of("inbound_qty", "received_qty", "qty"));
        String inboundDatetimeColumn = findFirstExistingColumn("fg_inbounds",
                List.of("inbound_datetime", "inbound_date"));
        String createdDateColumn = findFirstExistingColumn("fg_inbounds", List.of("created_date", "created_at"));
        String updatedDateColumn = findFirstExistingColumn("fg_inbounds", List.of("updated_date", "updated_at"));
        String createdByColumn = findFirstExistingColumn("fg_inbounds", List.of("created_by"));
        String updatedByColumn = findFirstExistingColumn("fg_inbounds", List.of("updated_by"));

        if (stylesIdColumn == null) {
            missingFields.add("fg_inbounds.styles_id");
        }
        if (prdAgreeIdColumn == null) {
            missingFields.add("fg_inbounds.prd_agree_id");
        }
        if (colorCodeColumn == null) {
            missingFields.add("fg_inbounds.color_code");
        }
        if (factoryCodeColumn == null) {
            missingFields.add("fg_inbounds.factory_code");
        }
        if (warehouseCodeColumn == null) {
            missingFields.add("fg_inbounds.warehouse_code");
        }
        if (inboundQtyColumn == null) {
            missingFields.add("fg_inbounds.inbound_qty");
        }

        if (!missingFields.isEmpty()) {
            return Map.of("success", false, "message", "입고 등록 실패 (필수 컬럼 누락)", "missingFields",
                    new ArrayList<>(missingFields));
        }

        boolean canUseInventory = hasTable("fg_inventory")
                && hasColumn("fg_inventory", "styles_id")
                && hasColumn("fg_inventory", "color_type")
                && hasColumn("fg_inventory", "color_code")
                && hasColumn("fg_inventory", "warehouse_type")
                && hasColumn("fg_inventory", "warehouse_code")
                && hasColumn("fg_inventory", "aisle")
                && hasColumn("fg_inventory", "section")
                && hasColumn("fg_inventory", "level")
                && hasColumn("fg_inventory", "qty");
        if (!canUseInventory) {
            log.warn("fg_inventory 테이블 또는 필수 컬럼이 없습니다. 재고 반영을 건너뜁니다.");
        }

        int created = 0;
        int skipped = 0;
        int inventorySkipped = 0;
        int index = 0;
        for (FgReceiptSaveLine line : request.getItems()) {
            index++;
            if (line == null) {
                lineErrors.add(Map.of("index", index, "error", "line is null"));
                skipped++;
                continue;
            }

            BigDecimal inboundQty = line.getInboundQty();
            if (!StringUtils.hasText(line.getColorCode())) {
                lineErrors.add(Map.of("index", index, "error", "색상 누락"));
                skipped++;
                continue;
            }
            String warehouseCode = normalizeCode(line.getWarehouseCode());
            String aisle = normalizeLocation(line.getAisle());
            String section = normalizeLocation(line.getSection());
            String level = normalizeLocation(line.getLevel());
            boolean hasInventoryInputs = StringUtils.hasText(warehouseCode)
                    && inboundQty != null
                    && inboundQty.compareTo(BigDecimal.ZERO) > 0
                    && StringUtils.hasText(aisle)
                    && StringUtils.hasText(section)
                    && StringUtils.hasText(level);

            String styleId = resolveStyleId(line.getStylesId(), line.getStyleCode());
            Long prdAgreeId = line.getPrdAgreeId();
            if (prdAgreeId == null && StringUtils.hasText(line.getPrdAgreeCode())) {
                prdAgreeId = resolvePrdAgreeId(line.getPrdAgreeCode(), line.getColorCode());
            }
            if (!StringUtils.hasText(styleId) || prdAgreeId == null) {
                lineErrors.add(Map.of("index", index, "error", "stylesId/prdAgreeId 누락"));
                skipped++;
                continue;
            }

            String lcCode = buildLcCode(warehouseCode, aisle, section, level);
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("stylesId", styleId)
                    .addValue("prdAgreeId", prdAgreeId)
                    .addValue("colorType", "color")
                    .addValue("colorCode", line.getColorCode())
                    .addValue("factoryType", "customer")
                    .addValue("factoryCode", line.getFactoryCode())
                    .addValue("warehouseType", "warehouse")
                    .addValue("warehouseCode", warehouseCode)
                    .addValue("aisle", aisle)
                    .addValue("section", section)
                    .addValue("level", level)
                    .addValue("lcCode", lcCode)
                    .addValue("inboundQty", inboundQty)
                    .addValue("nowDate", Timestamp.valueOf(LocalDateTime.now()))
                    .addValue("updatedBy", empNo)
                    .addValue("createdBy", empNo);

            List<String> columns = new ArrayList<>();
            List<String> values = new ArrayList<>();

            columns.add(stylesIdColumn);
            values.add(":stylesId");
            columns.add(prdAgreeIdColumn);
            values.add(":prdAgreeId");
            if (colorTypeColumn != null) {
                columns.add(colorTypeColumn);
                values.add(":colorType");
            }
            if (colorCodeColumn != null) {
                columns.add(colorCodeColumn);
                values.add(":colorCode");
            }
            if (factoryTypeColumn != null) {
                columns.add(factoryTypeColumn);
                values.add(":factoryType");
            }
            if (factoryCodeColumn != null) {
                columns.add(factoryCodeColumn);
                values.add(":factoryCode");
            }
            if (warehouseTypeColumn != null) {
                columns.add(warehouseTypeColumn);
                values.add(":warehouseType");
            }
            if (warehouseCodeColumn != null) {
                columns.add(warehouseCodeColumn);
                values.add(":warehouseCode");
            }
            if (aisleColumn != null) {
                columns.add(aisleColumn);
                values.add(":aisle");
            }
            if (sectionColumn != null) {
                columns.add(sectionColumn);
                values.add(":section");
            }
            if (levelColumn != null) {
                columns.add(levelColumn);
                values.add(":level");
            }
            if (lcCodeColumn != null) {
                columns.add(lcCodeColumn);
                values.add(":lcCode");
            }
            columns.add(inboundQtyColumn);
            values.add(":inboundQty");
            if (inboundDatetimeColumn != null) {
                columns.add(inboundDatetimeColumn);
                values.add(":nowDate");
            }
            if (createdDateColumn != null) {
                columns.add(createdDateColumn);
                values.add(":nowDate");
            }
            if (updatedDateColumn != null) {
                columns.add(updatedDateColumn);
                values.add(":nowDate");
            }
            if (createdByColumn != null) {
                columns.add(createdByColumn);
                values.add(":createdBy");
            }
            if (updatedByColumn != null) {
                columns.add(updatedByColumn);
                values.add(":updatedBy");
            }

            String sql = "insert into fg_inbounds (" + String.join(", ", columns) + ") values ("
                    + String.join(", ", values) + ")";
            try {
                int affected = jdbcTemplate.update(sql, params);
                if (affected > 0) {
                    created++;
                    if (canUseInventory && hasInventoryInputs) {
                        try {
                            upsertFgInventory(styleId, line.getColorCode(), warehouseCode, aisle, section, level,
                                    inboundQty, params.getValue("nowDate"));
                        } catch (Exception ex) {
                            log.warn("fg_inventory 반영 실패: stylesId={}, prdAgreeId={}, detail={}", styleId, prdAgreeId,
                                    ex.getMessage());
                            lineErrors.add(Map.of("index", index, "error", "fgInventoryUpsertFailed",
                                    "detail", ex.getMessage()));
                        }
                    } else if (canUseInventory) {
                        inventorySkipped++;
                        lineErrors.add(Map.of("index", index, "error", "fgInventorySkipped",
                                "detail", "창고/수량/위치 누락"));
                    } else {
                        lineErrors.add(Map.of("index", index, "error", "fgInventorySkipped",
                                "detail", "fg_inventory 테이블/컬럼 없음"));
                    }
                } else {
                    skipped++;
                    lineErrors.add(Map.of("index", index, "error", "insertFailed"));
                }
            } catch (Exception ex) {
                skipped++;
                log.warn("fg_inbounds 저장 실패: {}", ex.getMessage());
                lineErrors.add(Map.of("index", index, "error", "insertFailed", "detail", ex.getMessage()));
            }
        }

        boolean success = created > 0;
        String message = success
                ? (inventorySkipped > 0
                        ? "입고내역 저장 완료(일부 라인은 창고/수량/위치 누락으로 재고 반영 제외) (created=" + created
                                + ", skipped=" + skipped + ", inventorySkipped=" + inventorySkipped + ")"
                        : "입고 등록 완료 (created=" + created + ", skipped=" + skipped + ")")
                : "입고 등록 실패 (created=0, skipped=" + skipped + ") - lineErrors 확인";
        List<Map<String, Object>> lineErrorsLimited = lineErrors.size() > 20 ? lineErrors.subList(0, 20) : lineErrors;
        return Map.of("success", success, "created", created, "skipped", skipped, "message", message, "lineErrors",
                lineErrorsLimited);
    }

    private void upsertFgInventory(String stylesId, String colorCode, String warehouseCode, String aisle,
            String section, String level, BigDecimal inboundQty, Object nowValue) {
        String updatedColumn = findFirstExistingColumn("fg_inventory", List.of("updated_at", "updated_date"));
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("stylesId", stylesId)
                .addValue("colorType", "color")
                .addValue("colorCode", colorCode)
                .addValue("warehouseType", "warehouse")
                .addValue("warehouseCode", warehouseCode)
                .addValue("aisle", aisle)
                .addValue("section", section)
                .addValue("level", level)
                .addValue("qty", inboundQty)
                .addValue("nowDate", nowValue);

        String updateSql = "update fg_inventory set qty = qty + :qty"
                + (updatedColumn != null ? ", " + updatedColumn + " = :nowDate" : "")
                + " where styles_id = :stylesId and color_type = :colorType and color_code = :colorCode "
                + "and warehouse_type = :warehouseType and warehouse_code = :warehouseCode "
                + "and aisle = :aisle and section = :section and level = :level";
        int updated = jdbcTemplate.update(updateSql, params);
        if (updated > 0) {
            return;
        }
        String insertSql = "insert into fg_inventory (styles_id, color_type, color_code, warehouse_type, warehouse_code, "
                + "aisle, section, level, qty"
                + (updatedColumn != null ? ", " + updatedColumn : "")
                + ") values (:stylesId, :colorType, :colorCode, :warehouseType, :warehouseCode, :aisle, :section, :level, :qty"
                + (updatedColumn != null ? ", :nowDate" : "")
                + ")";
        jdbcTemplate.update(insertSql, params);
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeLocation(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        if (!StringUtils.hasText(digits)) {
            return null;
        }
        if (digits.length() == 1) {
            return "0" + digits;
        }
        return digits.length() > 2 ? digits.substring(digits.length() - 2) : digits;
    }

    private String buildLcCode(String warehouseCode, String aisle, String section, String level) {
        if (!StringUtils.hasText(warehouseCode) || !StringUtils.hasText(aisle)
                || !StringUtils.hasText(section) || !StringUtils.hasText(level)) {
            return null;
        }
        String raw = warehouseCode.trim();
        String prefix = raw.length() > 1 ? raw.substring(1).replaceFirst("^0+", "") : raw;
        if (!StringUtils.hasText(prefix)) {
            prefix = "0";
        }
        return prefix + "-" + aisle + "-" + section + "-" + level;
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
        if (!hasTable("production_agreements")) {
            return null;
        }
        String prdAgreeIdColumn = findFirstExistingColumn("production_agreements", List.of("prd_agree_id",
                "prd_agree_no", "prd_agree_seq"));
        String agreementCodeCol = findFirstExistingColumn("production_agreements",
                List.of("agreement_code", "prd_agree_code", "prd_agree_no"));
        String colorCol = findFirstExistingColumn("production_agreements", List.of("color_code", "color"));
        if (prdAgreeIdColumn == null || agreementCodeCol == null || colorCol == null) {
            return null;
        }
        String sql = "select max(" + prdAgreeIdColumn + ") as prd_agree_id from production_agreements where "
                + agreementCodeCol + " = :agreementCode and " + colorCol + " = :colorCode";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("agreementCode", agreementCode)
                .addValue("colorCode", colorCode);
        List<Long> result = jdbcTemplate.query(sql, params,
                (rs, rowNum) -> rs.getObject("prd_agree_id") != null ? rs.getLong("prd_agree_id") : null);
        return result.isEmpty() ? null : result.get(0);
    }

    private boolean hasTable(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    select count(*)
                    from information_schema.tables
                    where upper(table_name) = upper(:tableName)
                    """, new MapSqlParameterSource("tableName", tableName), Integer.class);
            return count != null && count > 0;
        } catch (Exception ex) {
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
        } catch (Exception ex) {
            return false;
        }
    }

    private String findFirstExistingColumn(String tableName, List<String> candidates) {
        for (String column : candidates) {
            if (hasColumn(tableName, column)) {
                return column;
            }
        }
        return null;
    }
}
