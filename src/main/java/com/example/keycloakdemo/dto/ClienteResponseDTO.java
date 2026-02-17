package com.example.keycloakdemo.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClienteResponseDTO {
    
    private Long id;
    private String nombre;
    private String email;
    private String telefono;
    private String direccion;
    private LocalDateTime fechaRegistro;
    
    // Lista resumida de pedidos (solo info básica, sin items)
    private List<PedidoResumenDTO> pedidos;
}
