package com.example.keycloakdemo.dto;

import lombok.Data;
import java.util.List;

@Data
public class CategoriaResponseDTO {
    
    private Long id;
    private String nombre;
    private String descripcion;
    
    // Lista resumida de productos (sin categoría dentro para evitar recursión)
    private List<ProductoResumenDTO> productos;
}