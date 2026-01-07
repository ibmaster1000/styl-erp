package com.example.erp.repository;

import com.example.erp.domain.StylesRule;
import com.example.erp.domain.StylesRuleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StylesRuleRepository extends JpaRepository<StylesRule, StylesRuleId> {
}