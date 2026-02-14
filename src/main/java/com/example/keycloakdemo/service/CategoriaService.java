package com.example.keycloakdemo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.keycloakdemo.dto.CategoriaDTO;
import com.example.keycloakdemo.mapper.CategoriaMapper;
import com.example.keycloakdemo.model.Categoria;
import com.example.keycloakdemo.repository.CategoriaRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoriaService {

   private final CategoriaRepository categoriaRepository;
   private final CategoriaMapper categoriaMapper;

   @Transactional
   public Categoria crearCategoria(Categoria categoria) {
      return categoriaRepository.save(categoria);
   }

   public List<CategoriaDTO> obtenerTodas() {
      List<Categoria> categorias = categoriaRepository.findAll();
      return categoriaMapper.toDtoList(categorias);
   }

   public Categoria obtenerPorId(Long id) {
      return categoriaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Categoria no encontrada"));
   }

   @Transactional
   public Categoria actualizarCategoria(Long id, Categoria categoriaActualizada) {
      Categoria categoria = obtenerPorId(id);
      categoria.setNombre(categoriaActualizada.getNombre());
      categoria.setDescripcion(categoriaActualizada.getDescripcion());
      categoria.setProductos(categoriaActualizada.getProductos());
      return categoriaRepository.save(categoria);
   }

   @Transactional
   public void eliminarCategoria(Long id) {
      categoriaRepository.deleteById(id);
   }
}
