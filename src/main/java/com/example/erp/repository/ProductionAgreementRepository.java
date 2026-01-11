package com.example.erp.repository;

import com.example.erp.domain.ProductionAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductionAgreementRepository extends JpaRepository<ProductionAgreement, String> {
	
	@Query(value = "select exists(select 1 from production_agreements where styles_id = :stylesId)", nativeQuery = true)
    boolean existsByStylesId(@Param("stylesId") Long stylesId);
}