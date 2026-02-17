package com.example.keycloakdemo.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductoResumenDTO {
    private Long id;
    private String nombre;
    private BigDecimal precio;
    private Integer stock;
    // Sin categoría, sin descripción
}