package com.example.erp.service;

import com.example.erp.controller.dto.StyleListRow;
import com.example.erp.controller.dto.StyleSearchCondition;
import com.example.erp.controller.dto.StyleSearchResult;
import com.example.erp.repository.StyleQueryRepository;
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

		List<StyleListRow> rows = new ArrayList<>();
		for (StyleSnapshot style : styles) {
			String styleKey = resolveStyleKey(style);
			String item = resolveItem(style, itemNames);
			String colors = normalizeListValue(style.colors());
			String sizes = normalizeListValue(style.sizes());
			String designer = resolveUserName(style.designerEmpNo(), userNames);
			String production = resolveUserName(style.productEmpNo(), userNames);
			String sales = resolveUserName(style.salesEmpNo(), userNames);
			String logistic = resolveUserName(style.logisticEmpNo(), userNames);
			boolean active = style.active() == null || style.active();
		
			rows.add(new StyleListRow(styleKey, item, colors, sizes, designer, production, sales, logistic,
					style.productionCost(), style.supplyPrice(), style.salesPrice(), style.startDate(), active));
		}

		sortRows(rows, condition);

		return new StyleSearchResult(rows, Collections.emptyMap());
	}

	private Map<String, String> loadItemNames(List<StyleSnapshot> styles) {
		Set<String> itemCodes = styles.stream().map(StyleSnapshot::itemCode).filter(StringUtils::hasText)
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

	private String normalizeListValue(String value) {
		if (!StringUtils.hasText(value)) {
			return "-";
		}
		return value;
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
			comparator = Comparator.comparing(StyleListRow::getLogisticManager, this::compareNullableText);
		} else if ("amount".equals(sort)) {
			comparator = Comparator.comparing(StyleListRow::getSalesPrice, this::compareNullableAmount);
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
