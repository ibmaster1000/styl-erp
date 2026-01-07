package com.example.erp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.erp.domain.Style;

@Repository
public interface StyleRepository extends JpaRepository<Style, Long> {
	Optional<Style> findTopByStyleCodeStartingWithOrderByStyleCodeDesc(String prefix);
}