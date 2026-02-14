package com.example.keycloakdemo.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.keycloakdemo.model.Cliente;
import com.example.keycloakdemo.model.Pedido;
import com.example.keycloakdemo.model.PedidoItem;
import com.example.keycloakdemo.model.Producto;
import com.example.keycloakdemo.repository.PedidoRepository;
import com.example.keycloakdemo.repository.ProductoRepository;
import com.example.keycloakdemo.model.EstadoPedido;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final ClienteService clienteService;

    @Transactional
    public Pedido crearPedido(Pedido pedido, String keycloakUserId) {
        Cliente cliente = clienteService.obtenerPorKeycloakUserId(keycloakUserId);
        pedido.setCliente(cliente);
        
        BigDecimal total = BigDecimal.ZERO;
        for (PedidoItem item : pedido.getItems()) {
            item.setPedido(pedido);
            Producto producto = productoRepository.findById(item.getProducto().getId())
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado"));
            
            item.setProducto(producto);
            item.setPrecioUnitario(producto.getPrecio());
            item.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad())));
            total = total.add(item.getSubtotal());
            
            // Actualizar stock
            producto.setStock(producto.getStock() - item.getCantidad());
        }
        
        pedido.setTotal(total);
        return pedidoRepository.save(pedido);
    }

    public List<Pedido> obtenerTodos() {
        return pedidoRepository.findAll();
    }

    public Pedido obtenerPorId(Long id) {
        return pedidoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado"));
    }

    public List<Pedido> obtenerPedidosPorCliente(String keycloakUserId) {
        return pedidoRepository.findByClienteKeycloakUserId(keycloakUserId);
    }

    @Transactional
    public Pedido actualizarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = obtenerPorId(id);
        pedido.setEstado(nuevoEstado);
        return pedidoRepository.save(pedido);
    }

    public boolean esPropietario(Long pedidoId, String keycloakUserId) {
        try {
            Pedido pedido = obtenerPorId(pedidoId);
            return pedido.getCliente().getKeycloakUserId().equals(keycloakUserId);
        } catch (EntityNotFoundException e) {
            return false;
        }
    }
}
