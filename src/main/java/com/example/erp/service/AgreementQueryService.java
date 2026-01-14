package com.example.erp.service;

import com.example.erp.controller.dto.AgreementQuantitySummary;
import com.example.erp.repository.AgreementQuantitySummaryView;
import com.example.erp.repository.ProductionAgreementRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service
public class AgreementQueryService {

    private final ProductionAgreementRepository productionAgreementRepository;

    public AgreementQueryService(ProductionAgreementRepository productionAgreementRepository) {
        this.productionAgreementRepository = productionAgreementRepository;
    }

    public BigDecimal sumQuantityByAgreementAndColor(String agreementCode, String colorCode) {
        if (!StringUtils.hasText(agreementCode) || !StringUtils.hasText(colorCode)) {
            return BigDecimal.ZERO;
        }
        BigDecimal result = productionAgreementRepository
                .sumQuantityByAgreementAndColor(agreementCode.trim(), colorCode.trim());
        return result != null ? result : BigDecimal.ZERO;
    }

    public List<AgreementQuantitySummary> findAgreementQuantities(String agreementCode) {
        if (!StringUtils.hasText(agreementCode)) {
            return Collections.emptyList();
        }
        List<AgreementQuantitySummaryView> rows = productionAgreementRepository
                .findAgreementQuantitySummariesByAgreementCode(agreementCode.trim());
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(row -> new AgreementQuantitySummary(row.getColorCode(),
                        row.getQuantity() != null ? row.getQuantity() : BigDecimal.ZERO))
                .toList();
    }
}
