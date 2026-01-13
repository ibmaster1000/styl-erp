package com.example.erp.controller.dto;

public record MaterialSpecValidationError(int row, String field, String message) {
}
