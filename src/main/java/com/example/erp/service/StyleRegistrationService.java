package com.example.erp.service;

import com.example.erp.controller.dto.StyleDetailResponse;
import com.example.erp.controller.dto.StyleRegisterRequest;
import com.example.erp.domain.Item;
import com.example.erp.domain.Style;
import com.example.erp.domain.StylesRule;
import com.example.erp.domain.StylesRuleId;
import com.example.erp.controller.dto.StyleRuleCodeView;
import com.example.erp.repository.ItemRepository;
import com.example.erp.repository.ProductionAgreementRepository;
import com.example.erp.repository.StyleQueryRepository;
import com.example.erp.repository.StyleRepository;
import com.example.erp.repository.StylesRuleRepository;
import com.example.erp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class StyleRegistrationService {

    private final StyleRepository styleRepository;
    private final ItemRepository itemRepository;
    private final StylesRuleRepository stylesRuleRepository;
    private final StyleQueryRepository styleQueryRepository;
    private final UserRepository userRepository;
    private final ProductionAgreementRepository productionAgreementRepository;

    public StyleRegistrationService(
            StyleRepository styleRepository,
            ItemRepository itemRepository,
            StylesRuleRepository stylesRuleRepository,
            StyleQueryRepository styleQueryRepository,
            UserRepository userRepository,
            ProductionAgreementRepository productionAgreementRepository) {
        this.styleRepository = styleRepository;
        this.itemRepository = itemRepository;
        this.stylesRuleRepository = stylesRuleRepository;
        this.styleQueryRepository = styleQueryRepository;
        this.userRepository = userRepository;
        this.productionAgreementRepository = productionAgreementRepository;
    }

    public Style create(StyleRegisterRequest req) {
        validateCreateRequest(req);

        Item item = loadActiveItem(req.getItemCode());
        String styleCode = generateStyleCode(item.getItemCode(), req.getYear(), req.getMonth());

        Style style = new Style();
        style.setStyleCode(styleCode);
        style.setItemCode(item.getItemCode());
        style.setItemName(item.getItemName());
        style.setStartDate(req.getStartDate());
        style.setProductionCost(req.getProductionCost());
        style.setSupplyPrice(req.getSupplyPrice());
        style.setSalesPrice(req.getSalesPrice());
        style.setDesignerEmpNo(req.getDesignerEmpNo());
        style.setProductEmpNo(req.getProductEmpNo());
        style.setSalesEmpNo(req.getSalesEmpNo());
        style.setLogisticEmpNo(req.getLogisticEmpNo());
        style.setIsActive(req.getIsActive() != null ? req.getIsActive() : 1);

        Style savedStyle = styleRepository.save(style);
        replaceRules(savedStyle.getStylesId(), req.getColorCodes(), req.getSizeCodes());

        return savedStyle;
    }

    public Style update(StyleRegisterRequest req) {
        validateUpdateRequest(req);

        Style style = resolveStyle(req);
        Item item = loadActiveItem(req.getItemCode());

        style.setStyleCode(req.getStyleCode());
        style.setItemCode(item.getItemCode());
        style.setItemName(item.getItemName());
        style.setStartDate(req.getStartDate());
        style.setProductionCost(req.getProductionCost());
        style.setSupplyPrice(req.getSupplyPrice());
        style.setSalesPrice(req.getSalesPrice());
        style.setDesignerEmpNo(req.getDesignerEmpNo());
        style.setProductEmpNo(req.getProductEmpNo());
        style.setSalesEmpNo(req.getSalesEmpNo());
        style.setLogisticEmpNo(req.getLogisticEmpNo());
        if (req.getIsActive() != null) {
            style.setIsActive(req.getIsActive());
        }

        Style savedStyle = styleRepository.save(style);
        replaceRules(savedStyle.getStylesId(), req.getColorCodes(), req.getSizeCodes());

        return savedStyle;
    }

    public StyleDeleteResult delete(Long stylesId) {
        if (stylesId == null) {
            throw new IllegalArgumentException("stylesId is required");
        }
        Style style = styleRepository.findById(stylesId)
                .orElseThrow(() -> new IllegalArgumentException("style not found"));
        stylesRuleRepository.deleteAllByStylesId(stylesId);
        boolean hasAgreement = productionAgreementRepository.existsByStylesId(stylesId);
        if (hasAgreement) {
            style.setIsActive(0);
            styleRepository.save(style);
            return StyleDeleteResult.deactivated(stylesId, "Referenced by production_agreements");
        }
        styleRepository.delete(style);
        styleRepository.flush();
        return StyleDeleteResult.deleted(stylesId);
    }

    @Transactional(readOnly = true)
    public StyleDetailResponse findDetail(Long stylesId, String styleCode) {
        Style style = resolveStyle(stylesId, styleCode);
        List<StylesRule> rules = stylesRuleRepository.findByIdStylesId(style.getStylesId());
        List<String> colors = new ArrayList<>();
        List<String> sizes = new ArrayList<>();
        for (StylesRule rule : rules) {
            StylesRuleId id = rule.getId();
            if (id == null) {
                continue;
            }
            if ("COLOR".equalsIgnoreCase(id.getCodeType())) {
                colors.add(id.getCode());
            } else if ("SIZE".equalsIgnoreCase(id.getCodeType())) {
                sizes.add(id.getCode());
            }
        }

        Map<StyleQueryRepository.CodeKey, String> codeNameMap = resolveCodeNames(colors, sizes);
        List<StyleRuleCodeView> colorRules = colors.stream()
                .map(code -> new StyleRuleCodeView(code, resolveCodeName(codeNameMap, "COLOR", code)))
                .toList();
        List<StyleRuleCodeView> sizeRules = sizes.stream()
                .map(code -> new StyleRuleCodeView(code, resolveCodeName(codeNameMap, "SIZE", code)))
                .toList();

        return new StyleDetailResponse(
                style.getStylesId(),
                style.getStyleCode(),
                style.getItemCode(),
                style.getItemName(),
                style.getDesignerEmpNo(),
                style.getProductEmpNo(),
                style.getSalesEmpNo(),
                style.getLogisticEmpNo(),
                style.getStartDate(),
                style.getProductionCost(),
                style.getSupplyPrice(),
                style.getSalesPrice(),
                style.getIsActive(),
                colors,
                sizes,
                colorRules,
                sizeRules
        );
    }

    private Map<StyleQueryRepository.CodeKey, String> resolveCodeNames(List<String> colors, List<String> sizes) {
        Set<StyleQueryRepository.CodeKey> keys = new LinkedHashSet<>();
        for (String code : colors) {
            if (StringUtils.hasText(code)) {
                keys.add(new StyleQueryRepository.CodeKey("COLOR", code));
                keys.add(new StyleQueryRepository.CodeKey("color", code));
            }
        }
        for (String code : sizes) {
            if (StringUtils.hasText(code)) {
                keys.add(new StyleQueryRepository.CodeKey("SIZE", code));
                keys.add(new StyleQueryRepository.CodeKey("size", code));
            }
        }
        return styleQueryRepository.findCodeNames(keys);
    }

    private String resolveCodeName(Map<StyleQueryRepository.CodeKey, String> codeNameMap, String codeType, String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String exact = codeNameMap.get(new StyleQueryRepository.CodeKey(codeType, code));
        if (StringUtils.hasText(exact)) {
            return exact;
        }
        String lower = codeNameMap.get(new StyleQueryRepository.CodeKey(codeType.toLowerCase(), code));
        if (StringUtils.hasText(lower)) {
            return lower;
        }
        return codeNameMap.get(new StyleQueryRepository.CodeKey(codeType.toUpperCase(), code));
    }

    private void validateCreateRequest(StyleRegisterRequest req) {
        validateCommon(req);
        if (req.getYear() == null || req.getMonth() == null) {
            throw new IllegalArgumentException("year/month is required");
        }
    }

    private void validateUpdateRequest(StyleRegisterRequest req) {
        validateCommon(req);
        if (!StringUtils.hasText(req.getStyleCode())) {
            throw new IllegalArgumentException("styleCode is required");
        }
    }

    private void validateCommon(StyleRegisterRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (!StringUtils.hasText(req.getItemCode())) {
            throw new IllegalArgumentException("itemCode is required");
        }
        if (req.getStartDate() == null) {
            throw new IllegalArgumentException("startDate is required");
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
        validateEmployee("designerEmpNo", req.getDesignerEmpNo(), true);
        validateEmployee("productEmpNo", req.getProductEmpNo(), false);
        validateEmployee("salesEmpNo", req.getSalesEmpNo(), false);
        validateEmployee("logisticEmpNo", req.getLogisticEmpNo(), false);
        if (req.getColorCodes() == null || req.getColorCodes().isEmpty()) {
            throw new IllegalArgumentException("colorCodes is required");
        }
        if (req.getSizeCodes() == null || req.getSizeCodes().isEmpty()) {
            throw new IllegalArgumentException("sizeCodes is required");
        }
    }

    private Item loadActiveItem(String itemCode) {
        return itemRepository.findById(itemCode)
                .filter(found -> Integer.valueOf(1).equals(found.getIsActive()))
                .orElseThrow(() -> new IllegalArgumentException("active item not found"));
    }

    private void validateEmployee(String fieldName, String empNo, boolean required) {
        if (!StringUtils.hasText(empNo)) {
            if (required) {
                throw new IllegalArgumentException(fieldName + " is required");
            }
            return;
        }
        if (!userRepository.existsById(empNo)) {
            throw new IllegalArgumentException("Invalid " + fieldName + ": not found");
        }
    }

    private Style resolveStyle(StyleRegisterRequest req) {
        return resolveStyle(req.getStylesId(), req.getStyleCode());
    }

    private Style resolveStyle(Long stylesId, String styleCode) {
        if (stylesId != null) {
            return styleRepository.findById(stylesId)
                    .orElseThrow(() -> new IllegalArgumentException("style not found"));
        }
        if (StringUtils.hasText(styleCode)) {
            return styleRepository.findByStyleCode(styleCode)
                    .orElseThrow(() -> new IllegalArgumentException("style not found"));
        }
        throw new IllegalArgumentException("style identifier is required");
    }

    private String generateStyleCode(String itemCode, Integer year, Integer month) {
        String prefix = buildPrefix(itemCode, year, month);
        Optional<Style> last = styleRepository.findTopByStyleCodeStartingWithOrderByStyleCodeDesc(prefix);
        int nextSeq = last.map(found -> parseSequence(found.getStyleCode()) + 1).orElse(1);
        return prefix + String.format("%03d", nextSeq);
    }

    private String buildPrefix(String itemCode, Integer year, Integer month) {
        String trimmed = itemCode != null ? itemCode.trim() : "";
        String itemPrefix = trimmed.length() >= 2 ? trimmed.substring(0, 2) : trimmed;
        int yy = year != null ? year % 100 : 0;
        int mm = month != null ? month : 0;
        return String.format("%s%02d%02d", itemPrefix, yy, mm);
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

    private void replaceRules(Long stylesId, List<String> colorCodes, List<String> sizeCodes) {
        stylesRuleRepository.deleteByIdStylesId(stylesId);
        saveRules(stylesId, colorCodes, sizeCodes);
    }

    public record StyleDeleteResult(Long stylesId, boolean deleted, boolean deactivated, String reason) {

        public static StyleDeleteResult deleted(Long stylesId) {
            return new StyleDeleteResult(stylesId, true, false, null);
        }

        public static StyleDeleteResult deactivated(Long stylesId, String reason) {
            return new StyleDeleteResult(stylesId, false, true, reason);
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
