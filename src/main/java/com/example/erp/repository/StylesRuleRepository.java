package com.example.erp.repository;

import com.example.erp.domain.StylesRule;
import com.example.erp.domain.StylesRuleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StylesRuleRepository extends JpaRepository<StylesRule, StylesRuleId> {
    List<StylesRule> findByStylesId(Long stylesId);
}
