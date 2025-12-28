package com.example.erp.controller;

import com.example.erp.domain.Code;
import com.example.erp.service.CodeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

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
            codeService.create(form.getGroupCode(), form.getCode(), form.getName(), form.getSortOrder(), form.getDescription());
            redirectAttributes.addFlashAttribute("message", "코드가 추가되었습니다.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/codes";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @ModelAttribute("form") CodeForm form, RedirectAttributes redirectAttributes) {
        try {
            codeService.update(id, form.getGroupCode(), form.getCode(), form.getName(), form.getSortOrder(), form.getDescription());
            redirectAttributes.addFlashAttribute("message", "코드가 수정되었습니다.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/codes";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean removed = codeService.softDelete(id);
        redirectAttributes.addFlashAttribute("message", removed ? "코드가 삭제되었습니다." : "삭제할 코드를 찾을 수 없습니다.");
        return "redirect:/codes";
    }

    public static class CodeForm {
        private Long id;
        private String groupCode;
        private String code;
        private String name;
        private Integer sortOrder;
        private String description;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getGroupCode() {
            return groupCode;
        }

        public void setGroupCode(String groupCode) {
            this.groupCode = groupCode;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}