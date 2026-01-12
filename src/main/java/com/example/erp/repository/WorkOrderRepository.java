package com.example.erp.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class WorkOrderRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public WorkOrderRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Long> findStylesIdByStyleCode(String styleCode) {
        String sql = """
                select styles_id
                from styles
                where style_code = :styleCode
                limit 1
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("styleCode", styleCode);
        List<Long> result = jdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getLong("styles_id"));
        return result.isEmpty() ? Optional.empty() : Optional.ofNullable(result.get(0));
    }

    public Optional<Long> findOrderIdByStylesId(Long stylesId) {
        String sql = """
                select order_id
                from work_orders
                where styles_id = :stylesId
                limit 1
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("stylesId", stylesId);
        List<Long> result = jdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getLong("order_id"));
        return result.isEmpty() ? Optional.empty() : Optional.ofNullable(result.get(0));
    }

    public Optional<Long> findStylesIdByOrderId(Long orderId) {
        String sql = """
                select styles_id
                from work_orders
                where order_id = :orderId
                limit 1
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("orderId", orderId);
        List<Long> result = jdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getLong("styles_id"));
        return result.isEmpty() ? Optional.empty() : Optional.ofNullable(result.get(0));
    }

    public List<String> findSizeCodes(Long stylesId) {
        String sql = """
                select code
                from styles_rule
                where styles_id = :stylesId
                  and code_type = 'SIZE'
                order by code asc
                """;
        return jdbcTemplate.query(sql, new MapSqlParameterSource("stylesId", stylesId),
                (rs, rowNum) -> rs.getString("code"));
    }

    public Map<String, SizeSpecRow> findSizeSpecs(Long orderId) {
        String sql = """
                select size_code, total_length, waist_width, thigh_width, hip_width, inseam_length
                from work_order_size_specs
                where order_id = :orderId
                  and size_type = 'SIZE'
                """;
        Map<String, SizeSpecRow> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql, new MapSqlParameterSource("orderId", orderId), rs -> {
            String sizeCode = rs.getString("size_code");
            result.put(sizeCode,
                    new SizeSpecRow(sizeCode,
                            rs.getBigDecimal("total_length"),
                            rs.getBigDecimal("waist_width"),
                            rs.getBigDecimal("thigh_width"),
                            rs.getBigDecimal("hip_width"),
                            rs.getBigDecimal("inseam_length")));
        });
        return result;
    }

    public Optional<AttachmentRow> findAttachment(Long orderId) {
        String sql = """
                select illustration_path, sewing_path
                from work_order_attachments
                where order_id = :orderId
                limit 1
                """;
        List<AttachmentRow> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("orderId", orderId),
                (rs, rowNum) -> new AttachmentRow(rs.getString("illustration_path"), rs.getString("sewing_path")));
        return rows.isEmpty() ? Optional.empty() : Optional.ofNullable(rows.get(0));
    }

    public Long insertWorkOrder(Long stylesId, LocalDateTime now) {
        String sql = """
                insert into work_orders (styles_id, created_at, updated_at)
                values (:stylesId, :createdAt, :updatedAt)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("stylesId", stylesId)
                .addValue("createdAt", Timestamp.valueOf(now))
                .addValue("updatedAt", Timestamp.valueOf(now));
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[] { "order_id" });
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    public void updateWorkOrderUpdatedAt(Long orderId, LocalDateTime now) {
        String sql = """
                update work_orders
                set updated_at = :updatedAt
                where order_id = :orderId
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("orderId", orderId)
                .addValue("updatedAt", Timestamp.valueOf(now));
        jdbcTemplate.update(sql, params);
    }

    public boolean existsSizeSpec(Long orderId, String sizeCode) {
        String sql = """
                select count(*)
                from work_order_size_specs
                where order_id = :orderId
                  and size_code = :sizeCode
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("orderId", orderId)
                .addValue("sizeCode", sizeCode);
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public void insertSizeSpec(Long orderId, String sizeCode, SizeSpecRow spec, LocalDateTime now) {
        String sql = """
                insert into work_order_size_specs
                (order_id, size_type, size_code, total_length, waist_width, thigh_width, hip_width, inseam_length,
                 created_at, updated_at)
                values
                (:orderId, 'SIZE', :sizeCode, :totalLength, :waistWidth, :thighWidth, :hipWidth, :inseamLength,
                 :createdAt, :updatedAt)
                """;
        MapSqlParameterSource params = buildSizeSpecParams(orderId, sizeCode, spec, now, true);
        jdbcTemplate.update(sql, params);
    }

    public void updateSizeSpec(Long orderId, String sizeCode, SizeSpecRow spec, LocalDateTime now) {
        String sql = """
                update work_order_size_specs
                set total_length = :totalLength,
                    waist_width = :waistWidth,
                    thigh_width = :thighWidth,
                    hip_width = :hipWidth,
                    inseam_length = :inseamLength,
                    updated_at = :updatedAt
                where order_id = :orderId
                  and size_code = :sizeCode
                """;
        MapSqlParameterSource params = buildSizeSpecParams(orderId, sizeCode, spec, now, false);
        jdbcTemplate.update(sql, params);
    }

    public void clearAttachment(Long orderId, AttachmentType type) {
        String column = type == AttachmentType.ILLUSTRATION ? "illustration_path" : "sewing_path";
        String sql = """
                update work_order_attachments
                set %s = null
                where order_id = :orderId
                """.formatted(column);
        jdbcTemplate.update(sql, new MapSqlParameterSource("orderId", orderId));
    }

    public void upsertAttachment(Long orderId, AttachmentType type, String path, LocalDateTime now) {
        String column = type == AttachmentType.ILLUSTRATION ? "illustration_path" : "sewing_path";
        String updateSql = "update work_order_attachments set " + column + " = :path where order_id = :orderId";
        MapSqlParameterSource updateParams = new MapSqlParameterSource()
                .addValue("orderId", orderId)
                .addValue("path", path);
        int updated = jdbcTemplate.update(updateSql, updateParams);
        if (updated > 0) {
            return;
        }
        String insertSql = """
                insert into work_order_attachments
                (order_id, illustration_path, sewing_path, created_date)
                values
                (:orderId, :illustrationPath, :sewingPath, :createdDate)
                """;
        MapSqlParameterSource insertParams = new MapSqlParameterSource()
                .addValue("orderId", orderId)
                .addValue("illustrationPath", type == AttachmentType.ILLUSTRATION ? path : null)
                .addValue("sewingPath", type == AttachmentType.SEWING ? path : null)
                .addValue("createdDate", Timestamp.valueOf(now));
        jdbcTemplate.update(insertSql, insertParams);
    }

    private MapSqlParameterSource buildSizeSpecParams(Long orderId, String sizeCode, SizeSpecRow spec,
                                                      LocalDateTime now, boolean includeCreatedAt) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("orderId", orderId)
                .addValue("sizeCode", sizeCode)
                .addValue("totalLength", spec.totalLength())
                .addValue("waistWidth", spec.waistWidth())
                .addValue("thighWidth", spec.thighWidth())
                .addValue("hipWidth", spec.hipWidth())
                .addValue("inseamLength", spec.inseamLength())
                .addValue("updatedAt", Timestamp.valueOf(now));
        if (includeCreatedAt) {
            params.addValue("createdAt", Timestamp.valueOf(now));
        }
        return params;
    }

    public record SizeSpecRow(String sizeCode, BigDecimal totalLength, BigDecimal waistWidth, BigDecimal thighWidth,
                              BigDecimal hipWidth, BigDecimal inseamLength) {
        public static SizeSpecRow empty(String sizeCode) {
            return new SizeSpecRow(sizeCode, null, null, null, null, null);
        }
    }

    public record AttachmentRow(String illustrationPath, String sewingPath) {
    }

    public enum AttachmentType {
        ILLUSTRATION,
        SEWING
    }
}
