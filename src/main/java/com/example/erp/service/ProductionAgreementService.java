package com.example.erp.service;

import com.example.erp.domain.ProductionAgreement;
import com.example.erp.repository.ProductionAgreementRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductionAgreementService {

    private final ProductionAgreementRepository productionAgreementRepository;

    public ProductionAgreementService(ProductionAgreementRepository productionAgreementRepository) {
        this.productionAgreementRepository = productionAgreementRepository;
    }

    public List<ProductionAgreement> findAll() {
        return productionAgreementRepository.findAll();
    }
}