package com.example.erp.service;

import com.example.erp.domain.FgTransfer;
import com.example.erp.repository.FgTransferRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FgTransferService {

    private final FgTransferRepository fgTransferRepository;

    public FgTransferService(FgTransferRepository fgTransferRepository) {
        this.fgTransferRepository = fgTransferRepository;
    }

    public List<FgTransfer> findAll() {
        return fgTransferRepository.findAll();
    }
}