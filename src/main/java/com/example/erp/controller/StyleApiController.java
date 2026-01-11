package com.example.erp.controller;

import com.example.erp.controller.dto.CodeSearchResult;
import com.example.erp.controller.dto.ItemSearchResult;
import com.example.erp.controller.dto.StyleDeleteResponse;
import com.example.erp.controller.dto.StyleDetailResponse;
import com.example.erp.controller.dto.StyleRegisterRequest;
import com.example.erp.controller.dto.StyleSaveResponse;
import com.example.erp.domain.Item;
import com.example.erp.domain.Style;
import com.example.erp.domain.Code;
import com.example.erp.repository.ItemRepository;
import com.example.erp.service.CodeService;
import com.example.erp.service.StyleRegistrationService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/styles")
public class StyleApiController {

    private final StyleRegistrationService styleRegistrationService;
    private final ItemRepository itemRepository;
    private final CodeService codeService;

    public StyleApiController(StyleRegistrationService styleRegistrationService,
            ItemRepository itemRepository,
            CodeService codeService) {
        this.styleRegistrationService = styleRegistrationService;
        this.itemRepository = itemRepository;
        this.codeService = codeService;
    }

    @GetMapping("/items")
    public List<ItemSearchResult> searchItems(@RequestParam(name = "keyword", required = false) String keyword) {
        List<Item> items = itemRepository.findByIsActiveOrderByItemCodeAsc(1);
        String trimmed = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.KOREAN) : null;
        return items.stream()
                .filter(item -> {
                    if (!StringUtils.hasText(trimmed)) {
                        return true;
                    }
                    return (item.getItemCode() != null
                            && item.getItemCode().toLowerCase(Locale.KOREAN).contains(trimmed))
                            || (item.getItemName() != null
                                    && item.getItemName().toLowerCase(Locale.KOREAN).contains(trimmed));
                })
                .map(item -> new ItemSearchResult(item.getItemCode(), item.getItemName()))
                .toList();
    }

    @GetMapping("/codes")
    public List<CodeSearchResult> searchCodes(@RequestParam(name = "codeType") String codeType,
            @RequestParam(name = "keyword", required = false) String keyword) {
        String trimmed = StringUtils.hasText(codeType) ? codeType.trim() : null;
        if (!StringUtils.hasText(trimmed)) {
            return List.of();
        }
        String search = StringUtils.hasText(keyword) ? keyword.trim() : null;
        List<Code> codes = codeService.searchActiveCodes(trimmed, search, search, null);
        return codes.stream()
                .map(code -> new CodeSearchResult(code.getCodeType(), code.getCode(), code.getCodeName()))
                .toList();
    }

    @GetMapping("/detail")
    public StyleDetailResponse loadDetail(@RequestParam(name = "stylesId", required = false) Long stylesId,
            @RequestParam(name = "styleCode", required = false) String styleCode) {
        return styleRegistrationService.findDetail(stylesId, styleCode);
    }

    @PostMapping
    public StyleSaveResponse create(@RequestBody StyleRegisterRequest request) {
        Style style = styleRegistrationService.create(request);
        return new StyleSaveResponse(style.getStylesId(), style.getStyleCode());
    }

    @PutMapping("/{stylesId}")
    public StyleSaveResponse update(@PathVariable("stylesId") Long stylesId,
            @RequestBody StyleRegisterRequest request) {
        request.setStylesId(stylesId);
        Style style = styleRegistrationService.update(request);
        return new StyleSaveResponse(style.getStylesId(), style.getStyleCode());
    }

    @DeleteMapping("/{stylesId}")
    public StyleDeleteResponse delete(@PathVariable("stylesId") Long stylesId) {
        StyleRegistrationService.StyleDeleteResult result = styleRegistrationService.delete(stylesId);
        return new StyleDeleteResponse(result.stylesId(), result.deleted(), result.deactivated(), result.reason());
    }
}
