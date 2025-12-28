package com.example.erp.service;

import com.example.erp.domain.FgShipment;
import com.example.erp.repository.FgShipmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FgShipmentService {

    private final FgShipmentRepository fgShipmentRepository;

    public FgShipmentService(FgShipmentRepository fgShipmentRepository) {
        this.fgShipmentRepository = fgShipmentRepository;
    }

    public List<FgShipment> findAll() {
        return fgShipmentRepository.findAll();
    }
}