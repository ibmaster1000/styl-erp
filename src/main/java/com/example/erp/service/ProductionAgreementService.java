package com.example.erp.service;

import com.example.erp.controller.dto.AgreementCodeRow;
import com.example.erp.controller.dto.AgreementDetailRow;
import com.example.erp.controller.dto.AgreementDetailView;
import com.example.erp.controller.dto.PageInfo;
import com.example.erp.controller.dto.ProductionAgreementColorTotal;
import com.example.erp.repository.AgreementCodeRowView;
import com.example.erp.repository.AgreementDetailRowView;
import com.example.erp.repository.ProductionAgreementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ProductionAgreementService {

	private final ProductionAgreementRepository productionAgreementRepository;

	public ProductionAgreementService(ProductionAgreementRepository productionAgreementRepository) {
		this.productionAgreementRepository = productionAgreementRepository;
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

	public AgreementPageResult fetchAgreementPage(String styleCode, String agreementCode, Integer page,
			Integer size, String selectedAgreementCode) {
		String styleFilter = normalizeFilter(styleCode);
		String agreementFilter = normalizeFilter(agreementCode);
		String selectedFilter = normalizeFilter(selectedAgreementCode);
		int resolvedSize = size != null && size > 0 ? size : 25;
		long totalCount = productionAgreementRepository.countAgreementCodes(styleFilter, agreementFilter);
		int totalPages = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / resolvedSize);
		int resolvedPage = resolvePage(page, totalPages);
		int offset = Math.max(resolvedPage - 1, 0) * resolvedSize;

		List<AgreementCodeRowView> pageRows = productionAgreementRepository.findAgreementCodeRows(styleFilter,
				agreementFilter, resolvedSize, offset);
		List<AgreementCodeRow> leftRows = pageRows.stream()
				.map(row -> new AgreementCodeRow(valueOrDefault(row.getStyleCode()),
						valueOrDefault(row.getAgreementCode()),
						valueOrDefault(row.getColorName()),
						row.getTotalQuantity() != null ? row.getTotalQuantity() : 0,
						resolveManagerLabel(row.getProductionManager())))
				.collect(Collectors.toList());

		String resolvedSelected = StringUtils.hasText(selectedFilter) ? selectedFilter
				: leftRows.stream().map(AgreementCodeRow::getAgreementCode).findFirst().orElse(null);

		AgreementDetailView detailView = null;
		if (StringUtils.hasText(resolvedSelected)) {
			List<AgreementDetailRowView> detailRows = productionAgreementRepository
					.findAgreementDetailRows(resolvedSelected);
			detailView = buildAgreementDetailView(detailRows);
		}

		PageInfo pageInfo = new PageInfo(resolvedPage, resolvedSize, totalCount, totalPages);
		return new AgreementPageResult(leftRows, pageInfo, resolvedSelected, detailView);
	}

	private String normalizeFilter(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private AgreementDetailView buildAgreementDetailView(List<AgreementDetailRowView> detailRows) {
		if (detailRows == null || detailRows.isEmpty()) {
			return null;
		}
		List<AgreementDetailRow> rows = detailRows.stream()
				.filter(Objects::nonNull)
				.map(this::mapDetailRow)
				.sorted(Comparator.comparing(AgreementDetailRow::getColor)
						.thenComparing(AgreementDetailRow::getSize))
				.collect(Collectors.toList());

		Map<String, ProductionAgreementColorTotal> colorTotals = new TreeMap<>();
		rows.forEach(row -> {
			ProductionAgreementColorTotal current = colorTotals.get(row.getColor());
			int nextQuantity = row.getQuantity() + (current != null ? current.getTotalQuantity() : 0);
			BigDecimal nextAmount = row.getAmount()
					.add(current != null ? current.getTotalAmount() : BigDecimal.ZERO);
			colorTotals.put(row.getColor(),
					new ProductionAgreementColorTotal(row.getColor(), nextQuantity, nextAmount));
		});

		int grandQuantity = rows.stream().mapToInt(AgreementDetailRow::getQuantity).sum();
		BigDecimal grandAmount = rows.stream().map(AgreementDetailRow::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		AgreementDetailRowView first = detailRows.get(0);
		String styleCode = valueOrDefault(first.getStyleCode());
		String agreementCode = valueOrDefault(first.getAgreementCode());
		return new AgreementDetailView(styleCode, agreementCode, rows, new ArrayList<>(colorTotals.values()),
				new LinkedHashMap<>(colorTotals), grandQuantity, grandAmount);
	}

	private String valueOrDefault(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}

	private AgreementDetailRow mapDetailRow(AgreementDetailRowView view) {
		String color = resolveLabel(view.getColorName(), view.getColorCode());
		String size = resolveLabel(view.getSizeName(), view.getSizeCode());
		int quantity = view.getQuantity() != null ? view.getQuantity() : 0;
		BigDecimal supplyPrice = view.getSupplyPrice() != null ? view.getSupplyPrice() : BigDecimal.ZERO;
		BigDecimal amount = view.getAmount() != null ? view.getAmount()
				: supplyPrice.multiply(BigDecimal.valueOf(quantity));
		return new AgreementDetailRow(valueOrDefault(color), valueOrDefault(size), quantity, supplyPrice, amount);
	}

	private String resolveLabel(String name, String code) {
		if (StringUtils.hasText(name)) {
			return name;
		}
		return code;
	}

	private String resolveManagerLabel(String manager) {
		if (!StringUtils.hasText(manager)) {
			return "-";
		}
		return manager;
	}

	private int resolvePage(Integer requestedPage, int totalPages) {
		if (requestedPage == null || requestedPage < 1) {
			return totalPages > 0 ? 1 : 0;
		}
		if (totalPages > 0 && requestedPage > totalPages) {
			return totalPages;
		}
		return requestedPage;
	}

	public record AgreementPageResult(List<AgreementCodeRow> leftRows, PageInfo pageInfo,
			String selectedAgreementCode, AgreementDetailView detailView) {
	}
}
