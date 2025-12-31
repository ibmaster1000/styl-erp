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
    	return codeRepository.findByDeletedFalseAndActiveTrueOrderByIdCodeTypeAscSortOrderAscIdCodeAsc();
    }

    public Code getActive(CodeId id) {
        return codeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Code not found"));
    }

    @Transactional
    
    public Code create(String codeType, String code, String name, Integer sortOrder, String description) {
        validateRequired(codeType, code, name);
        if (codeRepository.existsByIdCodeTypeAndIdCodeAndDeletedFalse(codeType, code)) {
            throw new IllegalStateException("Duplicate code in same group");
        }

        Code entity = new Code();
        
        entity.setId(new CodeId(codeType.trim(), code.trim()));
        entity.setCodeName(name.trim());
        entity.setName(name.trim());
        entity.setDescription(StringUtils.hasText(description) ? description.trim() : null);
        entity.setSortOrder(sortOrder != null ? sortOrder : 0);
        entity.setActive(true);
        entity.setDeleted(false);

        return codeRepository.save(entity);
    }

    @Transactional
    
    public Code update(CodeId id, String codeType, String code, String name, Integer sortOrder, String description) {
        validateRequired(codeType, code, name);
        Code existing = getActive(id);
        boolean duplicate = codeRepository.existsByIdCodeTypeAndIdCodeAndDeletedFalse(codeType, code)
                && (!existing.getCodeType().equals(codeType) || !existing.getCode().equals(code));
        if (duplicate) {
            throw new IllegalStateException("Duplicate code in same group");
        }

        existing.setId(new CodeId(codeType.trim(), code.trim()));
        existing.setCodeName(name.trim());
        existing.setName(name.trim());
        existing.setDescription(StringUtils.hasText(description) ? description.trim() : null);
        existing.setSortOrder(sortOrder != null ? sortOrder : 0);
        return existing;
    }

    @Transactional
    public boolean softDelete(CodeId id) {
        return codeRepository.softDelete(id) > 0;
    }
    
    private void validateRequired(String codeType, String code, String name) {
        if (!StringUtils.hasText(codeType) || !StringUtils.hasText(code) || !StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Required field is empty");
        }
    }
    public List<Code> searchActiveCodes(String codeType, String code, String codeName, String remark) {
        return codeRepository.searchActiveCodes(
                StringUtils.hasText(codeType) ? codeType.trim() : null,
                StringUtils.hasText(code) ? code.trim() : null,
                StringUtils.hasText(codeName) ? codeName.trim() : null,
                StringUtils.hasText(remark) ? remark.trim() : null
        );
    }

    public List<String> findActiveCodeTypes() {
        return codeRepository.findDistinctActiveCodeTypes();
    }
}