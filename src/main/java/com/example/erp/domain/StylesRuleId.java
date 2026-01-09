package com.example.erp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class StylesRuleId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "styles_id")
    private Long stylesId;

    @Column(name = "code_type", length = 50)
    private String codeType;

    @Column(name = "code", length = 50)
    private String code;

    public Long getStylesId() {
        return stylesId;
    }

    public void setStylesId(Long stylesId) {
        this.stylesId = stylesId;
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StylesRuleId that = (StylesRuleId) o;
        return Objects.equals(stylesId, that.stylesId)
                && Objects.equals(codeType, that.codeType)
                && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stylesId, codeType, code);
    }
}
