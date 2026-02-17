package com.example.keycloakdemo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PedidoRequestDTO {
    
    @NotNull(message = "El ID del cliente es obligatorio")
    private Long clienteId;
    
    @NotEmpty(message = "El pedido debe tener al menos un item")
    @Valid  // Valida cada item de la lista
    private List<PedidoItemRequestDTO> items;
    
    // El total se calcula automáticamente
    // La fecha se asigna automáticamente
    // El estado inicial es PENDIENTE
}