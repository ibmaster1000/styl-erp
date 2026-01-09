package com.example.erp.controller;

import com.example.erp.controller.dto.StyleSearchCondition;
import com.example.erp.controller.dto.StyleSearchResult;
import com.example.erp.controller.dto.StyleListRow;
import com.example.erp.controller.support.PageViewSupport;
import com.example.erp.service.StyleQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/style")
public class StyleController extends PageViewSupport {

	private final StyleQueryService styleQueryService;

	public StyleController(StyleQueryService styleQueryService) {
		this.styleQueryService = styleQueryService;
	}

	@GetMapping
    public String list(@RequestParam(name = "styleCode", required = false) String styleCode,
            @RequestParam(name = "designerName", required = false) String designerName,
            @RequestParam(name = "designer", required = false) String designer,
            @RequestParam(name = "isActive", required = false) Integer isActive,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "viewMode", required = false) String viewMode,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", required = false) String direction,
            Model model) {
        String resolvedDesigner = designerName != null ? designerName : designer;
        Integer resolvedActive = isActive != null ? isActive : status;

        StyleSearchCondition condition = new StyleSearchCondition(styleCode, resolvedDesigner, resolvedActive,
                sort, direction);
		StyleSearchResult result = styleQueryService.search(condition);

		List<StyleListRow> safeList = result.getList() == null ? List.of()
				: result.getList().stream().filter(Objects::nonNull).toList();

		populate(model, "품번/스타일 관리", "style", "pages/style", safeList);
		model.addAttribute("styles_rule", result.getStylesRule());
		int currentYear = java.time.LocalDate.now().getYear();
		List<Integer> years = IntStream.rangeClosed(currentYear - 2, currentYear + 1).boxed().toList();
		List<Integer> months = IntStream.rangeClosed(1, 12).boxed().toList();
		model.addAttribute("styleYears", years);
		model.addAttribute("styleMonths", months);
		Boolean flatView = Boolean.FALSE;
		if ("flat".equalsIgnoreCase(viewMode)) {
			flatView = Boolean.TRUE;
		}
		model.addAttribute("flatView", flatView);
		return "layout/layout";
	}
}
