package com.example.erp.service;

import com.example.erp.controller.dto.WorkOrderAgreementRow;
import com.example.erp.controller.dto.WorkOrderSaveRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductionWorkOrderService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ProductionWorkOrderService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<WorkOrderAgreementRow> findAgreementRows(String stylesId, String styleCode, String prdAgreeCode,
            String colorCode) {
        if (!StringUtils.hasText(styleCode) || !StringUtils.hasText(prdAgreeCode) || !StringUtils.hasText(colorCode)) {
            return Collections.emptyList();
        }
        if (!hasTable("production_agreements") || !hasTable("styles")) {
            return Collections.emptyList();
        }

        String resolvedStyleId = resolveStyleId(stylesId, styleCode);

        StringBuilder sql = new StringBuilder()
                .append("select s.style_code as style_code, ")
                .append("pa.agreement_code as agreement_code, ")
                .append("pa.color_code as color_code, ")
                .append("coalesce(sum(pa.quantity), 0) as agreement_qty ")
                .append("from production_agreements pa ")
                .append("join styles s on s.styles_id = pa.styles_id ")
                .append("where pa.agreement_code = :agreementCode ")
                .append("and pa.color_code = :colorCode ");

        if (StringUtils.hasText(resolvedStyleId)) {
            sql.append("and pa.styles_id = :stylesId ");
        } else {
            sql.append("and s.style_code = :styleCode ");
        }

        sql.append("group by s.style_code, pa.agreement_code, pa.color_code ")
                .append("order by s.style_code asc, pa.agreement_code asc, pa.color_code asc");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("agreementCode", prdAgreeCode)
                .addValue("colorCode", colorCode)
                .addValue("stylesId", resolvedStyleId)
                .addValue("styleCode", styleCode);

        List<WorkOrderAgreementRow> rows = jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            WorkOrderAgreementRow row = new WorkOrderAgreementRow();
            row.setStyleCode(rs.getString("style_code"));
            row.setPrdAgreeCode(rs.getString("agreement_code"));
            row.setColorCode(rs.getString("color_code"));
            row.setAgreementQuantity(rs.getBigDecimal("agreement_qty"));
            return row;
        });

        if (rows.isEmpty() || !hasTable("fg_orders")) {
            return rows;
        }

        Map<String, ExistingOrder> existingOrders = loadExistingOrders(prdAgreeCode, colorCode, styleCode,
                resolvedStyleId);
        Set<String> producerCodes = existingOrders.values().stream()
                .map(ExistingOrder::producerCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Set<String> deliveryCodes = existingOrders.values().stream()
                .map(ExistingOrder::deliveryPlaceCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        Map<String, String> producerNames = findCodeNames("CUSTOMER", producerCodes);
        Map<String, String> deliveryNames = findCodeNames("WAREHOUSE", deliveryCodes);

        for (WorkOrderAgreementRow row : rows) {
            String key = buildKey(row.getStyleCode(), row.getPrdAgreeCode(), row.getColorCode());
            ExistingOrder existing = existingOrders.get(key);
            if (existing != null) {
                row.setOrdered(true);
                row.setProducerCode(existing.producerCode());
                row.setProducerName(producerNames.get(existing.producerCode()));
                row.setDueDate(existing.dueDate());
                row.setDeliveryPlaceCode(existing.deliveryPlaceCode());
                row.setDeliveryPlaceName(deliveryNames.get(existing.deliveryPlaceCode()));
            }
        }

        return rows;
    }

    @Transactional
    public Map<String, Object> saveWorkOrder(WorkOrderSaveRequest request, String empNo) {
        if (!hasTable("fg_orders")) {
            return Map.of("success", false, "message", "작업지시 테이블이 없습니다.");
        }
        if (!StringUtils.hasText(request.getStyleCode()) || !StringUtils.hasText(request.getPrdAgreeCode())
                || !StringUtils.hasText(request.getColorCode())) {
            return Map.of("success", false, "message", "작업지시 대상이 없습니다.");
        }
        if (!StringUtils.hasText(request.getProducerCode()) || !StringUtils.hasText(request.getDueDate())
                || !StringUtils.hasText(request.getDeliveryPlaceCode())) {
            return Map.of("success", false, "message", "생산처/납기일/납품장소를 입력하세요.");
        }

        String agreementColumn = findFirstExistingColumn("fg_orders", List.of("agreement_code", "prd_agree_code"));
        String colorColumn = findFirstExistingColumn("fg_orders", List.of("color_code"));
        String styleColumn = findFirstExistingColumn("fg_orders", List.of("style_code", "styles_code"));
        String styleIdColumn = findFirstExistingColumn("fg_orders", List.of("styles_id", "style_id"));
        String qtyColumn = findFirstExistingColumn("fg_orders", List.of("order_qty", "quantity", "agree_qty"));
        String producerColumn = findFirstExistingColumn("fg_orders", List.of("producer_code", "factory_code"));
        String dueDateColumn = findFirstExistingColumn("fg_orders", List.of("due_date", "delivery_due_date",
                "delivery_date"));
        String deliveryColumn = findFirstExistingColumn("fg_orders", List.of("delivery_place", "warehouse_code",
                "delivery_place_code"));
        String createdByColumn = findFirstExistingColumn("fg_orders", List.of("created_by", "ordered_by"));

        if (agreementColumn == null || colorColumn == null || producerColumn == null || dueDateColumn == null
                || deliveryColumn == null) {
            return Map.of("success", false, "message", "작업지시 컬럼이 부족합니다.");
        }

        String resolvedStyleId = resolveStyleId(null, request.getStyleCode());
        if (isOrderExists(agreementColumn, colorColumn, styleColumn, styleIdColumn, request.getPrdAgreeCode(),
                request.getColorCode(), request.getStyleCode(), resolvedStyleId)) {
            return Map.of("success", false, "message", "이미 작업지시가 등록되었습니다.");
        }

        BigDecimal agreementQty = findAgreementQuantity(request.getPrdAgreeCode(), request.getColorCode(),
                request.getStyleCode(), resolvedStyleId);
        LocalDateTime dueDate = parseDateTime(request.getDueDate());

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("styleCode", request.getStyleCode())
                .addValue("stylesId", resolvedStyleId)
                .addValue("agreementCode", request.getPrdAgreeCode())
                .addValue("colorCode", request.getColorCode())
                .addValue("agreementQty", agreementQty)
                .addValue("producerCode", request.getProducerCode())
                .addValue("dueDate", dueDate != null ? Timestamp.valueOf(dueDate) : null)
                .addValue("deliveryPlace", request.getDeliveryPlaceCode())
                .addValue("createdBy", empNo);

        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();
        if (styleColumn != null) {
            columns.add(styleColumn);
            values.add(":styleCode");
        }
        if (styleIdColumn != null && StringUtils.hasText(resolvedStyleId)) {
            columns.add(styleIdColumn);
            values.add(":stylesId");
        }
        columns.add(agreementColumn);
        values.add(":agreementCode");
        columns.add(colorColumn);
        values.add(":colorCode");
        if (qtyColumn != null) {
            columns.add(qtyColumn);
            values.add(":agreementQty");
        }
        columns.add(producerColumn);
        values.add(":producerCode");
        columns.add(dueDateColumn);
        values.add(":dueDate");
        columns.add(deliveryColumn);
        values.add(":deliveryPlace");
        if (createdByColumn != null) {
            columns.add(createdByColumn);
            values.add(":createdBy");
        }

        String sql = "insert into fg_orders (" + String.join(", ", columns) + ") values ("
                + String.join(", ", values) + ")";

        int affected = jdbcTemplate.update(sql, params);
        return Map.of("success", affected > 0, "message", affected > 0 ? "작업지시가 등록되었습니다." : "작업지시 등록에 실패했습니다.");
    }

    private Map<String, ExistingOrder> loadExistingOrders(String prdAgreeCode, String colorCode, String styleCode,
            String stylesId) {
        String agreementColumn = findFirstExistingColumn("fg_orders", List.of("agreement_code", "prd_agree_code"));
        String colorColumn = findFirstExistingColumn("fg_orders", List.of("color_code"));
        String styleColumn = findFirstExistingColumn("fg_orders", List.of("style_code", "styles_code"));
        String styleIdColumn = findFirstExistingColumn("fg_orders", List.of("styles_id", "style_id"));
        String producerColumn = findFirstExistingColumn("fg_orders", List.of("producer_code", "factory_code"));
        String dueDateColumn = findFirstExistingColumn("fg_orders", List.of("due_date", "delivery_due_date",
                "delivery_date"));
        String deliveryColumn = findFirstExistingColumn("fg_orders", List.of("delivery_place", "warehouse_code",
                "delivery_place_code"));

        if (agreementColumn == null || colorColumn == null) {
            return Collections.emptyMap();
        }

        StringBuilder sql = new StringBuilder()
                .append("select ")
                .append(styleColumn != null ? styleColumn + " as style_code, " : "null as style_code, ")
                .append(agreementColumn).append(" as agreement_code, ")
                .append(colorColumn).append(" as color_code ")
                .append(producerColumn != null ? ", " + producerColumn + " as producer_code" : ", null as producer_code")
                .append(dueDateColumn != null ? ", " + dueDateColumn + " as due_date" : ", null as due_date")
                .append(deliveryColumn != null ? ", " + deliveryColumn + " as delivery_place" : ", null as delivery_place")
                .append(" from fg_orders ")
                .append("where ").append(agreementColumn).append(" = :agreementCode ")
                .append("and ").append(colorColumn).append(" = :colorCode ");

        if (styleColumn != null) {
            sql.append("and ").append(styleColumn).append(" = :styleCode ");
        } else if (styleIdColumn != null && StringUtils.hasText(stylesId)) {
            sql.append("and ").append(styleIdColumn).append(" = :stylesId ");
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("agreementCode", prdAgreeCode)
                .addValue("colorCode", colorCode)
                .addValue("styleCode", styleCode)
                .addValue("stylesId", stylesId);

        Map<String, ExistingOrder> result = new HashMap<>();
        jdbcTemplate.query(sql.toString(), params, rs -> {
            String sCode = rs.getString("style_code");
            String agreement = rs.getString("agreement_code");
            String color = rs.getString("color_code");
            String key = buildKey(sCode != null ? sCode : styleCode, agreement, color);
            String producerCode = rs.getString("producer_code");
            String deliveryPlace = rs.getString("delivery_place");
            String dueDate = formatDateTime(rs.getObject("due_date"));
            result.put(key, new ExistingOrder(producerCode, dueDate, deliveryPlace));
        });
        return result;
    }

    private boolean isOrderExists(String agreementColumn, String colorColumn, String styleColumn,
            String styleIdColumn, String agreementCode, String colorCode, String styleCode, String stylesId) {
        StringBuilder sql = new StringBuilder()
                .append("select count(*) from fg_orders where ")
                .append(agreementColumn).append(" = :agreementCode ")
                .append("and ").append(colorColumn).append(" = :colorCode ");
        if (styleColumn != null) {
            sql.append("and ").append(styleColumn).append(" = :styleCode ");
        } else if (styleIdColumn != null && StringUtils.hasText(stylesId)) {
            sql.append("and ").append(styleIdColumn).append(" = :stylesId ");
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("agreementCode", agreementCode)
                .addValue("colorCode", colorCode)
                .addValue("styleCode", styleCode)
                .addValue("stylesId", stylesId);

        Integer count = jdbcTemplate.queryForObject(sql.toString(), params, Integer.class);
        return count != null && count > 0;
    }

    private BigDecimal findAgreementQuantity(String agreementCode, String colorCode, String styleCode,
            String stylesId) {
        if (!hasTable("production_agreements")) {
            return BigDecimal.ZERO;
        }
        StringBuilder sql = new StringBuilder()
                .append("select coalesce(sum(quantity), 0) from production_agreements ")
                .append("where agreement_code = :agreementCode ")
                .append("and color_code = :colorCode ");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("agreementCode", agreementCode)
                .addValue("colorCode", colorCode);

        if (StringUtils.hasText(stylesId)) {
            sql.append("and styles_id = :stylesId ");
            params.addValue("stylesId", stylesId);
        } else if (StringUtils.hasText(styleCode) && hasColumn("production_agreements", "style_code")) {
            sql.append("and style_code = :styleCode ");
            params.addValue("styleCode", styleCode);
        }

        BigDecimal result = jdbcTemplate.queryForObject(sql.toString(), params, BigDecimal.class);
        return result != null ? result : BigDecimal.ZERO;
    }

    private Map<String, String> findCodeNames(String codeType, Set<String> codes) {
        if (!StringUtils.hasText(codeType) || codes == null || codes.isEmpty() || !hasTable("codes")) {
            return Collections.emptyMap();
        }
        String codeColumn = findFirstExistingColumn("codes", List.of("code"));
        String codeTypeColumn = findFirstExistingColumn("codes", List.of("code_type"));
        String nameColumn = findFirstExistingColumn("codes", List.of("code_name", "name"));
        if (codeColumn == null || codeTypeColumn == null) {
            return Collections.emptyMap();
        }

        String sql = "select " + codeColumn + " as code_value, "
                + (nameColumn != null ? nameColumn : "null") + " as code_name "
                + "from codes where " + codeTypeColumn + " = :codeType and " + codeColumn + " in (:codes)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("codeType", codeType)
                .addValue("codes", codes);

        return jdbcTemplate.query(sql, params, rs -> {
            Map<String, String> result = new LinkedHashMap<>();
            while (rs.next()) {
                result.put(rs.getString("code_value"), rs.getString("code_name"));
            }
            return result;
        });
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

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATETIME_FORMATTER.withLocale(Locale.getDefault()));
        } catch (Exception ex) {
            return null;
        }
    }

    private String formatDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().format(DATETIME_FORMATTER);
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().atStartOfDay().format(DATETIME_FORMATTER);
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(DATETIME_FORMATTER);
        }
        return null;
    }

    private String buildKey(String styleCode, String agreementCode, String colorCode) {
        return String.join("|", String.valueOf(styleCode), String.valueOf(agreementCode), String.valueOf(colorCode));
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

    private record ExistingOrder(String producerCode, String dueDate, String deliveryPlaceCode) {
    }
}