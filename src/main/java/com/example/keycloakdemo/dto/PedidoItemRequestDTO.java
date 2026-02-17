package com.example.keycloakdemo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PedidoItemRequestDTO {
    
    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;
    
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;
    
    // El precio y subtotal se calculan automáticamente desde el producto
}