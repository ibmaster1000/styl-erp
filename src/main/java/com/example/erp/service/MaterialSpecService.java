package com.example.erp.service;

import com.example.erp.controller.dto.AgreementQuantitySummary;
import com.example.erp.controller.dto.MaterialSpecCodeView;
import com.example.erp.controller.dto.MaterialSpecContextView;
import com.example.erp.controller.dto.MaterialSpecItemView;
import com.example.erp.controller.dto.MaterialSpecMatrixCodeView;
import com.example.erp.controller.dto.MaterialSpecMatrixResponse;
import com.example.erp.controller.dto.MaterialSpecOptionsResponse;
import com.example.erp.controller.dto.MaterialSpecSaveItem;
import com.example.erp.controller.dto.MaterialSpecSaveRequest;
import com.example.erp.domain.Code;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class MaterialSpecService {

    private static final int MATERIAL_CODE_PADDING = 3;
    private static final Logger log = LoggerFactory.getLogger(MaterialSpecService.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final StyleRuleService styleRuleService;
    private final AgreementQueryService agreementQueryService;

    public MaterialSpecService(NamedParameterJdbcTemplate jdbcTemplate, StyleRuleService styleRuleService,
            AgreementQueryService agreementQueryService) {
        this.jdbcTemplate = jdbcTemplate;
        this.styleRuleService = styleRuleService;
        this.agreementQueryService = agreementQueryService;
    }

    public MaterialSpecContextView loadContext(String styleCode, String prdAgreeCode, String colorCode) {
        String normalizedStyleCode = normalize(styleCode);
        String normalizedAgreeCode = normalize(prdAgreeCode);
        String normalizedColor = normalize(colorCode);
        Long stylesId = resolveStylesId(normalizedStyleCode);
        Long prdAgreeId = resolveAgreementId(normalizedAgreeCode, normalizedColor);
        List<String> sizes = findSizes(normalizedStyleCode, normalizedColor);
        Map<String, Integer> quantities = findAgreementQuantities(normalizedStyleCode, normalizedAgreeCode,
                normalizedColor);
        List<AgreementQuantitySummary> agreementSummaryRows =
                agreementQueryService.findAgreementQuantities(normalizedAgreeCode);
        List<MaterialSpecItemView> materials = findMaterialSpecs(normalizedStyleCode, normalizedAgreeCode,
                normalizedColor, prdAgreeId, stylesId);
        return new MaterialSpecContextView(normalizedStyleCode, normalizedColor, normalizedAgreeCode, stylesId,
                prdAgreeId, sizes, quantities, materials, agreementSummaryRows);
    }

    public List<MaterialSpecCodeView> filterCodes(List<Code> codes, String keyword) {
        if (codes == null || codes.isEmpty()) {
            return Collections.emptyList();
        }
        String trimmed = normalize(keyword);
        return codes.stream()
                .filter(code -> {
                    if (!StringUtils.hasText(trimmed)) {
                        return true;
                    }
                    return containsIgnoreCase(code.getCode(), trimmed)
                            || containsIgnoreCase(code.getCodeName(), trimmed)
                            || containsIgnoreCase(code.getRemark(), trimmed);
                })
                .sorted(Comparator.comparing(Code::getCode, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(code -> new MaterialSpecCodeView(code.getCode(), code.getCodeName(), code.getRemark()))
                .toList();
    }

    public MaterialSpecOptionsResponse loadOptions(String styleCode) {
        String normalizedStyleCode = normalize(styleCode);
        if (!StringUtils.hasText(normalizedStyleCode) || !hasTable("production_agreements")) {
            return new MaterialSpecOptionsResponse(Collections.emptyList(), Collections.emptyList());
        }
        String styleCodeColumn = findFirstExistingColumn("production_agreements", List.of("style_code"));
        String colorColumn = findFirstExistingColumn("production_agreements", List.of("color_code"));
        String agreementColumn = findFirstExistingColumn("production_agreements",
                List.of("prd_agree_code", "agreement_code"));
        if (styleCodeColumn == null || colorColumn == null || agreementColumn == null) {
            return new MaterialSpecOptionsResponse(Collections.emptyList(), Collections.emptyList());
        }
        List<String> colors = findDistinctValues("production_agreements", colorColumn, styleCodeColumn,
                normalizedStyleCode);
        List<String> agreements = findDistinctValues("production_agreements", agreementColumn, styleCodeColumn,
                normalizedStyleCode);
        return new MaterialSpecOptionsResponse(colors, agreements);
    }

    public MaterialSpecMatrixResponse loadMatrix(String styleCode) {
        String normalizedStyleCode = normalize(styleCode);
        if (!StringUtils.hasText(normalizedStyleCode) || !hasTable("production_agreements")) {
            return new MaterialSpecMatrixResponse(Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyMap());
        }
        String styleCodeColumn = findFirstExistingColumn("production_agreements", List.of("style_code"));
        String colorColumn = findFirstExistingColumn("production_agreements", List.of("color_code"));
        String sizeColumn = findFirstExistingColumn("production_agreements", List.of("size_code"));
        String qtyColumn = findFirstExistingColumn("production_agreements", List.of("quantity"));
        String colorTypeColumn = findFirstExistingColumn("production_agreements", List.of("color_type"));
        String sizeTypeColumn = findFirstExistingColumn("production_agreements", List.of("size_type"));
        if (styleCodeColumn == null || colorColumn == null || sizeColumn == null) {
            return new MaterialSpecMatrixResponse(Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyMap());
        }

        String colorColumnRef = "pa." + colorColumn;
        String sizeColumnRef = "pa." + sizeColumn;
        String styleColumnRef = "pa." + styleCodeColumn;
        String colorTypeExpr = colorTypeColumn != null ? "pa." + colorTypeColumn : "'COLOR'";
        String sizeTypeExpr = sizeTypeColumn != null ? "pa." + sizeTypeColumn : "'SIZE'";

        boolean canJoinCodes = hasTable("codes");
        String codeTypeColumn = canJoinCodes ? findFirstExistingColumn("codes", List.of("code_type", "id_code_type"))
                : null;
        String codeColumn = canJoinCodes ? findFirstExistingColumn("codes", List.of("code", "id_code")) : null;
        String codeNameColumn = canJoinCodes ? findFirstExistingColumn("codes", List.of("code_name", "name")) : null;
        canJoinCodes = canJoinCodes && codeTypeColumn != null && codeColumn != null && codeNameColumn != null;

        MapSqlParameterSource params = new MapSqlParameterSource("styleCode", normalizedStyleCode);

        String colorSql = buildMatrixCodeSql("color_codes", colorColumnRef, styleColumnRef, colorTypeExpr,
                codeTypeColumn, codeColumn, codeNameColumn, canJoinCodes);
        List<MaterialSpecMatrixCodeView> colors = jdbcTemplate.query(colorSql, params,
                (rs, rowNum) -> new MaterialSpecMatrixCodeView(rs.getString("code"), rs.getString("name")));

        String sizeSql = buildMatrixCodeSql("size_codes", sizeColumnRef, styleColumnRef, sizeTypeExpr,
                codeTypeColumn, codeColumn, codeNameColumn, canJoinCodes);
        List<MaterialSpecMatrixCodeView> sizes = jdbcTemplate.query(sizeSql, params,
                (rs, rowNum) -> new MaterialSpecMatrixCodeView(rs.getString("code"), rs.getString("name")));

        Map<String, Integer> quantities = Collections.emptyMap();
        if (qtyColumn != null) {
            String qtySql = "select " + colorColumnRef + " as color_code, " + sizeColumnRef
                    + " as size_code, pa." + qtyColumn + " as quantity from production_agreements pa where "
                    + styleColumnRef + " = :styleCode and " + colorColumnRef + " is not null and "
                    + sizeColumnRef + " is not null";
            quantities = jdbcTemplate.query(qtySql, params, rs -> {
                Map<String, Integer> result = new LinkedHashMap<>();
                while (rs.next()) {
                    Integer quantity = rs.getObject("quantity") != null ? rs.getInt("quantity") : null;
                    if (quantity == null) {
                        continue;
                    }
                    String key = rs.getString("color_code") + "|" + rs.getString("size_code");
                    result.put(key, quantity);
                }
                return result;
            });
        }

        return new MaterialSpecMatrixResponse(colors, sizes, quantities);
    }

    @Transactional
    public Map<String, Object> saveSpecs(MaterialSpecSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getStyleCode())
                || !StringUtils.hasText(request.getPrdAgreeCode()) || !StringUtils.hasText(request.getColorCode())) {
            throw new IllegalArgumentException("조회된 컨텍스트가 없습니다.");
        }
        if (!hasTable("material_specs")) {
            throw new IllegalStateException("material_specs 테이블이 습니다.");
        }

        String styleCode = normalize(request.getStyleCode());
        String colorCode = normalize(request.getColorCode());
        String prdAgreeCode = normalize(request.getPrdAgreeCode());
        Long stylesId = resolveStylesId(styleCode);
        Long prdAgreeId = resolveAgreementId(prdAgreeCode, colorCode);

        String bomIdColumn = findFirstExistingColumn("material_specs", List.of("bom_id", "id"));
        String prdAgreeIdColumn = findFirstExistingColumn("material_specs", List.of("prd_agree_id"));
        String prdAgreeCodeColumn = findFirstExistingColumn("material_specs",
                List.of("prd_agree_code", "agreement_code"));
        String styleCodeColumn = findFirstExistingColumn("material_specs", List.of("style_code"));
        String styleIdColumn = findFirstExistingColumn("material_specs", List.of("styles_id", "style_id"));
        String colorCodeColumn = findFirstExistingColumn("material_specs", List.of("color_code", "color"));
        String materialCodeColumn = findFirstExistingColumn("material_specs", List.of("material_code"));
        String categoryColumn = findFirstExistingColumn("material_specs", List.of("category"));
        String materialNameColumn = findFirstExistingColumn("material_specs", List.of("material_name"));
        String materialUsageColumn = findFirstExistingColumn("material_specs", List.of("material_usage"));
        String specColumn = findFirstExistingColumn("material_specs", List.of("spec"));
        String materialColorColumn = findFirstExistingColumn("material_specs", List.of("material_color"));
        String uomColumn = findFirstExistingColumn("material_specs", List.of("uom"));
        String qtyPerPieceColumn = findFirstExistingColumn("material_specs", List.of("qty_per_piece"));
        String supplierCodeColumn = findFirstExistingColumn("material_specs", List.of("supplier_code"));
        String lossRateColumn = findFirstExistingColumn("material_specs", List.of("loss_rate"));
        String orderUomColumn = findFirstExistingColumn("material_specs", List.of("order_uom"));
        String unitPriceColumn = findFirstExistingColumn("material_specs", List.of("unit_price"));
        String remarkColumn = findFirstExistingColumn("material_specs", List.of("remark"));
        String colorTypeColumn = findFirstExistingColumn("material_specs", List.of("color_type"));
        String supplierTypeColumn = findFirstExistingColumn("material_specs", List.of("supplier_type"));
        if (isGeneratedColumn("material_specs", colorTypeColumn)) {
            colorTypeColumn = null;
        }
        if (isGeneratedColumn("material_specs", supplierTypeColumn)) {
            supplierTypeColumn = null;
        }

        if (supplierCodeColumn != null && !isNullableColumn("material_specs", supplierCodeColumn)) {
            for (MaterialSpecSaveItem item : safeItems(request.getItems())) {
                if (item == null) {
                    continue;
                }
                if (!StringUtils.hasText(normalize(item.getSupplierCode()))) {
                    throw new IllegalArgumentException("자재처는 필수입니다.");
                }
            }
        }

        int created = 0;
        int updated = 0;
        int deleted = 0;

        if (bomIdColumn != null && request.getDeletedIds() != null) {
            for (Long bomId : request.getDeletedIds()) {
                if (bomId == null) {
                    continue;
                }
                StringBuilder sql = new StringBuilder("delete from material_specs where ").append(bomIdColumn)
                        .append(" = :bomId ");
                MapSqlParameterSource params = new MapSqlParameterSource().addValue("bomId", bomId);
                if (prdAgreeCodeColumn != null) {
                    sql.append("and ").append(prdAgreeCodeColumn).append(" = :prdAgreeCode ");
                    params.addValue("prdAgreeCode", prdAgreeCode);
                }
                if (colorCodeColumn != null) {
                    sql.append("and ").append(colorCodeColumn).append(" = :colorCode ");
                    params.addValue("colorCode", colorCode);
                }
                if (styleIdColumn != null && stylesId != null) {
                    sql.append("and ").append(styleIdColumn).append(" = :stylesId ");
                    params.addValue("stylesId", stylesId);
                } else if (styleCodeColumn != null) {
                    sql.append("and ").append(styleCodeColumn).append(" = :styleCode ");
                    params.addValue("styleCode", styleCode);
                }
                deleted += jdbcTemplate.update(sql.toString(), params);
            }
        }

        Map<String, Integer> sequenceMap = new LinkedHashMap<>();
        for (MaterialSpecSaveItem item : safeItems(request.getItems())) {
            if (item == null) {
                continue;
            }
            String normalizedCategory = normalize(item.getCategory());
            String materialCode = item.getMaterialCode();
            String generatedCode = resolveMaterialCode(styleCode, colorCode, normalizedCategory, materialCode,
                    materialCodeColumn, sequenceMap);
            item.setMaterialCode(generatedCode);
            item.setCategory(normalizedCategory);
        }

        for (MaterialSpecSaveItem item : safeItems(request.getItems())) {
            if (item == null) {
                continue;
            }
            boolean isUpdate = item.getBomId() != null && bomIdColumn != null;
            if (isUpdate) {
                StringBuilder sql = new StringBuilder("update material_specs set ");
                List<String> updates = new ArrayList<>();
                MapSqlParameterSource params = buildParams(item, styleCode, stylesId, prdAgreeCode, prdAgreeId,
                        colorCode, materialCodeColumn, prdAgreeIdColumn, prdAgreeCodeColumn, styleCodeColumn,
                        styleIdColumn, colorCodeColumn, categoryColumn, materialNameColumn, materialUsageColumn,
                        specColumn, materialColorColumn, uomColumn, qtyPerPieceColumn, supplierCodeColumn,
                        lossRateColumn, orderUomColumn, unitPriceColumn, remarkColumn, colorTypeColumn,
                        supplierTypeColumn);
                params.addValue("bomId", item.getBomId());
                addUpdate(updates, prdAgreeIdColumn, "prdAgreeId");
                addUpdate(updates, prdAgreeCodeColumn, "prdAgreeCode");
                addUpdate(updates, styleCodeColumn, "styleCode");
                addUpdate(updates, styleIdColumn, "stylesId");
                addUpdate(updates, colorCodeColumn, "colorCode");
                addUpdate(updates, materialCodeColumn, "materialCode");
                addUpdate(updates, categoryColumn, "category");
                addUpdate(updates, materialNameColumn, "materialName");
                addUpdate(updates, materialUsageColumn, "materialUsage");
                addUpdate(updates, specColumn, "spec");
                addUpdate(updates, materialColorColumn, "materialColor");
                addUpdate(updates, uomColumn, "uom");
                addUpdate(updates, qtyPerPieceColumn, "qtyPerPiece");
                addUpdate(updates, supplierCodeColumn, "supplierCode");
                addUpdate(updates, lossRateColumn, "lossRate");
                addUpdate(updates, orderUomColumn, "orderUom");
                addUpdate(updates, unitPriceColumn, "unitPrice");
                addUpdate(updates, remarkColumn, "remark");
                addUpdate(updates, colorTypeColumn, "colorType");
                addUpdate(updates, supplierTypeColumn, "supplierType");
                sql.append(String.join(", ", updates));
                sql.append(" where ").append(bomIdColumn).append(" = :bomId");
                updated += jdbcTemplate.update(sql.toString(), params);
            } else {
                List<String> columns = new ArrayList<>();
                List<String> values = new ArrayList<>();
                MapSqlParameterSource params = buildParams(item, styleCode, stylesId, prdAgreeCode, prdAgreeId,
                        colorCode, materialCodeColumn, prdAgreeIdColumn, prdAgreeCodeColumn, styleCodeColumn,
                        styleIdColumn, colorCodeColumn, categoryColumn, materialNameColumn, materialUsageColumn,
                        specColumn, materialColorColumn, uomColumn, qtyPerPieceColumn, supplierCodeColumn,
                        lossRateColumn, orderUomColumn, unitPriceColumn, remarkColumn, colorTypeColumn,
                        supplierTypeColumn);
                addInsert(columns, values, prdAgreeIdColumn, "prdAgreeId");
                addInsert(columns, values, prdAgreeCodeColumn, "prdAgreeCode");
                addInsert(columns, values, styleCodeColumn, "styleCode");
                addInsert(columns, values, styleIdColumn, "stylesId");
                addInsert(columns, values, colorCodeColumn, "colorCode");
                addInsert(columns, values, materialCodeColumn, "materialCode");
                addInsert(columns, values, categoryColumn, "category");
                addInsert(columns, values, materialNameColumn, "materialName");
                addInsert(columns, values, materialUsageColumn, "materialUsage");
                addInsert(columns, values, specColumn, "spec");
                addInsert(columns, values, materialColorColumn, "materialColor");
                addInsert(columns, values, uomColumn, "uom");
                addInsert(columns, values, qtyPerPieceColumn, "qtyPerPiece");
                addInsert(columns, values, supplierCodeColumn, "supplierCode");
                addInsert(columns, values, lossRateColumn, "lossRate");
                addInsert(columns, values, orderUomColumn, "orderUom");
                addInsert(columns, values, unitPriceColumn, "unitPrice");
                addInsert(columns, values, remarkColumn, "remark");
                addInsert(columns, values, colorTypeColumn, "colorType");
                addInsert(columns, values, supplierTypeColumn, "supplierType");
                if (!columns.isEmpty()) {
                    String sql = "insert into material_specs (" + String.join(", ", columns) + ") values ("
                            + String.join(", ", values) + ")";
                    created += jdbcTemplate.update(sql, params);
                }
            }
        }

        return Map.of("success", true, "created", created, "updated", updated, "deleted", deleted);
    }

    private List<String> findSizes(String styleCode, String colorCode) {
        if (!hasTable("production_agreements") || !StringUtils.hasText(styleCode) || !StringUtils.hasText(colorCode)) {
            return Collections.emptyList();
        }
        String styleCodeColumn = findFirstExistingColumn("production_agreements", List.of("style_code"));
        String colorColumn = findFirstExistingColumn("production_agreements", List.of("color_code"));
        String sizeColumn = findFirstExistingColumn("production_agreements", List.of("size_code"));
        if (styleCodeColumn == null || colorColumn == null || sizeColumn == null) {
            return Collections.emptyList();
        }
        String sql = "select distinct " + sizeColumn + " as size_code from production_agreements where "
                + styleCodeColumn + " = :styleCode and " + colorColumn + " = :colorCode order by " + sizeColumn;
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("styleCode", styleCode)
                .addValue("colorCode", colorCode);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getString("size_code"));
    }

    private Map<String, Integer> findAgreementQuantities(String styleCode, String prdAgreeCode, String colorCode) {
        if (!hasTable("production_agreements") || !StringUtils.hasText(styleCode) || !StringUtils.hasText(prdAgreeCode)
                || !StringUtils.hasText(colorCode)) {
            return Collections.emptyMap();
        }
        String styleCodeColumn = findFirstExistingColumn("production_agreements", List.of("style_code"));
        String prdColumn = findFirstExistingColumn("production_agreements",
                List.of("prd_agree_code", "agreement_code"));
        String colorColumn = findFirstExistingColumn("production_agreements", List.of("color_code"));
        String sizeColumn = findFirstExistingColumn("production_agreements", List.of("size_code"));
        String qtyColumn = findFirstExistingColumn("production_agreements", List.of("quantity"));
        if (styleCodeColumn == null || prdColumn == null || colorColumn == null || sizeColumn == null
                || qtyColumn == null) {
            return Collections.emptyMap();
        }
        String sql = "select " + sizeColumn + " as size_code, sum(" + qtyColumn + ") as quantity from production_agreements where "
                + styleCodeColumn + " = :styleCode and " + prdColumn + " = :prdAgreeCode and " + colorColumn
                + " = :colorCode group by " + sizeColumn;
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("styleCode", styleCode)
                .addValue("prdAgreeCode", prdAgreeCode).addValue("colorCode", colorCode);

        return jdbcTemplate.query(sql, params, rs -> {
            Map<String, Integer> result = new LinkedHashMap<>();
            while (rs.next()) {
                result.put(rs.getString("size_code"), rs.getInt("quantity"));
            }
            return result;
        });
    }

    private List<MaterialSpecItemView> findMaterialSpecs(String styleCode, String prdAgreeCode, String colorCode,
            Long prdAgreeId, Long stylesId) {
        if (!hasTable("material_specs") || !StringUtils.hasText(colorCode)) {
            return Collections.emptyList();
        }
        String bomIdColumn = findFirstExistingColumn("material_specs", List.of("bom_id", "id"));
        String prdAgreeIdColumn = findFirstExistingColumn("material_specs", List.of("prd_agree_id"));
        String prdColumn = findFirstExistingColumn("material_specs", List.of("prd_agree_code", "agreement_code"));
        String colorColumn = findFirstExistingColumn("material_specs",
                List.of("color_code", "material_color", "color"));
        String styleCodeColumn = findFirstExistingColumn("material_specs", List.of("style_code"));
        String styleIdColumn = findFirstExistingColumn("material_specs", List.of("styles_id", "style_id"));

        if (colorColumn == null) {
            return Collections.emptyList();
        }
        String agreementCondition = null;
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("colorCode", colorCode);
        String usingColumn = null;
        if (prdAgreeIdColumn != null && prdAgreeId != null) {
            agreementCondition = prdAgreeIdColumn + " = :prdAgreeId";
            params.addValue("prdAgreeId", prdAgreeId);
            usingColumn = "prd_agree_id";
        } else if (prdColumn != null && StringUtils.hasText(prdAgreeCode)) {
            agreementCondition = prdColumn + " = :prdAgreeCode";
            params.addValue("prdAgreeCode", prdAgreeCode);
            usingColumn = prdColumn;
        }
        if (agreementCondition == null) {
            return Collections.emptyList();
        }
        log.info("findMaterialSpecs by prdAgreeIdColumn={}, prdColumn={}, using={}",
                prdAgreeIdColumn, prdColumn, usingColumn);

        List<String> selectColumns = List.of(
                "%s as bom_id".formatted(selectOrNull(bomIdColumn, "bom_id")),
                "%s as category".formatted(selectOrNull("category")),
                "%s as material_name".formatted(selectOrNull("material_name")),
                "%s as material_usage".formatted(selectOrNull("material_usage")),
                "%s as spec".formatted(selectOrNull("spec")),
                "%s as material_color".formatted(selectOrNull("material_color")),
                "%s as uom".formatted(selectOrNull("uom")),
                "%s as qty_per_piece".formatted(selectOrNull("qty_per_piece")),
                "%s as material_code".formatted(selectOrNull("material_code")),
                "%s as supplier_code".formatted(selectOrNull("supplier_code")),
                "%s as loss_rate".formatted(selectOrNull("loss_rate")),
                "%s as order_uom".formatted(selectOrNull("order_uom")),
                "%s as unit_price".formatted(selectOrNull("unit_price")),
                "%s as remark".formatted(selectOrNull("remark")));

        String sql = """
                select %s
                from material_specs
                where %s
                  and %s = :colorCode
                %s
                order by %s
                """.formatted(String.join(",\n       ", selectColumns), agreementCondition, colorColumn,
                StringUtils.hasText(styleCode) && styleCodeColumn != null
                        ? "and " + styleCodeColumn + " = :styleCode"
                        : stylesId != null && styleIdColumn != null ? "and " + styleIdColumn + " = :stylesId" : "",
                bomIdColumn != null ? bomIdColumn : prdColumn != null ? prdColumn : prdAgreeIdColumn);
        if (StringUtils.hasText(styleCode) && styleCodeColumn != null) {
            params.addValue("styleCode", styleCode);
        } else if (stylesId != null && styleIdColumn != null) {
            params.addValue("stylesId", stylesId);
        }

        return jdbcTemplate.query(sql, params,
                (rs, rowNum) -> new MaterialSpecItemView(
                        rs.getObject("bom_id") != null ? rs.getLong("bom_id") : null,
                        rs.getString("category"),
                        rs.getString("material_name"),
                        rs.getString("material_usage"),
                        rs.getString("spec"),
                        rs.getString("material_color"),
                        rs.getString("uom"),
                        rs.getBigDecimal("qty_per_piece"),
                        rs.getString("material_code"),
                        rs.getString("supplier_code"),
                        rs.getBigDecimal("loss_rate"),
                        rs.getString("order_uom"),
                        rs.getBigDecimal("unit_price"),
                        rs.getString("remark")));
    }

    private Long resolveStylesId(String styleCode) {
        if (!StringUtils.hasText(styleCode) || !hasTable("styles")) {
            return null;
        }
        String styleCodeColumn = findFirstExistingColumn("styles", List.of("style_code"));
        String styleIdColumn = findFirstExistingColumn("styles", List.of("styles_id", "style_id", "id"));
        if (styleCodeColumn == null || styleIdColumn == null) {
            return null;
        }
        String sql = "select " + styleIdColumn + " as styles_id from styles where " + styleCodeColumn
                + " = :styleCode limit 1";
        MapSqlParameterSource params = new MapSqlParameterSource("styleCode", styleCode);
        List<Long> ids = jdbcTemplate.query(sql, params,
                (rs, rowNum) -> rs.getObject("styles_id") != null ? rs.getLong("styles_id") : null);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Long resolveAgreementId(String prdAgreeCode, String colorCode) {
        if (!StringUtils.hasText(prdAgreeCode) || !hasTable("production_agreements")) {
            return null;
        }
        String prdColumn = findFirstExistingColumn("production_agreements",
                List.of("prd_agree_code", "agreement_code"));
        String idColumn = findFirstExistingColumn("production_agreements", List.of("prd_agree_id"));
        String colorColumn = findFirstExistingColumn("production_agreements", List.of("color_code"));
        if (prdColumn == null || idColumn == null) {
            return null;
        }

        StringBuilder sql = new StringBuilder("select ").append(idColumn)
                .append(" as prd_agree_id from production_agreements where ").append(prdColumn)
                .append(" = :prdAgreeCode ");
        MapSqlParameterSource params = new MapSqlParameterSource("prdAgreeCode", prdAgreeCode);
        if (StringUtils.hasText(colorCode) && colorColumn != null) {
            sql.append("and ").append(colorColumn).append(" = :colorCode ");
            params.addValue("colorCode", colorCode);
        }
        sql.append("order by ").append(idColumn).append(" asc limit 1");

        List<Long> ids = jdbcTemplate.query(sql.toString(), params,
                (rs, rowNum) -> rs.getObject("prd_agree_id") != null ? rs.getLong("prd_agree_id") : null);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private String resolveMaterialCode(String styleCode, String colorCode, String category, String materialCode,
            String materialCodeColumn, Map<String, Integer> sequenceMap) {
        if (!StringUtils.hasText(styleCode) || !StringUtils.hasText(colorCode) || !StringUtils.hasText(category)
                || materialCodeColumn == null) {
            return materialCode;
        }
        String categoryCode = toCategoryCode(category);
        if (!StringUtils.hasText(categoryCode)) {
            return materialCode;
        }
        String prefix = styleCode + "-" + colorCode + "-" + categoryCode;
        if (StringUtils.hasText(materialCode) && materialCode.startsWith(prefix + "-")) {
            return materialCode;
        }
        int nextSeq = sequenceMap.compute(prefix, (key, value) -> {
            if (value == null) {
                return loadMaxSequence(prefix, materialCodeColumn) + 1;
            }
            return value + 1;
        });
        return prefix + "-" + String.format(Locale.US, "%0" + MATERIAL_CODE_PADDING + "d", nextSeq);
    }

    private int loadMaxSequence(String prefix, String materialCodeColumn) {
        String sql = "select " + materialCodeColumn + " as material_code from material_specs where "
                + materialCodeColumn + " like :prefix";
        MapSqlParameterSource params = new MapSqlParameterSource("prefix", prefix + "%");
        List<String> codes = jdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getString("material_code"));
        int max = 0;
        for (String code : codes) {
            Integer seq = extractSequence(code);
            if (seq != null && seq > max) {
                max = seq;
            }
        }
        return max;
    }

    private Integer extractSequence(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        int lastDash = code.lastIndexOf('-');
        if (lastDash < 0 || lastDash == code.length() - 1) {
            return null;
        }
        String suffix = code.substring(lastDash + 1);
        try {
            return Integer.parseInt(suffix);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String toCategoryCode(String category) {
        if (!StringUtils.hasText(category)) {
            return null;
        }
        String normalized = category.trim();
        if ("원자재".equals(normalized)) {
            return "1";
        }
        if ("부자재".equals(normalized)) {
            return "2";
        }
        return null;
    }

    private MapSqlParameterSource buildParams(MaterialSpecSaveItem item, String styleCode, Long stylesId,
            String prdAgreeCode, Long prdAgreeId, String colorCode, String materialCodeColumn, String prdAgreeIdColumn,
            String prdAgreeCodeColumn, String styleCodeColumn, String styleIdColumn, String colorCodeColumn,
            String categoryColumn, String materialNameColumn, String materialUsageColumn, String specColumn,
            String materialColorColumn, String uomColumn, String qtyPerPieceColumn, String supplierCodeColumn,
            String lossRateColumn, String orderUomColumn, String unitPriceColumn, String remarkColumn,
            String colorTypeColumn, String supplierTypeColumn) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (prdAgreeIdColumn != null) {
            params.addValue("prdAgreeId", prdAgreeId);
        }
        if (prdAgreeCodeColumn != null) {
            params.addValue("prdAgreeCode", prdAgreeCode);
        }
        if (styleCodeColumn != null) {
            params.addValue("styleCode", styleCode);
        }
        if (styleIdColumn != null) {
            params.addValue("stylesId", stylesId);
        }
        if (colorCodeColumn != null) {
            params.addValue("colorCode", colorCode);
        }
        if (materialCodeColumn != null) {
            params.addValue("materialCode", normalize(item.getMaterialCode()));
        }
        if (categoryColumn != null) {
            params.addValue("category", normalize(item.getCategory()));
        }
        if (materialNameColumn != null) {
            params.addValue("materialName", normalize(item.getMaterialName()));
        }
        if (materialUsageColumn != null) {
            params.addValue("materialUsage", normalize(item.getMaterialUsage()));
        }
        if (specColumn != null) {
            params.addValue("spec", normalize(item.getSpec()));
        }
        if (materialColorColumn != null) {
            params.addValue("materialColor", normalize(item.getMaterialColor()));
        }
        if (uomColumn != null) {
            params.addValue("uom", normalize(item.getUom()));
        }
        if (qtyPerPieceColumn != null) {
            params.addValue("qtyPerPiece", item.getQtyPerPiece());
        }
        if (supplierCodeColumn != null) {
            params.addValue("supplierCode", normalize(item.getSupplierCode()));
        }
        if (lossRateColumn != null) {
            params.addValue("lossRate", item.getLossRate());
        }
        if (orderUomColumn != null) {
            params.addValue("orderUom", normalize(item.getOrderUom()));
        }
        if (unitPriceColumn != null) {
            params.addValue("unitPrice", item.getUnitPrice());
        }
        if (remarkColumn != null) {
            params.addValue("remark", normalize(item.getRemark()));
        }
        if (colorTypeColumn != null) {
            params.addValue("colorType", "COLOR");
        }
        if (supplierTypeColumn != null) {
            params.addValue("supplierType", "CUSTOMER");
        }
        return params;
    }

    private void addUpdate(List<String> updates, String column, String param) {
        if (column != null) {
            updates.add(column + " = :" + param);
        }
    }

    private void addInsert(List<String> columns, List<String> values, String column, String param) {
        if (column != null) {
            columns.add(column);
            values.add(":" + param);
        }
    }

    private List<MaterialSpecSaveItem> safeItems(List<MaterialSpecSaveItem> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        return items;
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
        return selectOrNull(column, null);
    }

    private String selectOrNull(String column, String alias) {
        if (column == null) {
            return "null";
        }
        return column;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        if (!StringUtils.hasText(value) || !StringUtils.hasText(keyword)) {
            return false;
        }
        return value.toLowerCase(Locale.KOREAN).contains(keyword.toLowerCase(Locale.KOREAN));
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

    private boolean isGeneratedColumn(String tableName, String columnName) {
        if (!StringUtils.hasText(columnName)) {
            return false;
        }
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    select count(*)
                    from information_schema.columns
                    where upper(table_name) = upper(:tableName)
                      and upper(column_name) = upper(:columnName)
                      and generation_expression is not null
                      and generation_expression <> ''
                    """, new MapSqlParameterSource()
                    .addValue("tableName", tableName)
                    .addValue("columnName", columnName), Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isNullableColumn(String tableName, String columnName) {
        if (!StringUtils.hasText(columnName)) {
            return true;
        }
        try {
            String isNullable = jdbcTemplate.queryForObject("""
                    select is_nullable
                    from information_schema.columns
                    where upper(table_name) = upper(:tableName)
                      and upper(column_name) = upper(:columnName)
                    """, new MapSqlParameterSource()
                    .addValue("tableName", tableName)
                    .addValue("columnName", columnName), String.class);
            return "YES".equalsIgnoreCase(isNullable);
        } catch (Exception e) {
            return true;
        }
    }

    private List<String> findDistinctValues(String tableName, String valueColumn, String styleCodeColumn,
            String styleCode) {
        String sql = "select distinct " + valueColumn + " as value from " + tableName + " where " + styleCodeColumn
                + " = :styleCode and " + valueColumn + " is not null order by " + valueColumn;
        MapSqlParameterSource params = new MapSqlParameterSource("styleCode", styleCode);
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> rs.getString("value"));
    }

    private String buildMatrixCodeSql(String alias, String codeColumnRef, String styleColumnRef, String typeExpr,
            String codeTypeColumn, String codeColumn, String codeNameColumn, boolean canJoinCodes) {
        StringBuilder sql = new StringBuilder("select ").append(codeColumnRef).append(" as code, ");
        if (canJoinCodes) {
            sql.append("coalesce(min(").append(alias).append(".").append(codeNameColumn).append("), ")
                    .append(codeColumnRef).append(") as name ");
        } else {
            sql.append(codeColumnRef).append(" as name ");
        }
        sql.append("from production_agreements pa ");
        if (canJoinCodes) {
            sql.append("left join codes ").append(alias).append(" on ").append(alias).append(".")
                    .append(codeTypeColumn).append(" = ").append(typeExpr).append(" and ").append(alias).append(".")
                    .append(codeColumn).append(" = ").append(codeColumnRef).append(" ");
        }
        sql.append("where ").append(styleColumnRef).append(" = :styleCode and ").append(codeColumnRef)
                .append(" is not null ");
        sql.append("group by ").append(codeColumnRef).append(" order by ").append(codeColumnRef);
        return sql.toString();
    }
}
