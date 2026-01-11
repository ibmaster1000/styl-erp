package com.example.erp.service;

import com.example.erp.controller.dto.ProductionAgreementColorTotal;
import com.example.erp.controller.dto.ProductionAgreementDetailLine;
import com.example.erp.controller.dto.ProductionAgreementDetailView;
import com.example.erp.repository.ProductionAgreementRepository;
import com.example.erp.repository.ProductionAgreementView;
import org.springframework.stereotype.Service;

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

	public List<ProductionAgreementView> findAll() {
		return productionAgreementRepository.findAllWithCodeNames();
	}

	public Map<String, ProductionAgreementDetailView> buildDetailViews(List<ProductionAgreementView> agreements,
			Map<String, Map<String, List<String>>> styleRules, Map<String, BigDecimal> supplyPrices) {
		if (agreements == null || agreements.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<String, List<ProductionAgreementView>> groupedByAgreement = agreements.stream().filter(Objects::nonNull)
				.collect(Collectors.groupingBy(ProductionAgreementView::getAgreementCode, LinkedHashMap::new,
						Collectors.toList()));

		Map<String, ProductionAgreementDetailView> result = new LinkedHashMap<>();
		groupedByAgreement.forEach((agreementCode, items) -> {
			String styleCode = items.stream().map(ProductionAgreementView::getStyleCode).filter(Objects::nonNull)
					.findFirst()
					.orElse("-");

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
			if (!rule.isEmpty()) {
				rule.forEach((color, sizes) -> {
					List<String> normalizedSizes = sizes != null ? sizes : List.of("-");
					normalizedSizes.forEach(size -> {
						int quantity = findQuantity(items, color, size);
						String colorLabel = valueOrDefault(resolveName(colorNames, color));
						String sizeLabel = valueOrDefault(resolveName(sizeNames, size));
						detailLines.add(createDetailLine(colorLabel, sizeLabel, quantity, supplyPrice));
					});
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

			result.put(agreementCode, new ProductionAgreementDetailView(styleCode, agreementCode, detailLines,
					new ArrayList<>(colorTotals.values()), grandQuantity, grandAmount));
		});

		return result;
	}

	public Set<String> extractStyleCodes(List<ProductionAgreementView> agreements) {
		return agreements.stream().map(ProductionAgreementView::getStyleCode).filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	private int findQuantity(List<ProductionAgreementView> items, String colorCode, String sizeCode) {
		return items.stream()
				.filter(item -> matches(item.getColorCode(), colorCode) && matches(item.getSizeCode(), sizeCode))
				.map(ProductionAgreementView::getQuantity).filter(Objects::nonNull).findFirst().orElse(0);
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

	private ProductionAgreementDetailLine createDetailLine(String color, String size, int quantity,
			BigDecimal supplyPrice) {
		BigDecimal effectiveSupply = supplyPrice != null ? supplyPrice : BigDecimal.ZERO;
		BigDecimal amount = effectiveSupply.multiply(BigDecimal.valueOf(quantity));
		return new ProductionAgreementDetailLine(valueOrDefault(color), valueOrDefault(size), quantity, effectiveSupply,
				amount);
	}
}
