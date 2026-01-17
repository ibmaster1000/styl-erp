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
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ACTIVE = "ACTIVE";

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

        String prdAgreeIdColumn = findFirstExistingColumn("production_agreements", List.of("prd_agree_id",
                "prd_agree_no", "prd_agree_seq"));

        StringBuilder sql = new StringBuilder()
                .append("select s.style_code as style_code, ")
                .append("pa.agreement_code as agreement_code, ")
                .append("pa.color_code as color_code, ")
                .append("coalesce(sum(pa.quantity), 0) as agreement_qty ");
        if (prdAgreeIdColumn != null) {
            sql.append(", max(pa.").append(prdAgreeIdColumn).append(") as prd_agree_id ");
        }
        sql.append(" ")
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
            if (prdAgreeIdColumn != null) {
                Object prdAgreeValue = rs.getObject("prd_agree_id");
                if (prdAgreeValue != null) {
                    row.setPrdAgreeId(((Number) prdAgreeValue).longValue());
                }
            }
            return row;
        });

        if (rows.isEmpty() || !hasTable("production_jobs")) {
            return rows;
        }

        Map<String, ExistingJob> existingOrders = loadExistingJobs(prdAgreeCode, colorCode, styleCode,
                resolvedStyleId);
        Set<String> producerCodes = existingOrders.values().stream()
                .map(ExistingJob::producerCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Set<String> deliveryCodes = existingOrders.values().stream()
                .map(ExistingJob::deliveryPlaceCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        Map<String, String> producerNames = findCodeNames("CUSTOMER", producerCodes);
        Map<String, String> deliveryNames = findCodeNames("WAREHOUSE", deliveryCodes);

        for (WorkOrderAgreementRow row : rows) {
            String key = buildKey(row.getStyleCode(), row.getPrdAgreeCode(), row.getColorCode());
            ExistingJob existing = existingOrders.get(key);
            if (existing != null) {
                row.setStatus(existing.status());
                if (STATUS_ACTIVE.equalsIgnoreCase(existing.status())) {
                    row.setOrdered(true);
                }
                row.setProducerCode(existing.producerCode());
                row.setProducerName(producerNames.get(existing.producerCode()));
                row.setDueDate(existing.dueDate());
                row.setDeliveryPlaceCode(existing.deliveryPlaceCode());
                row.setDeliveryPlaceName(deliveryNames.get(existing.deliveryPlaceCode()));
            }
        }

        return rows;
    }
    
    public Map<String, Object> findAgreementHeaderPage(String styleCode, String prdAgreeCode, int page, int size) {
        if (!hasTable("production_agreements") || !hasTable("styles")) {
            return Map.of("items", Collections.emptyList(), "page", 1, "size", size, "total", 0, "totalPages", 0);
        }

        String agreementColumn = findFirstExistingColumn("production_agreements",
                List.of("agreement_code", "prd_agree_code", "prd_agree_no"));
        String colorColumn = findFirstExistingColumn("production_agreements", List.of("color_code", "color"));
        String qtyColumn = findFirstExistingColumn("production_agreements", List.of("quantity", "agree_qty", "order_qty"));
        String prdAgreeIdColumn = findFirstExistingColumn("production_agreements", List.of("prd_agree_id",
                "prd_agree_no", "prd_agree_seq"));
        String stylesIdColumn = findFirstExistingColumn("production_agreements", List.of("styles_id", "style_id"));
        String stylesIdColumnInStyles = findFirstExistingColumn("styles", List.of("styles_id", "style_id", "id"));
        String styleCodeColumn = findFirstExistingColumn("styles", List.of("style_code", "styles_code"));

        if (agreementColumn == null || colorColumn == null || qtyColumn == null || prdAgreeIdColumn == null
                || stylesIdColumn == null || stylesIdColumnInStyles == null || styleCodeColumn == null) {
            return Map.of("items", Collections.emptyList(), "page", 1, "size", size, "total", 0, "totalPages", 0);
        }

        int safeSize = size > 0 ? size : 25;
        int safePage = Math.max(page, 1);
        int offset = (safePage - 1) * safeSize;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("size", safeSize)
                .addValue("offset", offset);

        StringBuilder countSql = new StringBuilder()
                .append("select count(*) from (")
                .append("select s.").append(styleCodeColumn).append(" as style_code, ")
                .append("pa.").append(agreementColumn).append(" as agreement_code ")
                .append("from production_agreements pa ")
                .append("join styles s on s.").append(stylesIdColumnInStyles)
                .append(" = pa.").append(stylesIdColumn).append(" ");
        appendHeaderFilters(countSql, params, "s", styleCodeColumn, "pa", agreementColumn, styleCode, prdAgreeCode);
        countSql.append("group by s.").append(styleCodeColumn).append(", pa.").append(agreementColumn).append(") t");

        Integer totalCount = jdbcTemplate.queryForObject(countSql.toString(), params, Integer.class);
        int total = totalCount != null ? totalCount : 0;
        int totalPages = safeSize > 0 ? (int) Math.ceil(total / (double) safeSize) : 0;

        StringBuilder latestSql = new StringBuilder()
                .append("select pa_latest.").append(stylesIdColumn).append(" as styles_id, ")
                .append("pa_latest.").append(agreementColumn).append(" as agreement_code, ")
                .append("pa_latest.").append(colorColumn).append(" as color_code, ")
                .append("pa_latest.").append(prdAgreeIdColumn).append(" as prd_agree_id ")
                .append("from production_agreements pa_latest ")
                .append("join styles s_latest on s_latest.").append(stylesIdColumnInStyles)
                .append(" = pa_latest.").append(stylesIdColumn).append(" ");
        appendHeaderFilters(latestSql, params, "s_latest", styleCodeColumn, "pa_latest", agreementColumn, styleCode,
                prdAgreeCode);
        latestSql.append("and pa_latest.").append(prdAgreeIdColumn).append(" = (")
                .append("select max(pa_inner.").append(prdAgreeIdColumn).append(") from production_agreements pa_inner ")
                .append("where pa_inner.").append(stylesIdColumn).append(" = pa_latest.")
                .append(stylesIdColumn).append(" ")
                .append("and pa_inner.").append(agreementColumn).append(" = pa_latest.")
                .append(agreementColumn).append(")");

        StringBuilder sql = new StringBuilder()
                .append("select s.").append(styleCodeColumn).append(" as style_code, ")
                .append("pa.").append(agreementColumn).append(" as agreement_code, ")
                .append("coalesce(sum(pa.").append(qtyColumn).append("), 0) as agreement_qty, ")
                .append("latest.color_code as color_code, ")
                .append("max(pa.").append(prdAgreeIdColumn).append(") as prd_agree_id ")
                .append("from production_agreements pa ")
                .append("join styles s on s.").append(stylesIdColumnInStyles)
                .append(" = pa.").append(stylesIdColumn).append(" ")
                .append("join (").append(latestSql).append(") latest on latest.styles_id = pa.")
                .append(stylesIdColumn).append(" and latest.agreement_code = pa.").append(agreementColumn).append(" ");
        appendHeaderFilters(sql, params, "s", styleCodeColumn, "pa", agreementColumn, styleCode, prdAgreeCode);
        sql.append("group by s.").append(styleCodeColumn).append(", pa.").append(agreementColumn)
                .append(", latest.color_code ")
                .append("order by max(pa.").append(prdAgreeIdColumn).append(") desc ")
                .append("limit :size offset :offset");

        List<WorkOrderAgreementRow> items = jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            WorkOrderAgreementRow row = new WorkOrderAgreementRow();
            row.setStyleCode(rs.getString("style_code"));
            row.setPrdAgreeCode(rs.getString("agreement_code"));
            row.setColorCode(rs.getString("color_code"));
            row.setAgreementQuantity(rs.getBigDecimal("agreement_qty"));
            Object prdAgreeValue = rs.getObject("prd_agree_id");
            if (prdAgreeValue != null) {
                row.setPrdAgreeId(((Number) prdAgreeValue).longValue());
            }
            return row;
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("total", total);
        result.put("totalPages", totalPages);
        return result;
    }

    public List<WorkOrderAgreementRow> findAgreementRowsByAgreement(String stylesId, String styleCode,
            String prdAgreeCode) {
        if (!StringUtils.hasText(prdAgreeCode)) {
            return Collections.emptyList();
        }
        if (!hasTable("production_agreements") || !hasTable("styles")) {
            return Collections.emptyList();
        }

        String resolvedStyleId = resolveStyleId(stylesId, styleCode);

        String prdAgreeIdColumn = findFirstExistingColumn("production_agreements", List.of("prd_agree_id",
                "prd_agree_no", "prd_agree_seq"));

        StringBuilder sql = new StringBuilder()
                .append("select s.style_code as style_code, ")
                .append("pa.agreement_code as agreement_code, ")
                .append("pa.color_code as color_code, ")
                .append("coalesce(sum(pa.quantity), 0) as agreement_qty ");
        if (prdAgreeIdColumn != null) {
            sql.append(", max(pa.").append(prdAgreeIdColumn).append(") as prd_agree_id ");
        }
        sql.append(" ")
                .append("from production_agreements pa ")
                .append("join styles s on s.styles_id = pa.styles_id ")
                .append("where pa.agreement_code = :agreementCode ");

        if (StringUtils.hasText(resolvedStyleId)) {
            sql.append("and pa.styles_id = :stylesId ");
        } else if (StringUtils.hasText(styleCode)) {
            sql.append("and s.style_code = :styleCode ");
        }

        sql.append("group by s.style_code, pa.agreement_code, pa.color_code ")
                .append("order by s.style_code asc, pa.agreement_code asc, pa.color_code asc");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("agreementCode", prdAgreeCode)
                .addValue("stylesId", resolvedStyleId)
                .addValue("styleCode", styleCode);

        List<WorkOrderAgreementRow> rows = jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            WorkOrderAgreementRow row = new WorkOrderAgreementRow();
            row.setStyleCode(rs.getString("style_code"));
            row.setPrdAgreeCode(rs.getString("agreement_code"));
            row.setColorCode(rs.getString("color_code"));
            row.setAgreementQuantity(rs.getBigDecimal("agreement_qty"));
            if (prdAgreeIdColumn != null) {
                Object prdAgreeValue = rs.getObject("prd_agree_id");
                if (prdAgreeValue != null) {
                    row.setPrdAgreeId(((Number) prdAgreeValue).longValue());
                }
            }
            return row;
        });

        if (rows.isEmpty() || !hasTable("production_jobs")) {
            return rows;
        }

        Map<String, ExistingJob> existingOrders = loadExistingJobsByAgreement(prdAgreeCode, styleCode, resolvedStyleId);
        Set<String> producerCodes = existingOrders.values().stream()
                .map(ExistingJob::producerCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Set<String> deliveryCodes = existingOrders.values().stream()
                .map(ExistingJob::deliveryPlaceCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        Map<String, String> producerNames = findCodeNames("CUSTOMER", producerCodes);
        Map<String, String> deliveryNames = findCodeNames("WAREHOUSE", deliveryCodes);

        for (WorkOrderAgreementRow row : rows) {
            String key = buildKey(row.getStyleCode(), row.getPrdAgreeCode(), row.getColorCode());
            ExistingJob existing = existingOrders.get(key);
            if (existing != null) {
                row.setStatus(existing.status());
                if (STATUS_ACTIVE.equalsIgnoreCase(existing.status())) {
                    row.setOrdered(true);
                }
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
    public Map<String, Object> saveDraftWorkOrder(WorkOrderSaveRequest request, String empNo) {
        if (!hasTable("production_jobs")) {
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

        String agreementColumn = findFirstExistingColumn("production_jobs", List.of("agreement_code", "prd_agree_code"));
        String colorColumn = findFirstExistingColumn("production_jobs", List.of("color_code"));
        String styleColumn = findFirstExistingColumn("production_jobs", List.of("style_code", "styles_code"));
        String styleIdColumn = findFirstExistingColumn("production_jobs", List.of("styles_id", "style_id"));
        String qtyColumn = findFirstExistingColumn("production_jobs", List.of("quantity", "order_qty", "agree_qty"));
        String producerColumn = findFirstExistingColumn("production_jobs", List.of("factory_code", "producer_code"));
        String dueDateColumn = findFirstExistingColumn("production_jobs", List.of("due_date", "delivery_due_date",
                "delivery_date"));
        String deliveryColumn = findFirstExistingColumn("production_jobs", List.of("delivery_location",
                "delivery_place_code", "delivery_place", "warehouse_code"));
        String statusColumn = findFirstExistingColumn("production_jobs", List.of("status"));
        String prdAgreeIdColumn = findFirstExistingColumn("production_jobs", List.of("prd_agree_id"));

        if (agreementColumn == null || colorColumn == null || producerColumn == null || dueDateColumn == null
                || deliveryColumn == null || statusColumn == null) {
            return Map.of("success", false, "message", "작업지시 컬럼이 부족합니다.");
        }

        String resolvedStyleId = resolveStyleId(null, request.getStyleCode());
        String existingStatus = findExistingJobStatus(agreementColumn, colorColumn, styleColumn, styleIdColumn,
                statusColumn, request.getPrdAgreeCode(), request.getColorCode(), request.getStyleCode(), resolvedStyleId);
        if (STATUS_ACTIVE.equalsIgnoreCase(existingStatus)) {
            return Map.of("success", false, "message", "이미 작업지시가 활성화되었습니다.");
        }

        BigDecimal agreementQty = request.getAgreementQuantity();
        if (agreementQty == null) {
            agreementQty = findAgreementQuantity(request.getPrdAgreeCode(), request.getColorCode(),
                    request.getStyleCode(), resolvedStyleId);
        }
        LocalDateTime dueDate = parseDateTime(request.getDueDate());
        Long prdAgreeId = request.getPrdAgreeId();
        if (prdAgreeId == null) {
            prdAgreeId = resolvePrdAgreeId(request.getPrdAgreeCode(), request.getColorCode());
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("styleCode", request.getStyleCode())
                .addValue("stylesId", resolvedStyleId)
                .addValue("agreementCode", request.getPrdAgreeCode())
                .addValue("colorCode", request.getColorCode())
                .addValue("agreementQty", agreementQty)
                .addValue("producerCode", request.getProducerCode())
                .addValue("dueDate", dueDate != null ? Timestamp.valueOf(dueDate) : null)
                .addValue("deliveryPlace", request.getDeliveryPlaceCode())
                .addValue("status", STATUS_DRAFT)
                .addValue("prdAgreeId", prdAgreeId)
                .addValue("updatedBy", empNo)
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
        columns.add(statusColumn);
        values.add(":status");
        if (prdAgreeIdColumn != null) {
            columns.add(prdAgreeIdColumn);
            values.add(":prdAgreeId");
        }

        if (StringUtils.hasText(existingStatus)) {
            List<String> updates = new ArrayList<>();
            if (qtyColumn != null) {
                updates.add(qtyColumn + " = :agreementQty");
            }
            updates.add(producerColumn + " = :producerCode");
            updates.add(dueDateColumn + " = :dueDate");
            updates.add(deliveryColumn + " = :deliveryPlace");
            updates.add(statusColumn + " = :status");
            if (prdAgreeIdColumn != null) {
                updates.add(prdAgreeIdColumn + " = :prdAgreeId");
            }

            StringBuilder updateSql = new StringBuilder()
                    .append("update production_jobs set ")
                    .append(String.join(", ", updates))
                    .append(" where ").append(agreementColumn).append(" = :agreementCode ")
                    .append("and ").append(colorColumn).append(" = :colorCode ");
            if (styleColumn != null) {
                updateSql.append("and ").append(styleColumn).append(" = :styleCode ");
            } else if (styleIdColumn != null && StringUtils.hasText(resolvedStyleId)) {
                updateSql.append("and ").append(styleIdColumn).append(" = :stylesId ");
            }

            int affected = jdbcTemplate.update(updateSql.toString(), params);
            return Map.of("success", affected > 0, "message", affected > 0 ? "작업지시 기본정보가 저장되었습니다." : "작업지시 저장에 실패했습니다.");
        }

        String sql = "insert into production_jobs (" + String.join(", ", columns) + ") values ("
                + String.join(", ", values) + ")";

        int affected = jdbcTemplate.update(sql, params);
        return Map.of("success", affected > 0,
                "message", affected > 0 ? "작업지시 기본정보가 저장되었습니다." : "작업지시 저장에 실패했습니다.");
    }

    @Transactional
    public Map<String, Object> activateWorkOrder(WorkOrderSaveRequest request, String empNo) {
        if (!hasTable("production_jobs")) {
            return Map.of("success", false, "message", "작업지시 테이블이 없습니다.");
        }
        if (!StringUtils.hasText(request.getStyleCode()) || !StringUtils.hasText(request.getPrdAgreeCode())
                || !StringUtils.hasText(request.getColorCode())) {
            return Map.of("success", false, "message", "작업지시 대상이 없습니다.");
        }

        String agreementColumn = findFirstExistingColumn("production_jobs", List.of("agreement_code", "prd_agree_code"));
        String colorColumn = findFirstExistingColumn("production_jobs", List.of("color_code"));
        String styleColumn = findFirstExistingColumn("production_jobs", List.of("style_code", "styles_code"));
        String styleIdColumn = findFirstExistingColumn("production_jobs", List.of("styles_id", "style_id"));
        String statusColumn = findFirstExistingColumn("production_jobs", List.of("status"));

        if (agreementColumn == null || colorColumn == null || statusColumn == null) {
            return Map.of("success", false, "message", "작업지시 컬럼이 부족합니다.");
        }

        String resolvedStyleId = resolveStyleId(null, request.getStyleCode());
        String existingStatus = findExistingJobStatus(agreementColumn, colorColumn, styleColumn, styleIdColumn,
                statusColumn, request.getPrdAgreeCode(), request.getColorCode(), request.getStyleCode(), resolvedStyleId);
        if (!STATUS_DRAFT.equalsIgnoreCase(existingStatus)) {
            return Map.of("success", false, "message", "먼저 저장 버튼으로 기본 정보를 저장하세요.");
        }

        StringBuilder sql = new StringBuilder()
                .append("update production_jobs set ").append(statusColumn).append(" = :status ")
                .append("where ").append(agreementColumn).append(" = :agreementCode ")
                .append("and ").append(colorColumn).append(" = :colorCode ");
        if (styleColumn != null) {
            sql.append("and ").append(styleColumn).append(" = :styleCode ");
        } else if (styleIdColumn != null && StringUtils.hasText(resolvedStyleId)) {
            sql.append("and ").append(styleIdColumn).append(" = :stylesId ");
        }
        sql.append("and ").append(statusColumn).append(" = :draftStatus");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", STATUS_ACTIVE)
                .addValue("draftStatus", STATUS_DRAFT)
                .addValue("agreementCode", request.getPrdAgreeCode())
                .addValue("colorCode", request.getColorCode())
                .addValue("styleCode", request.getStyleCode())
                .addValue("stylesId", resolvedStyleId);

        int affected = jdbcTemplate.update(sql.toString(), params);
        return Map.of("success", affected > 0,
                "message", affected > 0 ? "작업지시가 활성화되었습니다." : "작업지시 활성화에 실패했습니다.");
    }

    private Map<String, ExistingJob> loadExistingJobs(String prdAgreeCode, String colorCode, String styleCode,
            String stylesId) {
        String agreementColumn = findFirstExistingColumn("production_jobs", List.of("agreement_code", "prd_agree_code"));
        String colorColumn = findFirstExistingColumn("production_jobs", List.of("color_code"));
        String styleColumn = findFirstExistingColumn("production_jobs", List.of("style_code", "styles_code"));
        String styleIdColumn = findFirstExistingColumn("production_jobs", List.of("styles_id", "style_id"));
        String producerColumn = findFirstExistingColumn("production_jobs", List.of("factory_code", "producer_code"));
        String dueDateColumn = findFirstExistingColumn("production_jobs", List.of("due_date", "delivery_due_date",
                "delivery_date"));
        String deliveryColumn = findFirstExistingColumn("production_jobs", List.of("delivery_location",
                "delivery_place_code", "delivery_place", "warehouse_code"));
        String statusColumn = findFirstExistingColumn("production_jobs", List.of("status"));

        if (agreementColumn == null || colorColumn == null) {
            return Collections.emptyMap();
        }

        StringBuilder sql = new StringBuilder()
                .append("select ")
                .append(styleColumn != null ? styleColumn + " as style_code, " : "null as style_code, ")
                .append(agreementColumn).append(" as agreement_code, ")
                .append(colorColumn).append(" as color_code ")
                .append(statusColumn != null ? ", " + statusColumn + " as status" : ", null as status")
                .append(producerColumn != null ? ", " + producerColumn + " as producer_code" : ", null as producer_code")
                .append(dueDateColumn != null ? ", " + dueDateColumn + " as due_date" : ", null as due_date")
                .append(deliveryColumn != null ? ", " + deliveryColumn + " as delivery_place" : ", null as delivery_place")
                .append(" from production_jobs ")
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

        Map<String, ExistingJob> result = new HashMap<>();
        jdbcTemplate.query(sql.toString(), params, rs -> {
            String sCode = rs.getString("style_code");
            String agreement = rs.getString("agreement_code");
            String color = rs.getString("color_code");
            String key = buildKey(sCode != null ? sCode : styleCode, agreement, color);
            String producerCode = rs.getString("producer_code");
            String deliveryPlace = rs.getString("delivery_place");
            String dueDate = formatDateTime(rs.getObject("due_date"));
            String status = rs.getString("status");
            result.put(key, new ExistingJob(producerCode, dueDate, deliveryPlace, status));
        });
        return result;
    }
    
    private Map<String, ExistingJob> loadExistingJobsByAgreement(String prdAgreeCode, String styleCode,
            String stylesId) {
        String agreementColumn = findFirstExistingColumn("production_jobs", List.of("agreement_code", "prd_agree_code"));
        String colorColumn = findFirstExistingColumn("production_jobs", List.of("color_code"));
        String styleColumn = findFirstExistingColumn("production_jobs", List.of("style_code", "styles_code"));
        String styleIdColumn = findFirstExistingColumn("production_jobs", List.of("styles_id", "style_id"));
        String producerColumn = findFirstExistingColumn("production_jobs", List.of("factory_code", "producer_code"));
        String dueDateColumn = findFirstExistingColumn("production_jobs", List.of("due_date", "delivery_due_date",
                "delivery_date"));
        String deliveryColumn = findFirstExistingColumn("production_jobs", List.of("delivery_location",
                "delivery_place_code", "delivery_place", "warehouse_code"));
        String statusColumn = findFirstExistingColumn("production_jobs", List.of("status"));

        if (agreementColumn == null || colorColumn == null) {
            return Collections.emptyMap();
        }

        StringBuilder sql = new StringBuilder()
                .append("select ")
                .append(styleColumn != null ? styleColumn + " as style_code, " : "null as style_code, ")
                .append(agreementColumn).append(" as agreement_code, ")
                .append(colorColumn).append(" as color_code ")
                .append(statusColumn != null ? ", " + statusColumn + " as status" : ", null as status")
                .append(producerColumn != null ? ", " + producerColumn + " as producer_code" : ", null as producer_code")
                .append(dueDateColumn != null ? ", " + dueDateColumn + " as due_date" : ", null as due_date")
                .append(deliveryColumn != null ? ", " + deliveryColumn + " as delivery_place" : ", null as delivery_place")
                .append(" from production_jobs ")
                .append("where ").append(agreementColumn).append(" = :agreementCode ");

        if (styleColumn != null) {
            sql.append("and ").append(styleColumn).append(" = :styleCode ");
        } else if (styleIdColumn != null && StringUtils.hasText(stylesId)) {
            sql.append("and ").append(styleIdColumn).append(" = :stylesId ");
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("agreementCode", prdAgreeCode)
                .addValue("styleCode", styleCode)
                .addValue("stylesId", stylesId);

        Map<String, ExistingJob> result = new HashMap<>();
        jdbcTemplate.query(sql.toString(), params, rs -> {
            String sCode = rs.getString("style_code");
            String agreement = rs.getString("agreement_code");
            String color = rs.getString("color_code");
            String key = buildKey(sCode != null ? sCode : styleCode, agreement, color);
            String producerCode = rs.getString("producer_code");
            String deliveryPlace = rs.getString("delivery_place");
            String dueDate = formatDateTime(rs.getObject("due_date"));
            String status = rs.getString("status");
            result.put(key, new ExistingJob(producerCode, dueDate, deliveryPlace, status));
        });
        return result;
    }

    private void appendHeaderFilters(StringBuilder sql, MapSqlParameterSource params, String styleAlias,
            String styleCodeColumn, String agreementAlias, String agreementColumn, String styleCode,
            String prdAgreeCode) {
        sql.append("where 1=1 ");
        if (StringUtils.hasText(styleCode)) {
            sql.append("and ").append(styleAlias).append(".").append(styleCodeColumn).append(" like :styleCode ");
            if (!params.hasValue("styleCode")) {
                params.addValue("styleCode", "%" + styleCode + "%");
            }
        }
        if (StringUtils.hasText(prdAgreeCode)) {
            sql.append("and ").append(agreementAlias).append(".").append(agreementColumn)
                    .append(" like :prdAgreeCode ");
            if (!params.hasValue("prdAgreeCode")) {
                params.addValue("prdAgreeCode", "%" + prdAgreeCode + "%");
            }
        }
    }

    private String findExistingJobStatus(String agreementColumn, String colorColumn, String styleColumn,
            String styleIdColumn, String statusColumn, String agreementCode, String colorCode, String styleCode,
            String stylesId) {
        StringBuilder sql = new StringBuilder()
                .append("select ").append(statusColumn).append(" as status from production_jobs where ")
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

        List<String> result = jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> rs.getString("status"));
        if (result.stream().anyMatch(status -> STATUS_ACTIVE.equalsIgnoreCase(status))) {
            return STATUS_ACTIVE;
        }
        if (result.stream().anyMatch(status -> STATUS_DRAFT.equalsIgnoreCase(status))) {
            return STATUS_DRAFT;
        }
        return result.isEmpty() ? null : result.get(0);
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
            if (value.length() == 10) {
                return java.time.LocalDate.parse(value, DATE_FORMATTER.withLocale(Locale.getDefault())).atStartOfDay();
            }
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

    private record ExistingJob(String producerCode, String dueDate, String deliveryPlaceCode, String status) {
    }
}
