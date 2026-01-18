package com.example.erp.repository;

import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DashboardQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DashboardQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countProductionJobsNotDone() {
        if (!hasTable("production_jobs") || !hasColumn("production_jobs", "status")) {
            return 0L;
        }
        String sql = "select coalesce(count(*), 0) from production_jobs where status <> 'DONE'";
        return queryForCount(sql, new MapSqlParameterSource());
    }

    public long countMaterialShortage() {
        if (!hasTable("material_inventory") || !hasColumn("material_inventory", "qty")) {
            return 0L;
        }
        String sql = "select coalesce(count(*), 0) from material_inventory where qty <= 0";
        return queryForCount(sql, new MapSqlParameterSource());
    }

    public long countTodayFgInbounds() {
        if (!hasTable("fg_inbounds")) {
            return 0L;
        }
        String dateColumn = firstExistingColumn("fg_inbounds", List.of("created_at", "created_date", "createdAt"));
        if (!StringUtils.hasText(dateColumn)) {
            return 0L;
        }
        String sql = "select coalesce(count(*), 0) from `fg_inbounds` where date(`%s`) = curdate()"
                .formatted(dateColumn);
        return queryForCount(sql, new MapSqlParameterSource());
    }

    public long countTodayFgOutbounds() {
        String tableName = firstExistingTable(List.of("fg_outbounds", "fg_outbound", "finished_goods_outbounds"));
        if (!StringUtils.hasText(tableName)) {
            return 0L;
        }
        String dateColumn = firstExistingColumn(tableName, List.of("created_at", "created_date", "createdAt"));
        if (!StringUtils.hasText(dateColumn)) {
            return 0L;
        }
        String sql = "select coalesce(count(*), 0) from `%s` where date(`%s`) = curdate()"
                .formatted(tableName, dateColumn);
        return queryForCount(sql, new MapSqlParameterSource());
    }

    public long countProductionAgreementsTotal() {
        if (!hasTable("production_agreements")) {
            return 0L;
        }
        String sql = "select coalesce(count(*), 0) from production_agreements";
        return queryForCount(sql, new MapSqlParameterSource());
    }

    public long countProductionJobsTotal() {
        if (!hasTable("production_jobs")) {
            return 0L;
        }
        String sql = "select coalesce(count(*), 0) from production_jobs";
        return queryForCount(sql, new MapSqlParameterSource());
    }

    public long countProductionJobsNotWait() {
        if (!hasTable("production_jobs") || !hasColumn("production_jobs", "status")) {
            return 0L;
        }
        String sql = "select coalesce(count(*), 0) from production_jobs where status <> 'WAIT'";
        return queryForCount(sql, new MapSqlParameterSource());
    }

    public long countFgInboundsTotal() {
        if (!hasTable("fg_inbounds")) {
            return 0L;
        }
        String sql = "select coalesce(count(*), 0) from fg_inbounds";
        return queryForCount(sql, new MapSqlParameterSource());
    }

    public long countFgOutboundsTotal() {
        String tableName = firstExistingTable(List.of("fg_outbounds", "fg_outbound", "finished_goods_outbounds"));
        if (!StringUtils.hasText(tableName)) {
            return 0L;
        }
        String sql = "select coalesce(count(*), 0) from `%s`".formatted(tableName);
        return queryForCount(sql, new MapSqlParameterSource());
    }

    public Map<String, Long> countProductionJobsByStatus() {
        if (!hasTable("production_jobs") || !hasColumn("production_jobs", "status")) {
            return Collections.emptyMap();
        }
        String sql = "select status, coalesce(count(*), 0) as cnt from production_jobs group by status";
        Map<String, Long> result = new LinkedHashMap<>();
        try {
            jdbcTemplate.query(sql, new MapSqlParameterSource(), (RowCallbackHandler) rs -> {
                String status = rs.getString("status");
                if (!StringUtils.hasText(status)) {
                    status = "UNKNOWN";
                }
                result.put(status, rs.getLong("cnt"));
            });
            return result;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public boolean hasTable(String tableName) {
        if (!StringUtils.hasText(tableName)) {
            return false;
        }
        try {
            MapSqlParameterSource params = new MapSqlParameterSource("tableName", tableName);
            Integer count = jdbcTemplate.queryForObject("""
                    select count(*)
                    from information_schema.tables
                    where table_schema = database()
                      and upper(table_name) = upper(:tableName)
                    """, params, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
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
                    where table_schema = database()
                      and upper(table_name) = upper(:tableName)
                      and upper(column_name) = upper(:columnName)
                    """, params, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public String firstExistingTable(List<String> candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (hasTable(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public String firstExistingColumn(String table, List<String> candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (hasColumn(table, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private long queryForCount(String sql, MapSqlParameterSource params) {
        try {
            Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
            return count == null ? 0L : count;
        } catch (Exception e) {
            return 0L;
        }
    }
}
