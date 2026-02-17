package com.example.keycloakdemo.dto;

import lombok.Data;

@Data
public class ClienteResumenDTO {
    private Long id;
    private String nombre;
    private String email;
    // Solo lo esencial, sin pedidos
    // Para usar en otros DTOs
}
