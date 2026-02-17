package com.example.keycloakdemo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.keycloakdemo.dto.ProductoRequestDTO;
import com.example.keycloakdemo.dto.ProductoResponseDTO;
import com.example.keycloakdemo.service.ProductoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Gestión de productos")
@SecurityRequirement(name = "bearerAuth")
public class ProductoController {

    private final ProductoService productoService;

    // ADMIN puede crear/actualizar/eliminar productos
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear nuevo producto", description = "Crea un producto. Le asigna la categoría cuyo ID va en la request.")
    public ResponseEntity<ProductoResponseDTO> crearProducto(@Valid @RequestBody ProductoRequestDTO producto) {
        ProductoResponseDTO nuevo = productoService.crearProducto(producto);
        return ResponseEntity.ok(nuevo);
    }

    // Todos los usuarios autenticados pueden ver productos
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar todos los productos", description = "Accesible por autenticados")
    public ResponseEntity<List<ProductoResponseDTO>> obtenerTodosProductos() {
        return ResponseEntity.ok(productoService.obtenerTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener producto por ID", description = "Accesible por autenticados")
    public ResponseEntity<ProductoResponseDTO> obtenerProducto(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modificar producto por ID", description = "Sólo accesible por ADMIN")
    public ResponseEntity<ProductoResponseDTO> actualizarProducto(@PathVariable Long id,
            @Valid @RequestBody ProductoRequestDTO producto) {
        return ResponseEntity.ok(productoService.actualizarProducto(id, producto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar producto por ID", description = "Sólo accesible por ADMIN")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        productoService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }
}
