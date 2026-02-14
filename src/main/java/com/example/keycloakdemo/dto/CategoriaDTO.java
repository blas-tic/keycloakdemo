package com.example.keycloakdemo.dto;

import lombok.Data;
import java.util.List;

@Data
public class CategoriaDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    // Si quieres incluir los productos de forma resumida (sin bucle), usa un ProductoResumenDTO
    private List<ProductoRequestDTO> productos;
}