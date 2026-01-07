package com.example.erp.repository;

import com.example.erp.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, String> {
    List<Item> findByIsActiveTrueOrderByItemCodeAsc();
}
