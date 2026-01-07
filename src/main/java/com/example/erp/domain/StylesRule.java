package com.example.erp.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "styles_rule")
public class StylesRule {

    @EmbeddedId
    private StylesRuleId id;

    public StylesRuleId getId() {
        return id;
    }

    public void setId(StylesRuleId id) {
        this.id = id;
    }
}