package com.example.keycloakdemo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
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

import com.example.keycloakdemo.dto.ClienteRequestDTO;
import com.example.keycloakdemo.dto.ClienteResponseDTO;
import com.example.keycloakdemo.service.ClienteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gestión de clientes")
@SecurityRequirement(name = "bearerAuth")
public class ClienteController {

    private final ClienteService clienteService;

    // Solo ADMIN puede crear clientes
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear nuevo cliente", description = "Solo accesible por ADMIN")
    public ResponseEntity<ClienteResponseDTO> crearCliente(@Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clienteService.crear(request));
    }

    // ADMIN PUEDE VER TODOS LOS CLIENTES
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @Operation(summary = "Listar todos los clientes", description = "Devuelve la lista completa de clientes registrados")
    public ResponseEntity<List<ClienteResponseDTO>> listarClientes() {
        return ResponseEntity.ok(clienteService.obtenerTodos());
    }

    // CLIENTE PUEDE VER SU PROPIO PERFIL. ADMIN PUEDE VER TODOS
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @Operation(summary = "Obtener cliente por ID")
    public ResponseEntity<ClienteResponseDTO> obtenerCliente(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    // CLIENTE puede actualizar su propio perfil
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @clienteService.esPropietario(#id, #jwt.subject)")
    @Operation(summary = "Actualizar cliente existente")
    public ResponseEntity<ClienteResponseDTO> actualizarCliente(@PathVariable Long id,
            @Valid @RequestBody ClienteRequestDTO request) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, request));
    }

    // Solo ADMIN puede eliminar clientes
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar cliente")
    public ResponseEntity<Void> eliminarCliente(@PathVariable Long id) {
        clienteService.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }

    // SOLO ADMIN --> RESETEAR CONTRASEÑA DE KEYCLOAK DE UN USUARIO
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resetear contraseña de cliente")
    public ResponseEntity<Void> resetearPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String nuevaPassword = body.get("password");
        if (nuevaPassword == null || nuevaPassword.length() < 8) {
            throw new RuntimeException("La contraseña debe tener al menos 8 caracteres");
        }

        clienteService.resetearPassword(id, nuevaPassword);
        return ResponseEntity.ok().build();
    }
}
