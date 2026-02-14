package com.example.keycloakdemo.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ProductoRequestDTO {

   @NotBlank
   private String nombre;

   private String descripcion;

   @NotNull
   @Positive
   private BigDecimal precio;

   private Integer stock;

   @NotNull
   private Long categoriaID;
}
