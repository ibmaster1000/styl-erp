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

    public Map<String, Map<String, List<String>>> findRulesByStyleCodes(Set<String> styleCodes) {
        if (styleCodes == null || styleCodes.isEmpty() || !hasColumn("styles", "styles_rule")) {
            return Collections.emptyMap();
        }

     // style_code 기준 (품번 조회)
        String sql = """
                select style_code, styles_rule
                from styles
                where style_code in (:styleCodes)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("styleCodes", styleCodes);
        Map<String, Map<String, List<String>>> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql, params, rs -> {
        	String styleCode = rs.getString("style_code");
            String rawRule = rs.getString("styles_rule");
            Map<String, List<String>> parsed = parseRule(rawRule);
            if (styleCode != null && parsed != null) {
                result.put(styleCode, parsed);
            }
        });
        return result;
    }

    public Map<String, BigDecimal> findSupplyPrices(Set<String> styleCodes) {
        if (styleCodes == null || styleCodes.isEmpty() || !hasColumn("styles", "supply_price")) {
            return Collections.emptyMap();
        }

     // before: select style_no, supply_price from styles where style_no in (:styleNos)
        // after:  select style_code, supply_price from styles where style_code in (:styleCodes)
        // style_code 기준 (품번 조회)
        String sql = """
                select style_code, supply_price
                from styles
                where style_code in (:styleCodes)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource("styleCodes", styleCodes);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql, params, rs -> {
        	String styleCode = rs.getString("style_code");
            BigDecimal supplyPrice = rs.getBigDecimal("supply_price");
            if (styleCode != null && supplyPrice != null) {
                result.put(styleCode, supplyPrice);
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