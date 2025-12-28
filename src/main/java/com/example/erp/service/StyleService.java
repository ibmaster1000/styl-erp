package com.example.erp.service;

import com.example.erp.domain.Style;
import com.example.erp.repository.StyleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StyleService {

    private final StyleRepository styleRepository;

    public StyleService(StyleRepository styleRepository) {
        this.styleRepository = styleRepository;
    }

    public List<Style> findAll() {
        return styleRepository.findAll();
    }
}