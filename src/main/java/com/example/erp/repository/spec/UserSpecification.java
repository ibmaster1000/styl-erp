package com.example.erp.repository.spec;

import com.example.erp.domain.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<User> search(String name, String empNo, String dept) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(name)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(empNo)) {
                predicates.add(cb.like(cb.lower(root.get("empNo")), "%" + empNo.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(dept)) {
                predicates.add(cb.like(cb.lower(root.get("dept")), "%" + dept.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
