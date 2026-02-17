package com.example.keycloakdemo.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.keycloakdemo.dto.CategoriaRequestDTO;
import com.example.keycloakdemo.dto.CategoriaResponseDTO;
import com.example.keycloakdemo.dto.CategoriaResumenDTO;
import com.example.keycloakdemo.dto.ProductoResumenDTO;
import com.example.keycloakdemo.exception.DuplicateResourceException;
import com.example.keycloakdemo.exception.ResourceNotFoundException;
import com.example.keycloakdemo.model.Categoria;
import com.example.keycloakdemo.model.Producto;
import com.example.keycloakdemo.repository.CategoriaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CategoriaService {

   private final CategoriaRepository categoriaRepository;

   public CategoriaResponseDTO crearCategoria(CategoriaRequestDTO request) {
      if (categoriaRepository.existsByNombre(request.getNombre())) {
         throw new DuplicateResourceException("Ya existe una categoría con el nombre {}" + request.getNombre());
      }
      Categoria categoria = toEntity(request);
      Categoria guardada = categoriaRepository.save(categoria);
      log.info("Categoría guardada con ID: {}", guardada.getId());

      return toResponseDTO(guardada);

   }

   public List<CategoriaResponseDTO> obtenerTodas() {
      List<Categoria> categorias = categoriaRepository.findAll();
      return categorias.stream()
            .map(this::toResponseDTOWithProducts)
            .collect(Collectors.toList());
   }

   public CategoriaResponseDTO obtenerPorId(Long id) {
      Categoria categoria = categoriaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("No se encuentra categoría con ID: " + id));

      return toResponseDTOWithProducts(categoria);
   }

   public CategoriaResponseDTO actualizarCategoria(Long id, CategoriaRequestDTO request) {
      Categoria categoria = categoriaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("No se encuentra categoría con ID: " + id));

      // verificar duplicados
      categoriaRepository.findByNombre(request.getNombre())
            .ifPresent(existente -> {
               if (!existente.getId().equals(id)) {
                  throw new DuplicateResourceException("Ya existe otra categoría con ese nombre");
               }
            });

      categoria.setNombre(request.getNombre());
      categoria.setDescripcion(request.getDescripcion());

      Categoria actualizada = categoriaRepository.save(categoria);
      log.info("Categoría actualizada con ID: {}", actualizada.getId());

      return toResponseDTO(categoria);
   }

   public void eliminarCategoria(Long id) {
      if (!categoriaRepository.existsById(id)) {
         throw new ResourceNotFoundException(("No se encuentra categoria con ID: ") + id);
      }
      categoriaRepository.deleteById(id);
      log.info("Categoría eliminada con ID: {}", id);
   }

   // Método público para usar en ProductoService
   public CategoriaResumenDTO toResumenDTO(Categoria categoria) {
      return toResumenDTO(categoria);
   }

   // Mapeo manual de productos para evitar problemas con MapStruct
   private CategoriaResponseDTO toResponseDTOWithProducts(Categoria categoria) {
      CategoriaResponseDTO dto = toResponseDTO(categoria);

      // Mapear productos manualmente
      if (categoria.getProductos() != null && !categoria.getProductos().isEmpty()) {
         List<ProductoResumenDTO> productosResumen = categoria.getProductos().stream()
               .map(this::toProductoResumenDTO)
               .collect(Collectors.toList());
         dto.setProductos(productosResumen);
      }

      return dto;
   }

   private ProductoResumenDTO toProductoResumenDTO(Producto producto) {
      ProductoResumenDTO dto = new ProductoResumenDTO();
      dto.setId(producto.getId());
      dto.setNombre(producto.getNombre());
      dto.setPrecio(producto.getPrecio());
      dto.setStock(producto.getStock());
      return dto;
   }

   private Categoria toEntity(CategoriaRequestDTO request) {
      Categoria categoria = new Categoria();
      categoria.setNombre(request.getNombre());
      categoria.setDescripcion(request.getDescripcion());
      return categoria;
   }

   private CategoriaResponseDTO toResponseDTO(Categoria categoria) {
      CategoriaResponseDTO dto = new CategoriaResponseDTO();
      dto.setId(categoria.getId());
      dto.setNombre(categoria.getNombre());
      dto.setDescripcion(categoria.getDescripcion());

      if (categoria.getProductos() != null && !categoria.getProductos().isEmpty()) {
         dto.setProductos(categoria.getProductos().stream()
               .map(this::toProductoResumenDTO)
               .collect(Collectors.toList()));
      }

      return dto;
   }
}
