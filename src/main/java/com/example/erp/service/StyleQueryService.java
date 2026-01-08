package com.example.erp.service;

import com.example.erp.controller.dto.StyleListRow;
import com.example.erp.controller.dto.StyleSearchCondition;
import com.example.erp.controller.dto.StyleSearchResult;
import com.example.erp.repository.StyleQueryRepository;
import com.example.erp.repository.StyleQueryRepository.CodeKey;
import com.example.erp.repository.StyleQueryRepository.StyleRuleRow;
import com.example.erp.repository.StyleQueryRepository.StyleSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StyleQueryService {

    private final StyleQueryRepository repository;

    public StyleQueryService(StyleQueryRepository repository) {
        this.repository = repository;
    }

    public StyleSearchResult search(StyleSearchCondition condition) {
        List<StyleSnapshot> styles = repository.findStyles(condition);
        if (styles.isEmpty()) {
            return new StyleSearchResult(Collections.emptyList(), Collections.emptyMap());
        }

        Map<String, String> itemNames = loadItemNames(styles);
        Map<String, String> userNames = resolveUserNames(styles);
        Map<String, Map<String, List<String>>> stylesRule = buildStylesRule(styles);

        List<StyleListRow> rows = new ArrayList<>();
        for (StyleSnapshot style : styles) {
            String styleKey = resolveStyleKey(style);
            String item = resolveItem(style, itemNames);
            String colors = joinRuleValues(stylesRule.get(styleKey));
            String sizes = joinSizeValues(stylesRule.get(styleKey));
            String designer = resolveUserName(style.designerEmpNo(), userNames);
            String production = resolveUserName(style.productEmpNo(), userNames);
            String sales = resolveUserName(style.salesEmpNo(), userNames);
            String logistic = resolveUserName(style.logisticEmpNo(), userNames);
            BigDecimal amount = style.salesPrice() != null ? style.salesPrice() : style.supplyPrice();
            boolean active = style.active() == null || style.active();

            rows.add(new StyleListRow(styleKey, item, colors, sizes, designer, production, sales, logistic,
                    amount, style.startDate(), active));
        }

        if (StringUtils.hasText(condition.getDesignerNameLike())) {
            String keyword = condition.getDesignerNameLike().trim().toLowerCase(Locale.KOREAN);
            rows = rows.stream()
                    .filter(row -> row.getDesigner() != null
                            && row.getDesigner().toLowerCase(Locale.KOREAN).contains(keyword))
                    .collect(Collectors.toList());
        }

        sortRows(rows, condition);

        return new StyleSearchResult(rows, stylesRule);
    }

    private Map<String, String> loadItemNames(List<StyleSnapshot> styles) {
        Set<String> itemCodes = styles.stream()
                .map(StyleSnapshot::itemCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (itemCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return repository.findItemNames(itemCodes);
    }

    private Map<String, String> resolveUserNames(List<StyleSnapshot> styles) {
        Set<String> rawValues = new LinkedHashSet<>();
        for (StyleSnapshot style : styles) {
            collect(rawValues, style.designerEmpNo());
            collect(rawValues, style.productEmpNo());
            collect(rawValues, style.salesEmpNo());
            collect(rawValues, style.logisticEmpNo());
        }
        if (rawValues.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> numericValues = new LinkedHashSet<>();
        Set<String> textValues = new LinkedHashSet<>();
        for (String value : rawValues) {
            if (isNumeric(value)) {
                numericValues.add(value);
            } else {
                textValues.add(value);
            }
        }

        Map<String, String> resolved = new LinkedHashMap<>();
        if (rawValues.stream().anyMatch(value -> "admin".equalsIgnoreCase(value))) {
            Map<String, String> adminName = repository.findUserNamesByUsernames(Set.of("admin"));
            if (!adminName.isEmpty()) {
                resolved.put("admin", adminName.getOrDefault("admin", "관리자"));
                resolved.put("ADMIN", adminName.getOrDefault("admin", "관리자"));
            } else {
                resolved.put("admin", "관리자");
                resolved.put("ADMIN", "관리자");
            }
        }

        if (!numericValues.isEmpty()) {
            resolved.putAll(repository.findUserNamesByEmpNos(numericValues));
        }

        if (!textValues.isEmpty()) {
            resolved.putAll(repository.findUserNamesByUsernames(textValues));
            resolved.putAll(repository.findUserNamesByUserIds(textValues));
            resolved.putAll(repository.findUserNamesByEmpCodes(textValues));
        }

        return resolved;
    }

    private Map<String, Map<String, List<String>>> buildStylesRule(List<StyleSnapshot> styles) {
        Set<String> styleIds = styles.stream()
                .map(StyleSnapshot::stylesId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (styleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<StyleRuleRow> rules = repository.findStyleRules(styleIds);
        if (rules.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<CodeKey> codeKeys = new LinkedHashSet<>();
        for (StyleRuleRow rule : rules) {
            if (StringUtils.hasText(rule.codeType()) && StringUtils.hasText(rule.code())) {
                codeKeys.add(new CodeKey(rule.codeType(), rule.code()));
            }
        }
        Map<CodeKey, String> codeNames = repository.findCodeNames(codeKeys);

        Map<String, List<String>> colorsByStyle = new LinkedHashMap<>();
        Map<String, List<String>> sizesByStyle = new LinkedHashMap<>();

        for (StyleRuleRow rule : rules) {
            String type = rule.codeType();
            String styleId = rule.stylesId();
            String displayName = codeNames.getOrDefault(new CodeKey(type, rule.code()), rule.code());

            if ("COLOR".equalsIgnoreCase(type)) {
                colorsByStyle.computeIfAbsent(styleId, key -> new ArrayList<>()).add(displayName);
            } else if ("SIZE".equalsIgnoreCase(type)) {
                sizesByStyle.computeIfAbsent(styleId, key -> new ArrayList<>()).add(displayName);
            }
        }

        Map<String, Map<String, List<String>>> result = new LinkedHashMap<>();
        for (StyleSnapshot style : styles) {
            String styleId = style.stylesId();
            List<String> colors = colorsByStyle.getOrDefault(styleId, Collections.emptyList());
            List<String> sizes = sizesByStyle.getOrDefault(styleId, Collections.emptyList());

            if (colors.isEmpty() && sizes.isEmpty()) {
                continue;
            }
            List<String> effectiveColors = colors.isEmpty() ? List.of("-") : colors;
            List<String> effectiveSizes = sizes.isEmpty() ? List.of("-") : sizes;

            Map<String, List<String>> colorMap = new LinkedHashMap<>();
            for (String color : effectiveColors) {
                colorMap.put(color, effectiveSizes);
            }
            result.put(resolveStyleKey(style), colorMap);
        }

        return result;
    }

    private String resolveStyleKey(StyleSnapshot style) {
        if (StringUtils.hasText(style.styleCode())) {
            return style.styleCode();
        }
        return style.stylesId();
    }

    private String resolveItem(StyleSnapshot style, Map<String, String> itemNames) {
        if (StringUtils.hasText(style.itemName())) {
            return style.itemName();
        }
        if (StringUtils.hasText(style.itemCode())) {
            return itemNames.getOrDefault(style.itemCode(), style.itemCode());
        }
        return "-";
    }

    private String resolveUserName(String raw, Map<String, String> userNames) {
        if (!StringUtils.hasText(raw)) {
            return "-";
        }
        if ("admin".equalsIgnoreCase(raw)) {
            return userNames.getOrDefault(raw, "관리자");
        }
        return userNames.getOrDefault(raw, raw);
    }

    private String joinRuleValues(Map<String, List<String>> rule) {
        if (rule == null || rule.isEmpty()) {
            return "-";
        }
        return String.join(", ", rule.keySet());
    }

    private String joinSizeValues(Map<String, List<String>> rule) {
        if (rule == null || rule.isEmpty()) {
            return "-";
        }
        Set<String> sizes = new LinkedHashSet<>();
        for (List<String> value : rule.values()) {
            sizes.addAll(value);
        }
        if (sizes.isEmpty()) {
            return "-";
        }
        return String.join(", ", sizes);
    }

    private void sortRows(List<StyleListRow> rows, StyleSearchCondition condition) {
        if (rows.isEmpty()) {
            return;
        }
        String sort = normalizeSort(condition.getSort());
        if (!StringUtils.hasText(sort)) {
            return;
        }

        Comparator<StyleListRow> comparator;
        boolean desc = isDesc(condition.getDirection());
        if ("production".equals(sort)) {
            comparator = Comparator.comparing(StyleListRow::getProductionManager, this::compareNullableText);
        } else if ("sales".equals(sort)) {
            comparator = Comparator.comparing(StyleListRow::getSalesManager, this::compareNullableText);
        } else if ("transport".equals(sort)) {
            comparator = Comparator.comparing(StyleListRow::getTransportManager, this::compareNullableText);
        } else if ("amount".equals(sort)) {
            comparator = Comparator.comparing(StyleListRow::getAmount, this::compareNullableAmount);
            desc = !StringUtils.hasText(condition.getDirection()) || desc;
        } else {
            return;
        }

        if (desc) {
            comparator = comparator.reversed();
        }
        rows.sort(comparator);
    }

    private String normalizeSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return null;
        }
        return switch (sort.trim()) {
            case "productionManager", "product", "production" -> "production";
            case "salesManager", "sales" -> "sales";
            case "transportManager", "logistic", "transport" -> "transport";
            case "amount" -> "amount";
            default -> null;
        };
    }

    private boolean isDesc(String direction) {
        return direction != null && direction.equalsIgnoreCase("desc");
    }

    private int compareNullableText(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareToIgnoreCase(right);
    }

    private int compareNullableAmount(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    private void collect(Set<String> target, String value) {
        if (StringUtils.hasText(value)) {
            target.add(value.trim());
        }
    }

    private boolean isNumeric(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}