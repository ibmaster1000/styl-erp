package com.example.erp.repository;

import com.example.erp.domain.FgShipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FgShipmentRepository extends JpaRepository<FgShipment, Long> {
}