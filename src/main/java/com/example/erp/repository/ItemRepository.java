package com.example.erp.repository;

import com.example.erp.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, String> {
    List<Item> findByIsActiveOrderByItemCodeAsc(Integer isActive);
}