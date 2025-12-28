package com.example.erp.service;

import com.example.erp.domain.FgReceipt;
import com.example.erp.repository.FgReceiptRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FgReceiptService {

    private final FgReceiptRepository fgReceiptRepository;

    public FgReceiptService(FgReceiptRepository fgReceiptRepository) {
        this.fgReceiptRepository = fgReceiptRepository;
    }

    public List<FgReceipt> findAll() {
        return fgReceiptRepository.findAll();
    }
}