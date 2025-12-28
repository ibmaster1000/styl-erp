package com.example.erp.repository;

import com.example.erp.domain.FgReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FgReceiptRepository extends JpaRepository<FgReceipt, Long> {
}