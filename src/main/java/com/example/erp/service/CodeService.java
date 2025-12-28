package com.example.erp.service;

import com.example.erp.domain.Code;
import com.example.erp.domain.CodeId;
import com.example.erp.repository.CodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CodeService {

    private final CodeRepository codeRepository;

    public CodeService(CodeRepository codeRepository) {
        this.codeRepository = codeRepository;
    }

    public List<Code> findAllActive() {
        return codeRepository.findByDeletedFalseOrderByIdGroupCodeAscSortOrderAscIdCodeAsc();
    }

    public Code getActive(CodeId id) {
        return codeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Code not found"));
    }

    @Transactional
    public Code create(String groupCode, String code, String name, Integer sortOrder, String description) {
        validateRequired(groupCode, code, name);
        if (codeRepository.existsByIdGroupCodeAndIdCodeAndDeletedFalse(groupCode, code)) {
            throw new IllegalStateException("Duplicate code in same group");
        }

        Code entity = new Code();
        entity.setId(new CodeId(groupCode.trim(), code.trim()));
        entity.setName(name.trim());
        entity.setDescription(StringUtils.hasText(description) ? description.trim() : null);
        entity.setSortOrder(sortOrder != null ? sortOrder : 0);
        entity.setDeleted(false);

        return codeRepository.save(entity);
    }

    @Transactional
    public Code update(CodeId id, String groupCode, String code, String name, Integer sortOrder, String description) {
        validateRequired(groupCode, code, name);
        Code existing = getActive(id);

        boolean duplicate = codeRepository.existsByIdGroupCodeAndIdCodeAndDeletedFalse(groupCode, code)
                && (!existing.getGroupCode().equals(groupCode) || !existing.getCode().equals(code));
        if (duplicate) {
            throw new IllegalStateException("Duplicate code in same group");
        }

        existing.setId(new CodeId(groupCode.trim(), code.trim()));
        existing.setName(name.trim());
        existing.setDescription(StringUtils.hasText(description) ? description.trim() : null);
        existing.setSortOrder(sortOrder != null ? sortOrder : 0);
        return existing;
    }

    @Transactional
    public boolean softDelete(CodeId id) {
        return codeRepository.softDelete(id) > 0;
    }

    private void validateRequired(String groupCode, String code, String name) {
        if (!StringUtils.hasText(groupCode) || !StringUtils.hasText(code) || !StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Required field is empty");
        }
    }
}