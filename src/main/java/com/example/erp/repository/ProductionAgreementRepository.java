package com.example.erp.repository;

import com.example.erp.domain.ProductionAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionAgreementRepository extends JpaRepository<ProductionAgreement, String> {
    List<ProductionAgreement> findByStyleNoOrderByAgreementCodeAscColorAscSizeAsc(String styleNo);
}
