package com.example.erp.service;

import com.example.erp.controller.dto.ProductionAgreementColorTotal;
import com.example.erp.controller.dto.ProductionAgreementDetailLine;
import com.example.erp.controller.dto.ProductionAgreementDetailView;
import com.example.erp.domain.ProductionAgreement;
import com.example.erp.repository.ProductionAgreementRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ProductionAgreementService {

	private final ProductionAgreementRepository productionAgreementRepository;

	public ProductionAgreementService(ProductionAgreementRepository productionAgreementRepository) {
		this.productionAgreementRepository = productionAgreementRepository;
	}

	public List<ProductionAgreement> findAll() {
		return productionAgreementRepository.findAll();
	}

	public Map<String, ProductionAgreementDetailView> buildDetailViews(List<ProductionAgreement> agreements,
			Map<String, Map<String, List<String>>> styleRules, Map<String, BigDecimal> supplyPrices) {
		if (agreements == null || agreements.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<String, List<ProductionAgreement>> groupedByAgreement = agreements.stream().filter(Objects::nonNull)
				.collect(Collectors.groupingBy(ProductionAgreement::getAgreementCode, LinkedHashMap::new,
						Collectors.toList()));

		Map<String, ProductionAgreementDetailView> result = new LinkedHashMap<>();
		groupedByAgreement.forEach((agreementCode, items) -> {
			String styleNo = items.stream().map(ProductionAgreement::getStyleNo).filter(Objects::nonNull).findFirst()
					.orElse("-");

			Map<String, List<String>> rule = styleRules.getOrDefault(styleNo, Collections.emptyMap());
			BigDecimal supplyPrice = supplyPrices.getOrDefault(styleNo, BigDecimal.ZERO);

			List<ProductionAgreementDetailLine> detailLines = new ArrayList<>();
			if (!rule.isEmpty()) {
				rule.forEach((color, sizes) -> {
					List<String> normalizedSizes = sizes != null ? sizes : List.of("-");
					normalizedSizes.forEach(size -> {
						int quantity = findQuantity(items, color, size);
						detailLines.add(createDetailLine(color, size, quantity, supplyPrice));
					});
				});
			} else if (!items.isEmpty()) {
				items.forEach(item -> detailLines
						.add(createDetailLine(valueOrDefault(item.getColor()), valueOrDefault(item.getSize()),
								item.getQuantity() != null ? item.getQuantity() : 0, supplyPrice)));
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

			result.put(agreementCode, new ProductionAgreementDetailView(styleNo, agreementCode, detailLines,
					new ArrayList<>(colorTotals.values()), grandQuantity, grandAmount));
		});

		return result;
	}

	public Set<String> extractStyleNos(List<ProductionAgreement> agreements) {
		return agreements.stream().map(ProductionAgreement::getStyleNo).filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	private int findQuantity(List<ProductionAgreement> items, String color, String size) {
		return items.stream().filter(item -> matches(item.getColor(), color) && matches(item.getSize(), size))
				.map(ProductionAgreement::getQuantity).filter(Objects::nonNull).findFirst().orElse(0);
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