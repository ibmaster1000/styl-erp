package com.example.erp.repository;

import com.example.erp.domain.FgTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FgTransferRepository extends JpaRepository<FgTransfer, Long> {
}