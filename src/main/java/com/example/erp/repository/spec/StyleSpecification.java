package com.example.erp.repository.spec;

import com.example.erp.controller.dto.StyleSearchCriteria;
import com.example.erp.domain.Style;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class StyleSpecification {

    private StyleSpecification() {
    }

    public static Specification<Style> from(StyleSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (criteria == null) {
                return cb.conjunction();
            }

            if (StringUtils.hasText(criteria.getStyleNo())) {
                predicates.add(cb.like(cb.lower(root.get("styleNo")), "%" + criteria.getStyleNo().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(criteria.getDesigner())) {
                predicates.add(cb.like(cb.lower(root.get("designer")), "%" + criteria.getDesigner().toLowerCase() + "%"));
            }

            if (criteria.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), criteria.getActive()));
            }

            addRangePredicate(predicates, cb, root.get("productionCost"), criteria.getProductionCostMin(), criteria.getProductionCostMax());
            addRangePredicate(predicates, cb, root.get("supplyPrice"), criteria.getSupplyPriceMin(), criteria.getSupplyPriceMax());
            addRangePredicate(predicates, cb, root.get("salesPrice"), criteria.getSalesPriceMin(), criteria.getSalesPriceMax());

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private static void addRangePredicate(List<jakarta.persistence.criteria.Predicate> predicates,
                                          jakarta.persistence.criteria.CriteriaBuilder cb,
                                          jakarta.persistence.criteria.Path<BigDecimal> path,
                                          BigDecimal min,
                                          BigDecimal max) {
        if (min != null) {
            predicates.add(cb.greaterThanOrEqualTo(path, min));
        }
        if (max != null) {
            predicates.add(cb.lessThanOrEqualTo(path, max));
        }
    }
}