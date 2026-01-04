package com.example.erp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
public class StyleRuleService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StyleRuleService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Map<String, List<String>>> findRulesByStyleNos(Set<String> styleNos) {
        if (styleNos == null || styleNos.isEmpty() || !hasColumn("styles", "styles_rule")) {
            return Collections.emptyMap();
        }

        String sql = """
                select style_no, styles_rule
                from styles
                where style_no in (:styleNos)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("styleNos", styleNos);
        Map<String, Map<String, List<String>>> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql, params, rs -> {
            String styleNo = rs.getString("style_no");
            String rawRule = rs.getString("styles_rule");
            Map<String, List<String>> parsed = parseRule(rawRule);
            if (styleNo != null && parsed != null) {
                result.put(styleNo, parsed);
            }
        });
        return result;
    }

    public Map<String, BigDecimal> findSupplyPrices(Set<String> styleNos) {
        if (styleNos == null || styleNos.isEmpty() || !hasColumn("styles", "supply_price")) {
            return Collections.emptyMap();
        }

        String sql = """
                select style_no, supply_price
                from styles
                where style_no in (:styleNos)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("styleNos", styleNos);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql, params, rs -> {
            String styleNo = rs.getString("style_no");
            BigDecimal supplyPrice = rs.getBigDecimal("supply_price");
            if (styleNo != null && supplyPrice != null) {
                result.put(styleNo, supplyPrice);
            }
        });
        return result;
    }

    private Map<String, List<String>> parseRule(String rawRule) {
        if (rawRule == null || rawRule.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(rawRule);
            Map<String, List<String>> parsed = new TreeMap<>();
            node.fields().forEachRemaining(entry -> {
                JsonNode sizesNode = entry.getValue();
                if (sizesNode != null && sizesNode.isArray()) {
                    parsed.put(entry.getKey(), objectMapper.convertValue(sizesNode, List.class));
                }
            });
            return parsed;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean hasColumn(String tableName, String columnName) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("tableName", tableName)
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
}