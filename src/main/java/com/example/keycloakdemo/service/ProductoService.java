package com.example.keycloakdemo.service;

import com.example.keycloakdemo.dto.CategoriaResumenDTO;
import com.example.keycloakdemo.dto.ProductoRequestDTO;
import com.example.keycloakdemo.dto.ProductoResponseDTO;
import com.example.keycloakdemo.dto.ProductoResumenDTO;
import com.example.keycloakdemo.exception.ResourceNotFoundException;
import com.example.keycloakdemo.model.*;
import com.example.keycloakdemo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductoService {

    private final ProductoRepository repo;
    private final CategoriaRepository categoriaRepository;

    public Producto crearProducto(Producto producto) {
        return repo.save(producto);
    }

    public ProductoResponseDTO crearProducto(ProductoRequestDTO request) {
        // buscar categoria
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada con ID: "
                        + request.getCategoriaId()));

        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setCategoria(categoria);
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());

        Producto guardado = repo.save(producto);
        log.info("Producto creado con ID: {}", guardado.getId());

        return toResponseDTO(producto);

    }

    private ProductoResponseDTO toResponseDTO(Producto producto) {

        CategoriaResumenDTO cDto = new CategoriaResumenDTO();
        cDto.setId(producto.getCategoria().getId());
        cDto.setNombre(producto.getCategoria().getNombre());

        ProductoResponseDTO dto = new ProductoResponseDTO();
        dto.setId(producto.getId());
        dto.setNombre(producto.getNombre());
        dto.setDescripcion(producto.getDescripcion());
        dto.setPrecio(producto.getPrecio());
        dto.setStock(producto.getStock());
        dto.setCategoria(cDto);

        return dto;
    }

    public List<ProductoResponseDTO> obtenerTodos() {
        List<Producto> productos = repo.findAll();

        return productos.stream()
            .map(this::toResponseDTO)
            .collect(Collectors.toList());
    }
    
    public ProductoResponseDTO obtenerPorId(Long id) {
        Producto producto = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        return toResponseDTO(producto);
    }

    public ProductoResponseDTO actualizarProducto(Long id, ProductoRequestDTO request) {
        Producto producto = repo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("No se encuentra producto con ID: " + id));

        // actualizar categoria si cambió
        if (!producto.getCategoria().getId().equals(request.getCategoriaId())){
            Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encuentra categoría con ID: " + request.getCategoriaId()));
            producto.setCategoria(categoria);
        }
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());

        Producto actualizado = repo.save(producto);
        log.info("Producto actualizado con ID: {}", actualizado.getId());

        return toResponseDTO(actualizado);
    }

    public void eliminarProducto(Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("No se encuentra producto con ID: " + id);
        }
        repo.deleteById(id);
        log.info("Producto eliminado con ID: {}", id);
    }

    // Método público para usar en PedidoService
    public ProductoResumenDTO toResumenDTO(Producto producto) {
        return toResumenDTO(producto);
    }

}
