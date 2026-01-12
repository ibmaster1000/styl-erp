package com.example.erp.service;

import com.example.erp.controller.dto.AgreementCodeRow;
import com.example.erp.controller.dto.AgreementDetailColorGroup;
import com.example.erp.controller.dto.AgreementDetailRow;
import com.example.erp.controller.dto.AgreementDetailView;
import com.example.erp.controller.dto.PageInfo;
import com.example.erp.controller.dto.ProductionAgreementColorTotal;
import com.example.erp.repository.AgreementCodeRowView;
import com.example.erp.repository.AgreementDetailRowView;
import com.example.erp.domain.ProductionAgreement;
import com.example.erp.domain.Style;
import com.example.erp.repository.ProductionAgreementRepository;
import com.example.erp.repository.StyleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
	private final StyleRepository styleRepository;

	public ProductionAgreementService(ProductionAgreementRepository productionAgreementRepository,
			StyleRepository styleRepository) {
		this.productionAgreementRepository = productionAgreementRepository;
		this.styleRepository = styleRepository;
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
		return fetchAgreementPage(styleCode, agreementCode, page, size, selectedAgreementCode, null);
	}

	public AgreementPageResult fetchAgreementPage(String styleCode, String agreementCode, Integer page,
			Integer size, String selectedAgreementCode, String selectedColorCode) {
		String styleFilter = normalizeFilter(styleCode);
		String agreementFilter = normalizeFilter(agreementCode);
		String selectedFilter = normalizeFilter(selectedAgreementCode);
		String selectedColorFilter = normalizeFilter(selectedColorCode);
		int resolvedSize = size != null && size > 0 ? size : 25;
		int requestedPage = page == null || page < 1 ? 1 : page;
		Page<AgreementCodeRowView> pageResult = productionAgreementRepository.findAgreementCodeRows(styleFilter,
				agreementFilter, PageRequest.of(requestedPage - 1, resolvedSize));
		int totalPages = pageResult.getTotalPages();
		int resolvedPage = resolvePage(requestedPage, totalPages);
		if (resolvedPage != requestedPage && resolvedPage > 0) {
			pageResult = productionAgreementRepository.findAgreementCodeRows(styleFilter, agreementFilter,
					PageRequest.of(resolvedPage - 1, resolvedSize));
			totalPages = pageResult.getTotalPages();
		}
		long totalCount = pageResult.getTotalElements();
		List<AgreementCodeRowView> pageRows = pageResult.getContent();
		List<AgreementCodeRow> leftRows = buildLeftRows(pageRows);

		String resolvedSelected = StringUtils.hasText(selectedFilter) ? selectedFilter
				: leftRows.stream().map(AgreementCodeRow::getAgreementCode).findFirst().orElse(null);
		String resolvedColor = resolveSelectedColor(selectedColorFilter, resolvedSelected, leftRows);

		AgreementDetailView detailView = null;
		if (StringUtils.hasText(resolvedSelected)) {
			List<AgreementDetailRowView> detailRows = productionAgreementRepository
					.findAgreementDetailRows(resolvedSelected);
			detailView = buildAgreementDetailView(detailRows);
		}

		PageInfo pageInfo = new PageInfo(resolvedPage, resolvedSize, totalCount, totalPages);
		return new AgreementPageResult(leftRows, pageInfo, resolvedSelected, resolvedColor, detailView);
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
		Map<String, AgreementDetailColorGroup> colorGroups = new LinkedHashMap<>();
		rows.forEach(row -> {
			ProductionAgreementColorTotal current = colorTotals.get(row.getColor());
			int nextQuantity = row.getQuantity() + (current != null ? current.getTotalQuantity() : 0);
			BigDecimal nextAmount = row.getAmount()
					.add(current != null ? current.getTotalAmount() : BigDecimal.ZERO);
			colorTotals.put(row.getColor(),
					new ProductionAgreementColorTotal(row.getColor(), nextQuantity, nextAmount));
			colorGroups.computeIfAbsent(row.getColor(),
					color -> new AgreementDetailColorGroup(color, new ArrayList<>(), 0, BigDecimal.ZERO))
					.getRows().add(row);
		});

		List<AgreementDetailColorGroup> groupedRows = colorGroups.values().stream()
				.map(group -> {
					int totalQuantity = group.getRows().stream().mapToInt(AgreementDetailRow::getQuantity).sum();
					BigDecimal totalAmount = group.getRows().stream().map(AgreementDetailRow::getAmount)
							.reduce(BigDecimal.ZERO, BigDecimal::add);
					return new AgreementDetailColorGroup(group.getColor(), group.getRows(), totalQuantity, totalAmount);
				})
				.collect(Collectors.toList());

		int grandQuantity = groupedRows.stream().mapToInt(AgreementDetailColorGroup::getTotalQuantity).sum();
		BigDecimal grandAmount = groupedRows.stream().map(AgreementDetailColorGroup::getTotalAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		AgreementDetailRowView first = detailRows.get(0);
		String styleCode = valueOrDefault(first.getStyleCode());
		String agreementCode = valueOrDefault(first.getAgreementCode());
		return new AgreementDetailView(styleCode, agreementCode, rows, groupedRows,
				new ArrayList<>(colorTotals.values()),
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
		return new AgreementDetailRow(valueOrDefault(color), view.getColorCode(),
				valueOrDefault(size), view.getSizeCode(), quantity, supplyPrice, amount);
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

	private List<AgreementCodeRow> buildLeftRows(List<AgreementCodeRowView> pageRows) {
		List<AgreementCodeRow> leftRows = new ArrayList<>();
		String lastStyleCode = null;
		String lastAgreementCode = null;
		for (AgreementCodeRowView row : pageRows) {
			String styleCode = valueOrDefault(row.getStyleCode());
			String agreementCode = valueOrDefault(row.getAgreementCode());
			String colorCode = row.getColorCode();
			String displayStyleCode = Objects.equals(styleCode, lastStyleCode) ? "" : styleCode;
			String displayAgreementCode = Objects.equals(agreementCode, lastAgreementCode) ? "" : agreementCode;
			leftRows.add(new AgreementCodeRow(styleCode, agreementCode, displayStyleCode, displayAgreementCode,
					colorCode,
					valueOrDefault(row.getColorName()),
					row.getTotalQuantity() != null ? row.getTotalQuantity() : 0,
					resolveManagerLabel(row.getProductionManagerName())));
			lastStyleCode = styleCode;
			lastAgreementCode = agreementCode;
		}
		return leftRows;
	}

	private String resolveSelectedColor(String selectedColorCode, String selectedAgreementCode,
			List<AgreementCodeRow> leftRows) {
		if (StringUtils.hasText(selectedColorCode)) {
			return selectedColorCode;
		}
		if (!StringUtils.hasText(selectedAgreementCode)) {
			return leftRows.stream().findFirst().map(AgreementCodeRow::getColorCode).orElse(null);
		}
		return leftRows.stream()
				.filter(row -> Objects.equals(row.getAgreementCode(), selectedAgreementCode))
				.map(AgreementCodeRow::getColorCode)
				.findFirst()
				.orElse(null);
	}

	private int resolvePage(Integer requestedPage, int totalPages) {
		if (totalPages == 0) {
			return 0;
		}
		if (requestedPage == null || requestedPage < 1) {
			return 1;
		}
		if (totalPages > 0 && requestedPage > totalPages) {
			return totalPages;
		}
		return requestedPage;
	}

	public record AgreementPageResult(List<AgreementCodeRow> leftRows, PageInfo pageInfo,
			String selectedAgreementCode, String selectedColorCode, AgreementDetailView detailView) {
	}

	@Transactional
	public void createAgreement(String agreementCode, String styleCode, String colorType, String colorCode,
			String sizeType, String sizeCode, Integer quantity, String productionManager) {
		if (!StringUtils.hasText(agreementCode) || !StringUtils.hasText(styleCode)
				|| !StringUtils.hasText(colorCode) || !StringUtils.hasText(sizeCode) || quantity == null) {
			throw new IllegalArgumentException("필수 입력값이 누락되었습니다.");
		}
		Style style = styleRepository.findByStyleCode(styleCode)
				.orElseThrow(() -> new IllegalArgumentException("품번을 찾을 수 없습니다."));
		ProductionAgreement agreement = new ProductionAgreement();
		agreement.setAgreementCode(agreementCode.trim());
		agreement.setStyleCode(styleCode.trim());
		agreement.setStylesId(style.getStylesId());
		agreement.setColorType(normalizeFilter(colorType));
		agreement.setColorCode(colorCode.trim());
		agreement.setSizeType(normalizeFilter(sizeType));
		agreement.setSizeCode(sizeCode.trim());
		agreement.setQuantity(quantity);
		agreement.setProductionManager(normalizeFilter(productionManager));
		agreement.setStatus("CONFIRMED");
		productionAgreementRepository.save(agreement);
	}

	@Transactional
	public int updateAgreementQuantities(String agreementCode, List<String> colorCodes, List<String> sizeCodes,
			List<Integer> quantities) {
		if (!StringUtils.hasText(agreementCode)) {
			throw new IllegalArgumentException("생산합의 코드가 없습니다.");
		}
		if (colorCodes == null || sizeCodes == null || quantities == null
				|| colorCodes.isEmpty() || sizeCodes.isEmpty() || quantities.isEmpty()) {
			throw new IllegalArgumentException("수정할 항목이 없습니다.");
		}
		if (colorCodes.size() != sizeCodes.size() || colorCodes.size() != quantities.size()) {
			throw new IllegalArgumentException("수정 요청 데이터가 일치하지 않습니다.");
		}
		int updated = 0;
		for (int index = 0; index < colorCodes.size(); index++) {
			String colorCode = normalizeFilter(colorCodes.get(index));
			String sizeCode = normalizeFilter(sizeCodes.get(index));
			Integer quantity = quantities.get(index);
			if (!StringUtils.hasText(colorCode) || !StringUtils.hasText(sizeCode) || quantity == null) {
				continue;
			}
			updated += productionAgreementRepository.updateQuantityByAgreementCodeAndColorCodeAndSizeCode(
					agreementCode, colorCode, sizeCode, quantity);
		}
		return updated;
	}

	@Transactional
	public int deleteAgreement(String agreementCode, String colorCode) {
		if (!StringUtils.hasText(agreementCode)) {
			throw new IllegalArgumentException("삭제할 생산합의 코드가 없습니다.");
		}
		String normalizedColor = StringUtils.hasText(colorCode) ? colorCode.trim() : null;
		return productionAgreementRepository.deleteByAgreementCodeAndColorCode(agreementCode.trim(), normalizedColor);
	}
}
