package com.example.erp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CodeId implements Serializable {

    private static final long serialVersionUID = 1L;
	
	@Column(name = "code_type", nullable = false, length = 20)
    private String codeType;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    public CodeId() {
    }
    public CodeId(String codeType, String code) {
        this.codeType = codeType;
        this.code = code;
    }
    
    public String getCodeType() {
        return codeType;
    }
    public void setCodeType(String codeType) {
        this.codeType = codeType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeId codeId = (CodeId) o;
        return Objects.equals(codeType, codeId.codeType) && Objects.equals(code, codeId.code);
    }

    @Override
    public int hashCode() {
    	return Objects.hash(codeType, code);
    }
}
