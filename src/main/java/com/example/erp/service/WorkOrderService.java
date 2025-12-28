package com.example.erp.service;

import com.example.erp.domain.WorkOrder;
import com.example.erp.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;

    public WorkOrderService(WorkOrderRepository workOrderRepository) {
        this.workOrderRepository = workOrderRepository;
    }

    public List<WorkOrder> findAll() {
        return workOrderRepository.findAll();
    }
}