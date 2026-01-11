package com.example.erp.controller.dto;

public record StyleDeleteResponse(Long stylesId, boolean deleted, boolean deactivated, String reason) {
}