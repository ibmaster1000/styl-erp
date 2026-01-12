package com.example.erp.controller.dto;

import java.util.List;
import java.util.Map;

public record MaterialSpecMatrixResponse(List<MaterialSpecMatrixCodeView> colors,
                                         List<MaterialSpecMatrixCodeView> sizes,
                                         Map<String, Integer> quantities) {
}
