package com.example.erp.repository;

import com.example.erp.domain.ProductionJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionJobRepository extends JpaRepository<ProductionJob, Long> {
}
