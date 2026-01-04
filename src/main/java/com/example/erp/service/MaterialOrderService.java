package com.example.erp.service;

import com.example.erp.controller.dto.MaterialOrderItemView;
import com.example.erp.controller.dto.MaterialOrderRequest;
import com.example.erp.controller.dto.MaterialOrderSelection;
import com.example.erp.controller.dto.MaterialOrderStyleResult;
import com.example.erp.controller.dto.MaterialOrderSupplierView;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.UUID;

@Service
public class MaterialOrderService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final StyleRuleService styleRuleService;

    public MaterialOrderService(NamedParameterJdbcTemplate jdbcTemplate, StyleRuleService styleRuleService) {
        this.jdbcTemplate = jdbcTemplate;
        this.styleRuleService = styleRuleService;
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

        StringBuilder sql = new StringBuilder()
                .append("select ")
                .append(idColumn).append(" as styles_id, ")
                .append(codeColumn).append(" as style_code, ")
                .append(itemColumn != null ? itemColumn : "null").append(" as item, ")
                .append(activeColumn != null ? activeColumn : "false").append(" as is_active ")
                .append("from styles ");

        MapSqlParameterSource params = new MapSqlParameterSource();
        if (StringUtils.hasText(keyword)) {
            sql.append("where (")
                    .append(codeColumn).append(" like concat('%', :kw, '%') ");
            if (itemColumn != null) {
                sql.append("or ").append(itemColumn).append(" like concat('%', :kw, '%') ");
            }
            sql.append(") ");
            params.addValue("kw", keyword.trim());
        }
        sql.append("order by ").append(codeColumn).append(" asc limit 50");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> new MaterialOrderStyleResult(
                rs.getString("styles_id"),
                rs.getString("style_code"),
                rs.getString("item"),
                rs.getBoolean("is_active")
        ));
    }

    public MaterialOrderSelection loadSelection(String stylesId, String styleCode) {
        String effectiveStyleCode = StringUtils.hasText(styleCode) ? styleCode : resolveStyleCode(stylesId);
        Map<String, List<String>> styleRule = Collections.emptyMap();
        if (StringUtils.hasText(effectiveStyleCode)) {
            styleRule = styleRuleService.findRulesByStyleNos(Set.of(effectiveStyleCode))
                    .getOrDefault(effectiveStyleCode, Collections.emptyMap());
        }

        List<String> colors = !styleRule.isEmpty()
                ? new ArrayList<>(styleRule.keySet())
                : findColorsFromSpecs(stylesId, effectiveStyleCode);
        colors.sort(String::compareToIgnoreCase);
        List<String> agreements = findAgreements(stylesId, effectiveStyleCode);

        return new MaterialOrderSelection(stylesId, effectiveStyleCode, colors, agreements, styleRule);
    }

    public List<MaterialOrderSupplierView> findSuppliers(String stylesId, String prdAgreeCode, String colorCode) {
        if (!StringUtils.hasText(prdAgreeCode) || !StringUtils.hasText(colorCode) || !hasTable("material_specs")) {
            return Collections.emptyList();
        }

        String supplierColumn = findFirstExistingColumn("material_specs", List.of("supplier_code"));
        String prdColumn = findFirstExistingColumn("material_specs", List.of("prd_agree_code", "agreement_code"));
        String colorColumn = findFirstExistingColumn("material_specs", List.of("color_code", "material_color", "color"));
        String styleColumn = findFirstExistingColumn("material_specs", List.of("styles_id", "style_id", "style_no"));

        if (supplierColumn == null || prdColumn == null || colorColumn == null) {
            return Collections.emptyList();
        }

        StringBuilder sql = new StringBuilder()
                .append("select distinct ").append(supplierColumn).append(" as supplier_code ")
                .append("from material_specs ")
                .append("where ").append(prdColumn).append(" = :prdAgreeCode ")
                .append("and ").append(colorColumn).append(" = :colorCode ");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("prdAgreeCode", prdAgreeCode)
                .addValue("colorCode", colorCode);

        if (StringUtils.hasText(stylesId) && styleColumn != null) {
            sql.append("and ").append(styleColumn).append(" = :stylesId ");
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
            boolean ordered = isSupplierAlreadyOrdered(stylesId, prdAgreeCode, colorCode, code);
            result.add(new MaterialOrderSupplierView(code, supplierNames.getOrDefault(code, "-"), ordered));
        }
        return result;
    }

    public List<MaterialOrderItemView> findMaterials(String stylesId,
            String prdAgreeCode,
            String colorCode,
            String supplierCode) {
        if (!hasTable("material_specs")) {
            return Collections.emptyList();
        }

        String prdColumn = findFirstExistingColumn("material_specs", List.of("prd_agree_code", "agreement_code"));
        String colorColumn = findFirstExistingColumn("material_specs", List.of("color_code", "material_color", "color"));
        String supplierColumn = findFirstExistingColumn("material_specs", List.of("supplier_code"));
        String styleColumn = findFirstExistingColumn("material_specs", List.of("styles_id", "style_id", "style_no"));
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
                where %s = :prdAgreeCode
                  and %s = :colorCode
                  and %s = :supplierCode
                %s
                order by coalesce(%s, 0) asc
                """.formatted(
                selectOrNull(bomIdColumn, "bom_id"),
                selectOrNull(colorColumn, "color_code"),
                selectOrNull("category"),
                selectOrNull("material_name"),
                selectOrNull("material_usage"),
                selectOrNull("spec"),
                selectOrNull("material_color"),
                selectOrNull("uom"),
                selectOrNull("qty_per_piece"),
                selectOrNull(supplierColumn, "supplier_code"),
                selectOrNull("loss_rate"),
                selectOrNull("order_uom"),
                selectOrNull("unit_price"),
                selectOrNull("remark"),
                prdColumn,
                colorColumn,
                supplierColumn,
                StringUtils.hasText(stylesId) && styleColumn != null ? "and " + styleColumn + " = :stylesId" : "",
                bomIdColumn != null ? bomIdColumn : prdColumn);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("prdAgreeCode", prdAgreeCode)
                .addValue("colorCode", colorCode)
                .addValue("supplierCode", supplierCode);
        if (StringUtils.hasText(stylesId) && styleColumn != null) {
            params.addValue("stylesId", stylesId);
        }

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new MaterialOrderItemView(
                rs.getObject("bom_id") != null ? rs.getLong("bom_id") : null,
                rs.getString("color_code"),
                rs.getString("category"),
                rs.getString("material_name"),
                rs.getString("material_usage"),
                rs.getString("spec"),
                rs.getString("material_color"),
                rs.getString("uom"),
                rs.getBigDecimal("qty_per_piece"),
                rs.getString("supplier_code"),
                rs.getBigDecimal("loss_rate"),
                rs.getString("order_uom"),
                rs.getBigDecimal("unit_price"),
                rs.getString("remark")
        ));
    }

    @Transactional
    public Map<String, Object> submitOrders(MaterialOrderRequest request, String orderedBy) {
        if (!hasTable("material_orders")) {
            return Map.of("success", false, "created", 0, "skipped", 0);
        }

        String orderCodeColumn = findFirstExistingColumn("material_orders", List.of("m_order_code", "order_code"));
        String prdColumn = findFirstExistingColumn("material_orders", List.of("prd_agree_code", "agreement_code"));
        String colorColumn = findFirstExistingColumn("material_orders", List.of("color_code"));
        String bomIdColumn = findFirstExistingColumn("material_orders", List.of("bom_id"));
        String supplierColumn = findFirstExistingColumn("material_orders", List.of("supplier_code"));
        String styleColumn = findFirstExistingColumn("material_orders", List.of("styles_id", "style_id", "style_no"));
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

        List<MaterialOrderItemView> specs = findMaterials(request.getStylesId(),
                request.getPrdAgreeCode(),
                request.getColorCode(),
                request.getSupplierCode());

        int created = 0;
        int skipped = 0;
        for (MaterialOrderItemView spec : specs) {
            if (spec.getBomId() == null) {
                skipped++;
                continue;
            }
            if (isOrderExists(request.getStylesId(), request.getPrdAgreeCode(), request.getColorCode(),
                    request.getSupplierCode(), spec.getBomId())) {
                skipped++;
                continue;
            }

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("mOrderCode", UUID.randomUUID().toString())
                    .addValue("stylesId", request.getStylesId())
                    .addValue("styleCode", request.getStyleCode())
                    .addValue("prdAgreeCode", request.getPrdAgreeCode())
                    .addValue("colorCode", request.getColorCode())
                    .addValue("bomId", spec.getBomId())
                    .addValue("supplierCode", request.getSupplierCode())
                    .addValue("orderAmount", BigDecimal.ZERO)
                    .addValue("orderPrice", BigDecimal.ZERO)
                    .addValue("orderedBy", orderedBy)
                    .addValue("orderDate", toSqlDate(request.getOrderDate()))
                    .addValue("dueDate", toSqlDate(request.getDueDate()))
                    .addValue("deliveryPlace", request.getDeliveryPlace())
                    .addValue("remark", request.getRemark());

            List<String> columns = new ArrayList<>();
            List<String> values = new ArrayList<>();
            columns.add(orderCodeColumn);
            values.add(":mOrderCode");
            if (styleColumn != null) {
                columns.add(styleColumn);
                values.add(":stylesId");
            }
            columns.add(prdColumn);
            values.add(":prdAgreeCode");
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

    private boolean isSupplierAlreadyOrdered(String stylesId, String prdAgreeCode, String colorCode,
            String supplierCode) {
        if (!hasTable("material_orders")) {
            return false;
        }
        String prdColumn = findFirstExistingColumn("material_orders", List.of("prd_agree_code", "agreement_code"));
        String colorColumn = findFirstExistingColumn("material_orders", List.of("color_code"));
        String supplierColumn = findFirstExistingColumn("material_orders", List.of("supplier_code"));
        String styleColumn = findFirstExistingColumn("material_orders", List.of("styles_id", "style_id", "style_no"));

        if (prdColumn == null || colorColumn == null || supplierColumn == null) {
            return false;
        }

        StringBuilder sql = new StringBuilder()
                .append("select count(*) from material_orders ")
                .append("where ").append(prdColumn).append(" = :prdAgreeCode ")
                .append("and ").append(colorColumn).append(" = :colorCode ")
                .append("and ").append(supplierColumn).append(" = :supplierCode ");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("prdAgreeCode", prdAgreeCode)
                .addValue("colorCode", colorCode)
                .addValue("supplierCode", supplierCode);

        if (StringUtils.hasText(stylesId) && styleColumn != null) {
            sql.append("and ").append(styleColumn).append(" = :stylesId ");
            params.addValue("stylesId", stylesId);
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), params, Integer.class);
        return count != null && count > 0;
    }

    private boolean isOrderExists(String stylesId,
            String prdAgreeCode,
            String colorCode,
            String supplierCode,
            Long bomId) {
        if (!hasTable("material_orders")) {
            return false;
        }

        String prdColumn = findFirstExistingColumn("material_orders", List.of("prd_agree_code", "agreement_code"));
        String colorColumn = findFirstExistingColumn("material_orders", List.of("color_code"));
        String supplierColumn = findFirstExistingColumn("material_orders", List.of("supplier_code"));
        String styleColumn = findFirstExistingColumn("material_orders", List.of("styles_id", "style_id", "style_no"));
        String bomIdColumn = findFirstExistingColumn("material_orders", List.of("bom_id"));

        if (prdColumn == null || colorColumn == null || supplierColumn == null || bomIdColumn == null) {
            return false;
        }

        StringBuilder sql = new StringBuilder()
                .append("select count(*) from material_orders ")
                .append("where ").append(prdColumn).append(" = :prdAgreeCode ")
                .append("and ").append(colorColumn).append(" = :colorCode ")
                .append("and ").append(supplierColumn).append(" = :supplierCode ")
                .append("and ").append(bomIdColumn).append(" = :bomId ");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("prdAgreeCode", prdAgreeCode)
                .addValue("colorCode", colorCode)
                .addValue("supplierCode", supplierCode)
                .addValue("bomId", bomId);

        if (StringUtils.hasText(stylesId) && styleColumn != null) {
            sql.append("and ").append(styleColumn).append(" = :stylesId ");
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
        String styleColumn = findFirstExistingColumn("production_agreements",
                List.of("styles_id", "style_id", "style_no", "style_code"));

        if (prdColumn == null || styleColumn == null) {
            return Collections.emptyList();
        }

        String reference = StringUtils.hasText(stylesId) ? stylesId : styleCode;
        if (!StringUtils.hasText(reference)) {
            return Collections.emptyList();
        }

        StringBuilder sql = new StringBuilder()
                .append("select distinct ").append(prdColumn).append(" as prd_agree_code ")
                .append("from production_agreements where ").append(styleColumn).append(" = :styleRef ")
                .append("order by ").append(prdColumn).append(" asc");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("styleRef", reference);

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> rs.getString("prd_agree_code"));
    }

    private List<String> findColorsFromSpecs(String stylesId, String styleCode) {
        if (!hasTable("material_specs")) {
            return Collections.emptyList();
        }

        String colorColumn = findFirstExistingColumn("material_specs", List.of("color_code", "material_color", "color"));
        String styleColumn = findFirstExistingColumn("material_specs", List.of("styles_id", "style_id", "style_no"));
        if (colorColumn == null) {
            return Collections.emptyList();
        }

        StringBuilder sql = new StringBuilder()
                .append("select distinct ").append(colorColumn).append(" as color_code ")
                .append("from material_specs ");

        MapSqlParameterSource params = new MapSqlParameterSource();
        if (StringUtils.hasText(stylesId) && styleColumn != null) {
            sql.append("where ").append(styleColumn).append(" = :stylesId ");
            params.addValue("stylesId", stylesId);
        } else if (StringUtils.hasText(styleCode) && styleColumn != null) {
            sql.append("where ").append(styleColumn).append(" = :stylesId ");
            params.addValue("stylesId", styleCode);
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

        StringBuilder sql = new StringBuilder()
                .append("select ").append(codeColumn).append(" as code_value, ")
                .append(nameColumn != null ? nameColumn : "null").append(" as code_name ")
                .append(codeTypeColumn != null ? ", " + codeTypeColumn + " as code_type " : "")
                .append("from codes ")
                .append("where ").append(codeColumn).append(" in (:codes) ");

        if (activeColumn != null) {
            sql.append("and ").append(activeColumn).append(" = true ");
        }
        if (deletedColumn != null) {
            sql.append("and ").append(deletedColumn).append(" = false ");
        }

        MapSqlParameterSource params = new MapSqlParameterSource("codes", supplierCodes);
        Map<String, String> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql, params, rs -> {
            result.put(rs.getString("code_value"), rs.getString("code_name"));
        });
        return result;
    }

    private String resolveStyleIdColumn() {
        return findFirstExistingColumn("styles", List.of("styles_id", "style_id", "id", "style_no"));
    }

    private String resolveStyleCodeColumn() {
        return findFirstExistingColumn("styles", List.of("style_code", "style_no", "styles_code"));
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

    private String findFirstExistingColumn(String table, List<String> candidates) {
        for (String candidate : candidates) {
            if (hasColumn(table, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String selectOrNull(String column) {
        return selectOrNull(column, column);
    }

    private String selectOrNull(String column, String alias) {
        if (column == null) {
            return "null as " + alias;
        }
        return column + " as " + alias;
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
                    """, new MapSqlParameterSource()
                    .addValue("tableName", tableName)
                    .addValue("columnName", columnName), Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}