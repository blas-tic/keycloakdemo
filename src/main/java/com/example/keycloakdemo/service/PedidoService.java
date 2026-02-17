package com.example.keycloakdemo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.keycloakdemo.dto.ClienteResumenDTO;
import com.example.keycloakdemo.dto.PedidoItemRequestDTO;
import com.example.keycloakdemo.dto.PedidoItemResponseDTO;
import com.example.keycloakdemo.dto.PedidoRequestDTO;
import com.example.keycloakdemo.dto.PedidoResponseDTO;
import com.example.keycloakdemo.dto.PedidoResumenDTO;
import com.example.keycloakdemo.dto.ProductoResumenDTO;
import com.example.keycloakdemo.exception.ResourceNotFoundException;
import com.example.keycloakdemo.model.Cliente;
import com.example.keycloakdemo.model.Pedido;
import com.example.keycloakdemo.model.PedidoItem;
import com.example.keycloakdemo.model.Producto;
import com.example.keycloakdemo.repository.ClienteRepository;
import com.example.keycloakdemo.repository.PedidoRepository;
import com.example.keycloakdemo.repository.ProductoRepository;
import com.example.keycloakdemo.model.EstadoPedido;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;

    public PedidoResponseDTO crear(PedidoRequestDTO request) {
        log.info("Creando pedido para cliente ID: {}", request.getClienteId());


        // 1. Buscar cliente
        Cliente cliente = clienteRepository.findById(request.getClienteId())
            .orElseThrow(() -> new ResourceNotFoundException("No se encuentra cliente con ID: " + request.getClienteId()));

        // 2. Crear Pedido
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setFechaPedido(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);

        // 3. Procesar items y calcular total
        BigDecimal total = BigDecimal.ZERO;

        for (PedidoItemRequestDTO itemDto : request.getItems()) {
            // buscar producto
            Producto producto = productoRepository.findById(itemDto.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encuentra producto con ID: " + itemDto.getProductoId()));

            // verificar stock
            if (producto.getStock() < itemDto.getCantidad()) {
                throw new IllegalStateException(
                    String.format("Stock insuficiente para producto '%s'. Stock actual; %d, solicitado %d",
                        producto.getNombre(), producto.getStock(), itemDto.getCantidad()
                    )
                );
            }

            // crear item
            PedidoItem item = new PedidoItem();
            item.setPedido(pedido);
            item.setProducto(producto);
            item.setCantidad(itemDto.getCantidad());
            item.setPrecioUnitario(producto.getPrecio());

            BigDecimal subtotal = producto.getPrecio()
                .multiply(BigDecimal.valueOf(itemDto.getCantidad()));
            item.setSubtotal(subtotal);

            pedido.getItems().add(item);
            total = total.add(subtotal);

            // descontar stock
            producto.setStock(producto.getStock() - itemDto.getCantidad());
            productoRepository.save(producto);
        }

        pedido.setTotal(total);

        // 4. Guardar pedido (cascade guardará los items automáticamente)
        Pedido guardado = pedidoRepository.save(pedido);
        log.info("Pedido creado con ID: {}, total {}", guardado.getId(), guardado.getTotal());

        return toResponseDTO(guardado);
    }

    public List<PedidoResponseDTO> listarTodos() {
        return pedidoRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public PedidoResponseDTO obtenerPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        return toResponseDTO(pedido);
    }

    public List<PedidoResponseDTO> listarPorCliente(Long clienteId) {

        if(!clienteRepository.existsById(clienteId)) {
            throw new ResourceNotFoundException("Cliente no encontrado con ID: " + clienteId);
        }

        return pedidoRepository.findByClienteId(clienteId).stream()
            .map(this::toResponseDTO)
            .collect(Collectors.toList());
    }

    public PedidoResponseDTO actualizarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encuentra Pedido con ID: " + id));
        EstadoPedido estadoAnterior = pedido.getEstado();
        pedido.setEstado(nuevoEstado);

        Pedido actualizado = pedidoRepository.save(pedido);
        log.info("Pedido {} cambió de estado: {} -> {}", id, estadoAnterior, nuevoEstado);

        return toResponseDTO(actualizado);
    }

    public void cancelar(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encuentra Pedido con ID: " + id));

        // solo se puede cancelar si está PENDIENTE o CONFIRMADO
        if (pedido.getEstado() == EstadoPedido.ENVIADO ||
                pedido.getEstado() == EstadoPedido.ENTREGADO) {
            throw new IllegalStateException("No se puede cancelar un pedido que ya fue enviado o entregado");
        }

        // devolver stock
        for (PedidoItem item : pedido.getItems()) {
            Producto producto = item.getProducto();
            producto.setStock(producto.getStock() + item.getCantidad());
            productoRepository.save(producto);
        }

        // cambiar estado
        pedido.setEstado(EstadoPedido.CANCELADO);
        pedidoRepository.save(pedido);
        log.info("Pedido {} cancelado. Stock devuelto.", id);
    }

    private PedidoResponseDTO toResponseDTO(Pedido pedido) {
        PedidoResponseDTO dto = new PedidoResponseDTO();
        dto.setId(pedido.getId());
        dto.setCliente(toClienteResumenDTO(pedido.getCliente()));
        dto.setFechaPedido(pedido.getFechaPedido());
        dto.setEstado(pedido.getEstado());
        dto.setTotal(pedido.getTotal());

        // mapear items
        if (pedido.getItems() != null && !pedido.getItems().isEmpty()) {
            dto.setItems(pedido.getItems().stream()
                    .map(this::toItemResponseDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private PedidoItemResponseDTO toItemResponseDTO(PedidoItem item) {
        PedidoItemResponseDTO dto = new PedidoItemResponseDTO();
        dto.setId(item.getId());
        dto.setProducto(toProductoResumenDTO(item.getProducto()));
        dto.setCantidad(item.getCantidad());
        dto.setPrecioUnitario(item.getPrecioUnitario());
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }

    private ClienteResumenDTO toClienteResumenDTO(Cliente cliente) {
        ClienteResumenDTO dto = new ClienteResumenDTO();
        dto.setId(cliente.getId());
        dto.setNombre(cliente.getNombre());
        dto.setEmail(cliente.getEmail());
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

    // Para usar en ClienteService al devolver pedidos del cliente
    public PedidoResumenDTO toPedidoResumenDTO(Pedido pedido) {
        PedidoResumenDTO dto = new PedidoResumenDTO();
        dto.setId(pedido.getId());
        dto.setFechaPedido(pedido.getFechaPedido());
        dto.setTotal(pedido.getTotal());
        dto.setEstado(pedido.getEstado());

        // Calcular cantidad total de items
        int cantidadTotal = pedido.getItems().stream()
                .mapToInt(PedidoItem::getCantidad)
                .sum();
        dto.setCantidadItems(cantidadTotal);

        return dto;
    }

}
