package com.example.keycloakdemo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.keycloakdemo.model.Cliente;
import com.example.keycloakdemo.service.ClienteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

   private final ClienteService clienteService;

   // Solo ADMIN puede crear clientes
   @PostMapping
   @PreAuthorize("hasRole('ADMIN')")
   public ResponseEntity<Cliente> crearCliente(@Valid @RequestBody Cliente cliente) {
      return new ResponseEntity<>(clienteService.crearCliente(cliente), HttpStatus.CREATED);
   }

   // ADMIN PUEDE VER TODOS LOS CLIENTES
   @GetMapping
   @PreAuthorize("hasRole('ADMIN')")
   public ResponseEntity<List<Cliente>> obtenerTodosClientes() {
      return ResponseEntity.ok(clienteService.obtenerTodos());
   }

   // CLIENTE PUEDE VER SU PROPIO PERFIL. ADMIN PUEDE VER TODOS
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @clienteService.esPropietario(#id, #jwt.subject)")
    public ResponseEntity<Cliente> obtenerCliente(@PathVariable Long id, 
                                                  @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(clienteService.obtenerPorId(id));
    }

    // CLIENTE puede actualizar su propio perfil
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @clienteService.esPropietario(#id, #jwt.subject)")
    public ResponseEntity<Cliente> actualizarCliente(@PathVariable Long id, 
                                                     @Valid @RequestBody Cliente cliente,
                                                     @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, cliente));
    }
    
    // Solo ADMIN puede eliminar clientes
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarCliente(@PathVariable Long id) {
        clienteService.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }    
}
