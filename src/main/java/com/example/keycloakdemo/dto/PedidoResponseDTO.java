package com.example.keycloakdemo.dto;

import com.example.keycloakdemo.model.EstadoPedido;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PedidoResponseDTO {
    
    private Long id;
    private ClienteResumenDTO cliente;  // Info resumida del cliente
    private List<PedidoItemResponseDTO> items;  // Items con productos
    private BigDecimal total;
    private EstadoPedido estado;  // Enum directamente
    private LocalDateTime fechaPedido;
}