package com.example.keycloakdemo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.keycloakdemo.model.EstadoPedido;
import com.example.keycloakdemo.model.Pedido;
import com.example.keycloakdemo.service.PedidoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    // CLIENTE puede crear sus propios pedidos
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Pedido> crearPedido(@Valid @RequestBody Pedido pedido,
                                              @AuthenticationPrincipal Jwt jwt) {
        return new ResponseEntity<>(pedidoService.crearPedido(pedido, jwt.getSubject()), 
                                  HttpStatus.CREATED);
    }

    // ADMIN puede ver todos los pedidos, CLIENTE solo sus propios pedidos
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Pedido>> obtenerTodosPedidos() {
        return ResponseEntity.ok(pedidoService.obtenerTodos());
    }

    @GetMapping("/mis-pedidos")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<Pedido>> obtenerMisPedidos(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(pedidoService.obtenerPedidosPorCliente(jwt.getSubject()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @pedidoService.esPropietario(#id, #jwt.subject)")
    public ResponseEntity<Pedido> obtenerPedido(@PathVariable Long id,
                                                @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(pedidoService.obtenerPorId(id));
    }

    // Solo ADMIN puede actualizar estado de pedidos
    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Pedido> actualizarEstadoPedido(@PathVariable Long id,
                                                         @RequestParam EstadoPedido estado) {
        return ResponseEntity.ok(pedidoService.actualizarEstado(id, estado));
    }
}
