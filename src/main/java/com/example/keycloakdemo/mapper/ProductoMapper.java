package com.example.keycloakdemo.mapper;

import com.example.keycloakdemo.dto.CategoriaResumenDTO;
import com.example.keycloakdemo.dto.ProductoResponseDTO;
import com.example.keycloakdemo.model.Categoria;
import com.example.keycloakdemo.model.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    @Mapping(source = "categoria", target = "categoria")
    ProductoResponseDTO toDto(Producto producto);

    List<ProductoResponseDTO> toDtoList(List<Producto> productos);

    default CategoriaResumenDTO toCategoriaResumen(Categoria categoria) {
        if (categoria == null) return null;
        CategoriaResumenDTO dto = new CategoriaResumenDTO();
        dto.setId(categoria.getId());
        dto.setNombre(categoria.getNombre());
        return dto;
    }
}