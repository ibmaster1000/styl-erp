package com.example.erp.service;

import com.example.erp.controller.dto.AgreementLeftRow;
import com.example.erp.controller.dto.ProductionAgreementColorTotal;
import com.example.erp.controller.dto.ProductionAgreementDetailLine;
import com.example.erp.controller.dto.ProductionAgreementDetailView;
import com.example.erp.repository.ProductionAgreementRepository;
import com.example.erp.repository.ProductionAgreementView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ProductionAgreementService {

	private final ProductionAgreementRepository productionAgreementRepository;

	public ProductionAgreementService(ProductionAgreementRepository productionAgreementRepository) {
		this.productionAgreementRepository = productionAgreementRepository;
	}

	public List<ProductionAgreementView> findAll(String styleCode, String agreementCode) {
		String styleFilter = normalizeFilter(styleCode);
		String agreementFilter = normalizeFilter(agreementCode);
		return productionAgreementRepository.findAllWithCodeNamesFiltered(styleFilter, agreementFilter);
	}

	@Transactional
	public boolean updateCompletionStatus(String agreementCode, String colorCode, boolean completed) {
		if (!StringUtils.hasText(agreementCode)) {
			return false;
		}
		String status = completed ? "COMPLETED" : "CONFIRMED";
		String normalizedColor = StringUtils.hasText(colorCode) ? colorCode : null;
		int updated = productionAgreementRepository.updateStatusByAgreementCodeAndColorCode(agreementCode,
				normalizedColor, status);
		return updated > 0;
	}

	public List<AgreementLeftRow> buildLeftRows(List<ProductionAgreementView> agreements) {
		if (agreements == null || agreements.isEmpty()) {
			return Collections.emptyList();
		}

		Map<AgreementGroupKey, AgreementGroup> grouped = new LinkedHashMap<>();
		for (ProductionAgreementView item : agreements) {
			if (item == null) {
				continue;
			}
			String styleCode = valueOrDefault(item.getStyleCode());
			String agreementCode = valueOrDefault(item.getAgreementCode());
			String colorCode = valueOrDefault(resolveColorKey(item));
			String colorLabel = valueOrDefault(resolveDisplayColor(item));
			AgreementGroupKey key = new AgreementGroupKey(styleCode, agreementCode, colorCode);
			AgreementGroup group = grouped.computeIfAbsent(key,
					ignored -> new AgreementGroup(styleCode, agreementCode, colorLabel, colorCode,
							resolveManagerName(item.getProductEmpNo(), item.getProductEmpName())));
			group.addQuantity(Optional.ofNullable(item.getQuantity()).orElse(0));
			group.updateCompletion(item.getStatus());
		}

		List<AgreementLeftRow> result = new ArrayList<>();
		String lastStyleCode = null;
		String lastAgreementCode = null;
		for (AgreementGroup group : grouped.values()) {
			String displayStyleCode = group.styleCode();
			String displayAgreementCode = group.agreementCode();
			if (Objects.equals(lastStyleCode, group.styleCode())) {
				displayStyleCode = "";
			} else {
				lastStyleCode = group.styleCode();
				lastAgreementCode = null;
			}
			if (Objects.equals(lastAgreementCode, group.agreementCode())) {
				displayAgreementCode = "";
			} else {
				lastAgreementCode = group.agreementCode();
			}
			result.add(new AgreementLeftRow(group.styleCode(), group.agreementCode(), group.colorLabel(),
					group.colorCode(), group.totalQuantity(), group.managerName(), displayStyleCode,
					displayAgreementCode, group.isCompleted()));
		}

		return result;
	}

	public Map<String, ProductionAgreementDetailView> buildDetailViewsByColor(List<ProductionAgreementView> agreements,
			Map<String, Map<String, List<String>>> styleRules, Map<String, BigDecimal> supplyPrices) {
		if (agreements == null || agreements.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<String, List<ProductionAgreementView>> groupedByAgreement = agreements.stream().filter(Objects::nonNull)
				.collect(Collectors.groupingBy(
						item -> buildDetailKey(item.getAgreementCode(), resolveColorKey(item)),
						LinkedHashMap::new, Collectors.toList()));

		Map<String, ProductionAgreementDetailView> result = new LinkedHashMap<>();
		groupedByAgreement.forEach((detailKey, items) -> {
			String styleCode = items.stream().map(ProductionAgreementView::getStyleCode).filter(Objects::nonNull)
					.findFirst()
					.orElse("-");
			String agreementCode = items.stream().map(ProductionAgreementView::getAgreementCode)
					.filter(Objects::nonNull).findFirst().orElse("-");
			String colorCode = items.stream().map(this::resolveColorKey).filter(Objects::nonNull).findFirst().orElse("-");
			String colorName = items.stream().map(this::resolveDisplayColor).filter(StringUtils::hasText).findFirst()
					.orElse(colorCode);

			Map<String, List<String>> rule = styleRules.getOrDefault(styleCode, Collections.emptyMap());
			BigDecimal supplyPrice = supplyPrices.getOrDefault(styleCode, BigDecimal.ZERO);

			Map<String, String> colorNames = items.stream()
					.filter(item -> item.getColorCode() != null && item.getColorName() != null)
					.collect(Collectors.toMap(ProductionAgreementView::getColorCode, ProductionAgreementView::getColorName,
							(existing, replacement) -> existing));
			Map<String, String> sizeNames = items.stream()
					.filter(item -> item.getSizeCode() != null && item.getSizeName() != null)
					.collect(Collectors.toMap(ProductionAgreementView::getSizeCode, ProductionAgreementView::getSizeName,
							(existing, replacement) -> existing));

			List<ProductionAgreementDetailLine> detailLines = new ArrayList<>();
			List<String> ruleSizes = resolveRuleSizes(rule, colorCode, colorName);
			if (ruleSizes != null) {
				String colorLabel = valueOrDefault(resolveName(colorNames, colorCode));
				List<String> normalizedSizes = ruleSizes.isEmpty() ? List.of("-") : ruleSizes;
				normalizedSizes.forEach(size -> {
					int quantity = findQuantity(items, colorCode, size);
					String sizeLabel = valueOrDefault(resolveName(sizeNames, size));
					detailLines.add(createDetailLine(colorLabel, sizeLabel, quantity, supplyPrice));
				});
			} else if (!items.isEmpty()) {
				items.forEach(item -> {
					String colorLabel = valueOrDefault(resolveName(colorNames, item.getColorCode()));
					String sizeLabel = valueOrDefault(resolveName(sizeNames, item.getSizeCode()));
					detailLines.add(createDetailLine(colorLabel, sizeLabel,
							Optional.ofNullable(item.getQuantity()).orElse(0), supplyPrice));
				});
			} else {
				detailLines.add(createDetailLine("-", "-", 0, supplyPrice));
			}

			detailLines.sort(Comparator.comparing(ProductionAgreementDetailLine::getColor)
					.thenComparing(ProductionAgreementDetailLine::getSize));

			Map<String, ProductionAgreementColorTotal> colorTotals = new TreeMap<>();
			detailLines.forEach(line -> {
				ProductionAgreementColorTotal current = colorTotals.get(line.getColor());
				int nextQuantity = line.getQuantity() + (current != null ? current.getTotalQuantity() : 0);
				BigDecimal nextAmount = line.getAmount()
						.add(current != null ? current.getTotalAmount() : BigDecimal.ZERO);
				colorTotals.put(line.getColor(),
						new ProductionAgreementColorTotal(line.getColor(), nextQuantity, nextAmount));
			});

			int grandQuantity = detailLines.stream().mapToInt(ProductionAgreementDetailLine::getQuantity).sum();
			BigDecimal grandAmount = detailLines.stream().map(ProductionAgreementDetailLine::getAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add);

			result.put(detailKey, new ProductionAgreementDetailView(styleCode, agreementCode, detailLines,
					new ArrayList<>(colorTotals.values()), grandQuantity, grandAmount));
		});

		return result;
	}

	public Set<String> extractStyleCodes(List<ProductionAgreementView> agreements) {
		return agreements.stream().map(ProductionAgreementView::getStyleCode).filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	private String normalizeFilter(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private int findQuantity(List<ProductionAgreementView> items, String colorCode, String sizeCode) {
		return items.stream()
				.filter(item -> matches(item.getColorCode(), colorCode) && matches(item.getSizeCode(), sizeCode))
				.map(ProductionAgreementView::getQuantity).filter(Objects::nonNull).findFirst().orElse(0);
	}

	private String resolveColorKey(ProductionAgreementView item) {
		if (item == null) {
			return null;
		}
		if (StringUtils.hasText(item.getColorCode())) {
			return item.getColorCode();
		}
		return item.getColorName();
	}

	private String resolveDisplayColor(ProductionAgreementView item) {
		if (item == null) {
			return null;
		}
		if (StringUtils.hasText(item.getColorName())) {
			return item.getColorName();
		}
		return item.getColorCode();
	}

	private List<String> resolveRuleSizes(Map<String, List<String>> rule, String colorCode, String colorName) {
		if (rule == null || rule.isEmpty()) {
			return null;
		}
		if (StringUtils.hasText(colorCode) && rule.containsKey(colorCode)) {
			return Optional.ofNullable(rule.get(colorCode)).orElse(Collections.emptyList());
		}
		if (StringUtils.hasText(colorName) && rule.containsKey(colorName)) {
			return Optional.ofNullable(rule.get(colorName)).orElse(Collections.emptyList());
		}
		return null;
	}

	private String resolveName(Map<String, String> names, String code) {
		if (code == null) {
			return null;
		}
		return names.getOrDefault(code, code);
	}

	private boolean matches(String value, String expected) {
		if (value == null && expected == null) {
			return true;
		}
		if (value == null || expected == null) {
			return false;
		}
		return value.equalsIgnoreCase(expected);
	}

	private String valueOrDefault(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}

	private String resolveManagerName(String empNo, String empName) {
		if (!StringUtils.hasText(empNo)) {
			return "(미지정)";
		}
		if (StringUtils.hasText(empName)) {
			return empName;
		}
		return empNo;
	}

	private String buildDetailKey(String agreementCode, String colorCode) {
		return valueOrDefault(agreementCode) + "::" + valueOrDefault(colorCode);
	}

	private ProductionAgreementDetailLine createDetailLine(String color, String size, int quantity,
			BigDecimal supplyPrice) {
		BigDecimal effectiveSupply = supplyPrice != null ? supplyPrice : BigDecimal.ZERO;
		BigDecimal amount = effectiveSupply.multiply(BigDecimal.valueOf(quantity));
		return new ProductionAgreementDetailLine(valueOrDefault(color), valueOrDefault(size), quantity, effectiveSupply,
				amount);
	}

	private record AgreementGroupKey(String styleCode, String agreementCode, String colorCode) {
	}

	private static class AgreementGroup {
		private final String styleCode;
		private final String agreementCode;
		private final String colorLabel;
		private final String colorCode;
		private final String managerName;
		private int totalQuantity;
		private boolean completed;

		private AgreementGroup(String styleCode, String agreementCode, String colorLabel, String colorCode,
				String managerName) {
			this.styleCode = styleCode;
			this.agreementCode = agreementCode;
			this.colorLabel = colorLabel;
			this.colorCode = colorCode;
			this.managerName = managerName;
			this.totalQuantity = 0;
			this.completed = true;
		}

		private void addQuantity(int quantity) {
			this.totalQuantity += quantity;
		}

		private void updateCompletion(String status) {
			if (!"COMPLETED".equalsIgnoreCase(status)) {
				this.completed = false;
			}
		}

		private String styleCode() {
			return styleCode;
		}

		private String agreementCode() {
			return agreementCode;
		}

		private String colorLabel() {
			return colorLabel;
		}

		private String colorCode() {
			return colorCode;
		}

		private int totalQuantity() {
			return totalQuantity;
		}

		private String managerName() {
			return managerName;
		}

		private boolean isCompleted() {
			return completed;
		}
	}
}
