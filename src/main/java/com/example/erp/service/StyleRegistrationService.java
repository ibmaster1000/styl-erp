package com.example.erp.service;

import com.example.erp.controller.dto.StyleRegisterRequest;
import com.example.erp.domain.Item;
import com.example.erp.domain.Style;
import com.example.erp.domain.StylesRule;
import com.example.erp.domain.StylesRuleId;
import com.example.erp.repository.ItemRepository;
import com.example.erp.repository.StyleRepository;
import com.example.erp.repository.StylesRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class StyleRegistrationService {

    private final StyleRepository styleRepository;
    private final ItemRepository itemRepository;
    private final StylesRuleRepository stylesRuleRepository;

    public StyleRegistrationService(
            StyleRepository styleRepository,
            ItemRepository itemRepository,
            StylesRuleRepository stylesRuleRepository) {
        this.styleRepository = styleRepository;
        this.itemRepository = itemRepository;
        this.stylesRuleRepository = stylesRuleRepository;
    }

    public Style create(StyleRegisterRequest req) {
        validateRequest(req);

        Item item = itemRepository.findById(req.getItemCode())
                .filter(found -> Integer.valueOf(1).equals(found.getIsActive()))
                .orElseThrow(() -> new IllegalArgumentException("active item not found"));

        String styleCode = generateStyleCode(req.getItemCode(), req.getStartDate());

        Style style = new Style();
        style.setStyleCode(styleCode);
        style.setItemCode(item.getItemCode());
        style.setItemName(item.getItemName());
        style.setStartDate(req.getStartDate());
        style.setCostPrice(req.getCostPrice());
        style.setProductionCost(req.getProductionCost());
        style.setSupplyPrice(req.getSupplyPrice());
        style.setSalesPrice(req.getSalesPrice());
        style.setDesignerEmpNo(req.getDesignerEmpNo());
        style.setProductEmpNo(req.getProductEmpNo());
        style.setSalesEmpNo(req.getSalesEmpNo());
        style.setLogisticEmpNo(req.getLogisticEmpNo());
        style.setIsActive(1);

        Style savedStyle = styleRepository.save(style);
        saveRules(savedStyle.getStylesId(), req.getColorCodes(), req.getSizeCodes());

        return savedStyle;
    }

    private void validateRequest(StyleRegisterRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (!StringUtils.hasText(req.getItemCode())) {
            throw new IllegalArgumentException("itemCode is required");
        }
        if (req.getStartDate() == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (req.getCostPrice() == null) {
            throw new IllegalArgumentException("costPrice is required");
        }
        if (req.getProductionCost() == null) {
            throw new IllegalArgumentException("productionCost is required");
        }
        if (req.getSupplyPrice() == null) {
            throw new IllegalArgumentException("supplyPrice is required");
        }
        if (req.getSalesPrice() == null) {
            throw new IllegalArgumentException("salesPrice is required");
        }
        if (!StringUtils.hasText(req.getDesignerEmpNo())) {
            throw new IllegalArgumentException("designerEmpNo is required");
        }
        if (!StringUtils.hasText(req.getProductEmpNo())) {
            throw new IllegalArgumentException("productEmpNo is required");
        }
        if (!StringUtils.hasText(req.getSalesEmpNo())) {
            throw new IllegalArgumentException("salesEmpNo is required");
        }
        if (!StringUtils.hasText(req.getLogisticEmpNo())) {
            throw new IllegalArgumentException("logisticEmpNo is required");
        }
        if (req.getColorCodes() == null || req.getColorCodes().isEmpty()) {
            throw new IllegalArgumentException("colorCodes is required");
        }
        if (req.getSizeCodes() == null || req.getSizeCodes().isEmpty()) {
            throw new IllegalArgumentException("sizeCodes is required");
        }
    }

    private String generateStyleCode(String itemCode, java.time.LocalDate startDate) {
        String yymm = String.format("%02d%02d", startDate.getYear() % 100, startDate.getMonthValue());
        String prefix = itemCode + yymm;

        Optional<Style> last = styleRepository.findTopByStyleCodeStartingWithOrderByStyleCodeDesc(prefix);
        int nextSeq = last.map(found -> parseSequence(found.getStyleCode()) + 1).orElse(1);

        return prefix + String.format("%03d", nextSeq);
    }

    private int parseSequence(String styleCode) {
        if (!StringUtils.hasText(styleCode) || styleCode.length() < 3) {
            throw new IllegalArgumentException("invalid styleCode sequence");
        }
        String seq = styleCode.substring(styleCode.length() - 3);
        try {
            return Integer.parseInt(seq);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("invalid styleCode sequence", ex);
        }
    }

    private void saveRules(Long stylesId, List<String> colorCodes, List<String> sizeCodes) {
        Set<String> distinctColors = new LinkedHashSet<>(colorCodes);
        Set<String> distinctSizes = new LinkedHashSet<>(sizeCodes);
        List<StylesRule> rules = new ArrayList<>();

        for (String code : distinctColors) {
            if (!StringUtils.hasText(code)) {
                continue;
            }
            StylesRule rule = new StylesRule();
            StylesRuleId id = new StylesRuleId();
            id.setStylesId(stylesId);
            id.setCodeType("COLOR");
            id.setCode(code);
            rule.setId(id);
            rules.add(rule);
        }

        for (String code : distinctSizes) {
            if (!StringUtils.hasText(code)) {
                continue;
            }
            StylesRule rule = new StylesRule();
            StylesRuleId id = new StylesRuleId();
            id.setStylesId(stylesId);
            id.setCodeType("SIZE");
            id.setCode(code);
            rule.setId(id);
            rules.add(rule);
        }

        if (!rules.isEmpty()) {
            stylesRuleRepository.saveAll(rules);
        }
    }
}