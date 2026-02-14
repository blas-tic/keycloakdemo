package com.example.keycloakdemo.service;

import com.example.keycloakdemo.dto.CategoriaResumenDTO;
import com.example.keycloakdemo.dto.ProductoRequestDTO;
import com.example.keycloakdemo.dto.ProductoResponseDTO;
import com.example.keycloakdemo.model.*;
import com.example.keycloakdemo.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository repo;
    private final CategoriaRepository categoriaRepository;

    @Transactional
    public Producto crearProducto(Producto producto) {
        return repo.save(producto);
    }

    public Producto crearProducto(ProductoRequestDTO dto) {
        Categoria categoria = categoriaRepository.findById(dto.getCategoriaID())
            .orElseThrow(() -> new EntityNotFoundException("Categoria no encontrada con ID: " 
                + dto.getCategoriaID()));
        Producto producto = new Producto();
        producto.setNombre(dto.getNombre());
        producto.setCategoria(categoria);
        producto.setDescripcion(dto.getDescripcion());
        producto.setPrecio(dto.getPrecio());
        producto.setStock(dto.getStock());

        return repo.save(producto);
    }

    public List<Producto> obtenerTodos() {
        return repo.findAll();
    }

    public Producto obtenerPorId(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));
    }

    @Transactional
    public Producto actualizarProducto(Long id, Producto ProductoActualizado) {
        Producto Producto = obtenerPorId(id);
        Producto.setNombre(ProductoActualizado.getNombre());
        Producto.setDescripcion(ProductoActualizado.getDescripcion());
        Producto.setPrecio(ProductoActualizado.getPrecio());
        Producto.setStock(ProductoActualizado.getStock());
        Producto.setCategoria(ProductoActualizado.getCategoria());
        return repo.save(Producto);
    }

    @Transactional
    public void eliminarProducto(Long id) {
        repo.deleteById(id);
    }

    public ProductoResponseDTO convertirADTO(Producto producto) {
        ProductoResponseDTO dto = new ProductoResponseDTO();
        dto.setId(producto.getId());
        dto.setNombre(producto.getNombre());
        dto.setDescripcion(producto.getDescripcion());
        dto.setPrecio(producto.getPrecio());
        dto.setStock(producto.getStock());
        if (producto.getCategoria() != null) {
            CategoriaResumenDTO cDto = new CategoriaResumenDTO();
            cDto.setId(producto.getCategoria().getId());
            cDto.setNombre(producto.getCategoria().getNombre());

            dto.setCategoria(cDto);
        }
        return dto;
    }

}


