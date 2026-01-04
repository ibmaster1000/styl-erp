package com.example.erp.service;

import com.example.erp.controller.dto.MaterialSpecView;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MaterialSpecService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MaterialSpecService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MaterialSpecView> findAllSorted() {
        if (!hasTable("material_specs")) {
            return Collections.emptyList();
        }

        boolean hasCategory = hasColumn("material_specs", "category");
        boolean hasMaterialName = hasColumn("material_specs", "material_name");
        boolean hasMaterialUsage = hasColumn("material_specs", "material_usage");
        boolean hasSpec = hasColumn("material_specs", "spec");
        boolean hasMaterialColor = hasColumn("material_specs", "material_color");
        boolean hasUom = hasColumn("material_specs", "uom");
        boolean hasQtyPerPiece = hasColumn("material_specs", "qty_per_piece");
        boolean hasMaterialCode = hasColumn("material_specs", "material_code");
        boolean hasSupplierCode = hasColumn("material_specs", "supplier_code");
        boolean hasLossRate = hasColumn("material_specs", "loss_rate");
        boolean hasOrderUom = hasColumn("material_specs", "order_uom");
        boolean hasUnitPrice = hasColumn("material_specs", "unit_price");

        String sql = "select " +
                selectOrNull("category", hasCategory) + ", " +
                selectOrNull("material_name", hasMaterialName) + ", " +
                selectOrNull("material_usage", hasMaterialUsage) + ", " +
                selectOrNull("spec", hasSpec) + ", " +
                selectOrNull("material_color", hasMaterialColor) + ", " +
                selectOrNull("uom", hasUom) + ", " +
                selectOrNull("qty_per_piece", hasQtyPerPiece) + ", " +
                selectOrNull("material_code", hasMaterialCode) + ", " +
                selectOrNull("supplier_code", hasSupplierCode) + ", " +
                selectOrNull("loss_rate", hasLossRate) + ", " +
                selectOrNull("order_uom", hasOrderUom) + ", " +
                selectOrNull("unit_price", hasUnitPrice) +
                " from material_specs";
        if (hasCategory) {
            sql += " order by category";
        }

        List<MaterialSpecView> result = new ArrayList<>();
        jdbcTemplate.query(sql, rs -> {
            String category = rs.getString("category");
            String materialName = rs.getString("material_name");
            String materialUsage = rs.getString("material_usage");
            String spec = rs.getString("spec");
            String materialColor = rs.getString("material_color");
            String uom = rs.getString("uom");
            BigDecimal qtyPerPiece = hasQtyPerPiece ? rs.getBigDecimal("qty_per_piece") : null;
            String materialCode = rs.getString("material_code");
            String supplierCode = rs.getString("supplier_code");
            BigDecimal lossRate = hasLossRate ? rs.getBigDecimal("loss_rate") : null;
            String orderUom = rs.getString("order_uom");
            BigDecimal unitPrice = hasUnitPrice ? rs.getBigDecimal("unit_price") : null;
            result.add(new MaterialSpecView(category, materialName, materialUsage, spec, materialColor, uom,
                    qtyPerPiece, materialCode, supplierCode, lossRate, orderUom, unitPrice));
        });
        return result;
    }

    private String selectOrNull(String column, boolean exists) {
        return exists ? column : "null as " + column;
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