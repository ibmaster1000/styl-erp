package com.example.erp.domain;

import java.io.Serializable;
import java.util.Objects;

public class StylesRuleId implements Serializable {

    private Long stylesId;
    private String codeType;
    private String code;

    public StylesRuleId() {
    }

    public StylesRuleId(Long stylesId, String codeType, String code) {
        this.stylesId = stylesId;
        this.codeType = codeType;
        this.code = code;
    }

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
