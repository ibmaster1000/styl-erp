package com.example.erp.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.erp.domain.Code;
import com.example.erp.domain.CodeId;
import com.example.erp.service.CodeService;

@Controller
@RequestMapping("/codes")
public class CodeController {

	private final CodeService codeService;

	public CodeController(CodeService codeService) {
		this.codeService = codeService;
	}

	@GetMapping
	public String list(Model model, @ModelAttribute("message") String message) {
		List<Code> codes = codeService.findAllActive();
		model.addAttribute("codes", codes);
		model.addAttribute("form", new CodeForm());
		model.addAttribute("message", message);
		return "codes";
	}

	@PostMapping
	public String create(@ModelAttribute("form") CodeForm form, RedirectAttributes redirectAttributes) {
		try {
			codeService.create(form.getCodeType(), form.getCode(), form.getName(), form.getSortOrder(),
					form.getDescription());
			redirectAttributes.addFlashAttribute("message", "코드가 추가되었습니다.");
		} catch (RuntimeException e) {
			redirectAttributes.addFlashAttribute("message", e.getMessage());
		}
		return "redirect:/codes";
	}

	@PostMapping("/{groupCode}/{code}/update")
	public String update(@PathVariable(name = "groupCode") String codeType,
			@PathVariable(name = "code") String code,
			@ModelAttribute("form") CodeForm form, RedirectAttributes redirectAttributes) {
		try {
			codeService.update(new CodeId(codeType, code), form.getCodeType(), form.getCode(), form.getName(),
					form.getSortOrder(), form.getDescription());
			redirectAttributes.addFlashAttribute("message", "코드가 수정되었습니다.");
		} catch (RuntimeException e) {
			redirectAttributes.addFlashAttribute("message", e.getMessage());
		}
		return "redirect:/codes";
	}

	@PostMapping("/{groupCode}/{code}/delete")
	public String delete(@PathVariable(name = "groupCode") String codeType,
			@PathVariable(name = "code") String code,
			RedirectAttributes redirectAttributes) {
		boolean removed = codeService.softDelete(new CodeId(codeType, code));
		redirectAttributes.addFlashAttribute("message", removed ? "코드가 삭제되었습니다." : "삭제할 코드를 찾을 수 없습니다.");
		return "redirect:/codes";
	}

	public static class CodeForm {
		private Long id;
		private String codeType;
		private String code;
		private String name;
		private Integer sortOrder;
		private String description;

		public Long getId() { return id; }
	    public void setId(Long id) { this.id = id; }

	    public String getCodeType() { return codeType; }
	    public void setCodeType(String codeType) { this.codeType = codeType; }

	    public String getCode() { return code; }
	    public void setCode(String code) { this.code = code; }

	    public String getName() { return name; }
	    public void setName(String name) { this.name = name; }

	    public Integer getSortOrder() { return sortOrder; }
	    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

	    public String getDescription() { return description; }
	    public void setDescription(String description) { this.description = description; }

	}
}