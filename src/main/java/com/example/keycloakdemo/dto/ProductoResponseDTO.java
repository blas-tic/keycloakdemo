package com.example.keycloakdemo.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductoResponseDTO {
   private Long id;
   private String nombre;
   private String descripcion;
   private BigDecimal precio;
   private Integer stock;
   private CategoriaResumenDTO categoria; // Versión simplificada de Categoria
}
