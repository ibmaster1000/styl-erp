package com.example.erp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "styles_rule")
@IdClass(StylesRuleId.class)
public class StylesRule {

    @Id
    @Column(name = "styles_id")
    private Long stylesId;

    @Id
    @Column(name = "code_type", length = 20)
    private String codeType;

    @Id
    @Column(name = "code", length = 20)
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
}
