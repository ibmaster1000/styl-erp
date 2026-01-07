package com.example.erp.service;

import com.example.erp.controller.dto.StyleRegisterRequest;
import com.example.erp.domain.Item;
import com.example.erp.domain.Style;
import com.example.erp.domain.StylesRule;
import com.example.erp.repository.ItemRepository;
import com.example.erp.repository.StyleRepository;
import com.example.erp.repository.StylesRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class StyleRegistrationService {

    private static final DateTimeFormatter STYLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMM", Locale.KOREA);

    private final ItemRepository itemRepository;
    private final StyleRepository styleRepository;
    private final StylesRuleRepository stylesRuleRepository;

    public StyleRegistrationService(ItemRepository itemRepository,
                                    StyleRepository styleRepository,
                                    StylesRuleRepository stylesRuleRepository) {
        this.itemRepository = itemRepository;
        this.styleRepository = styleRepository;
        this.stylesRuleRepository = stylesRuleRepository;
    }

    public Style createStyleWithRules(StyleRegisterRequest request) {
        validate(request);
        Item item = itemRepository.findById(request.getItemCode())
                .filter(Item::isActive)
                .orElseThrow(() -> new IllegalArgumentException("유효한 아이템을 선택해주세요."));

        String prefix = item.getItemCode() + request.getStartDate().format(STYLE_DATE_FORMATTER);
        String styleCode = nextStyleCode(prefix);

        Style style = new Style();
        style.setStyleCode(styleCode);
        style.setItemCode(item.getItemCode());
        style.setItemName(item.getItemName());
        style.setDesignerEmpNo(normalize(request.getDesignerEmpNo()));
        style.setProductEmpNo(normalize(request.getProductEmpNo()));
        style.setSalesEmpNo(normalize(request.getSalesEmpNo()));
        style.setLogisticEmpNo(normalize(request.getLogisticEmpNo()));
        style.setStartDate(request.getStartDate());
        style.setCostPrice(request.getCostPrice());
        style.setProductionCost(request.getProductionCost());
        style.setSupplyPrice(request.getSupplyPrice());
        style.setSalesPrice(request.getSalesPrice());
        style.setActive(true);

        Style savedStyle = styleRepository.save(style);
        saveRules(savedStyle.getStylesId(), "COLOR", normalizeCodes(request.getColorCodes()));
        saveRules(savedStyle.getStylesId(), "SIZE", normalizeCodes(request.getSizeCodes()));
        return savedStyle;
    }

    @Transactional(readOnly = true)
    public List<Item> findActiveItems() {
        return itemRepository.findByIsActiveTrueOrderByItemCodeAsc();
    }

    private String nextStyleCode(String prefix) {
        Optional<Style> existing = styleRepository.findTopByStyleCodeStartingWithOrderByStyleCodeDesc(prefix);
        int nextSequence = 1;
        if (existing.isPresent() && StringUtils.hasText(existing.get().getStyleCode())) {
            String existingCode = existing.get().getStyleCode();
            if (existingCode.length() >= prefix.length() + 3) {
                String suffix = existingCode.substring(existingCode.length() - 3);
                try {
                    nextSequence = Integer.parseInt(suffix) + 1;
                } catch (NumberFormatException ignored) {
                    nextSequence = 1;
                }
            }
        }
        return prefix + String.format("%03d", nextSequence);
    }

    private void saveRules(Long stylesId, String codeType, List<String> codes) {
        if (codes.isEmpty()) {
            return;
        }
        List<StylesRule> rules = new ArrayList<>();
        for (String code : codes) {
            StylesRule rule = new StylesRule();
            rule.setStylesId(stylesId);
            rule.setCodeType(codeType);
            rule.setCode(code);
            rules.add(rule);
        }
        stylesRuleRepository.saveAll(rules);
    }

    private List<String> normalizeCodes(List<String> codes) {
        Set<String> deduped = new LinkedHashSet<>();
        if (codes != null) {
            for (String code : codes) {
                if (StringUtils.hasText(code)) {
                    deduped.add(code.trim());
                }
            }
        }
        return new ArrayList<>(deduped);
    }

    private void validate(StyleRegisterRequest request) {
        if (request == null || !StringUtils.hasText(request.getItemCode())) {
            throw new IllegalArgumentException("아이템을 선택해주세요.");
        }
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("시작일을 입력해주세요.");
        }
        requireNonNegative(request.getCostPrice(), "원가");
        requireNonNegative(request.getProductionCost(), "생산원가");
        requireNonNegative(request.getSupplyPrice(), "공장출고가");
        requireNonNegative(request.getSalesPrice(), "판매가");

        if (normalizeCodes(request.getColorCodes()).isEmpty()) {
            throw new IllegalArgumentException("색상 코드를 1개 이상 선택해주세요.");
        }
        if (normalizeCodes(request.getSizeCodes()).isEmpty()) {
            throw new IllegalArgumentException("사이즈 코드를 1개 이상 선택해주세요.");
        }
    }

    private void requireNonNegative(BigDecimal value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + "을(를) 입력해주세요.");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(label + "은(는) 0 이상이어야 합니다.");
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
