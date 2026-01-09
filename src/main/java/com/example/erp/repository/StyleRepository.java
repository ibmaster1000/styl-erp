package com.example.erp.repository;

import com.example.erp.domain.Style;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public interface StyleRepository extends JpaRepository<Style, Long> {
    Optional<Style> findTopByStyleCodeStartingWithOrderByStyleCodeDesc(String prefix);

    Optional<Style> findByStyleCode(String styleCode);
}
