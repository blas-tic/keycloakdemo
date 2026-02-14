package com.example.keycloakdemo.mapper;


import com.example.keycloakdemo.dto.CategoriaDTO;
import com.example.keycloakdemo.dto.ProductoRequestDTO;
import com.example.keycloakdemo.model.Categoria;
import com.example.keycloakdemo.model.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    // Mapeo de Categoria a CategoriaDTO
    @Mapping(source = "productos", target = "productos", qualifiedByName = "toProductoResumenList")
    CategoriaDTO toDto(Categoria categoria);

    List<CategoriaDTO> toDtoList(List<Categoria> categorias);

    @Named("toProductoResumenList")
    List<ProductoRequestDTO> toProductoResumenList(List<Producto> productos);

    @Named("toProductoResumen")
    @Mapping(target = "categoriaID", source = "categoria.id")
    ProductoRequestDTO toProductoResumen(Producto producto);
}
