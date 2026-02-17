package com.example.keycloakdemo.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PedidoItemResponseDTO {
    
    private Long id;
    private ProductoResumenDTO producto;  // Info resumida del producto
    private Integer cantidad;
    private BigDecimal precioUnitario;  // Precio al momento de la compra
    private BigDecimal subtotal;  // cantidad * precioUnitario
}