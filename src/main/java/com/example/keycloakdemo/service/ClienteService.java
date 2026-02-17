package com.example.keycloakdemo.service;

import com.example.keycloakdemo.dto.ClienteRequestDTO;
import com.example.keycloakdemo.dto.ClienteResponseDTO;
import com.example.keycloakdemo.dto.PedidoResumenDTO;
import com.example.keycloakdemo.exception.DuplicateResourceException;
import com.example.keycloakdemo.exception.KeycloakOperationException;
import com.example.keycloakdemo.exception.ResourceNotFoundException;
import com.example.keycloakdemo.model.*;
import com.example.keycloakdemo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.
springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final PedidoService pedidoService;

    public ClienteResponseDTO crear(ClienteRequestDTO request) {

        log.info("Iniciando creación de cliente: {}", request.getEmail());

        // 1. Verificar que el email no esté duplicado en la b.d. local
        if (clienteRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "Ya existe un cliente con ese email");
        }
        // 2. Validar que el username no esté duplicado en Keycloak
        if (keycloakAdminService.existeUsername(request.getUsername())) {
            throw new DuplicateResourceException("El nombre de usuario ya está en uso en Keycloak");
        }
        // 3. Validar que el email no esté duplicado en Keycloak
        if (keycloakAdminService.existeEmail(request.getEmail())) {
            throw new DuplicateResourceException("El email ya está registrado en Keycloak");
        }
        try {
            // 4. Crear usuario en Keycloak
            String keycloakUserId = keycloakAdminService.crearUsuarioCliente(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getNombre());
            // VERIFICACIÓN: El keycloakUserId NO debe ser null
            if (keycloakUserId == null || keycloakUserId.isEmpty()) {
                throw new KeycloakOperationException("No se obtuvo el ID de usuario de Keycloak");
            }
            
            // 5. Crear cliente en la base de datos
            Cliente cliente = new Cliente();
            cliente.setNombre(request.getNombre());
            cliente.setEmail(request.getEmail());
            cliente.setTelefono(request.getTelefono());
            cliente.setDireccion(request.getDireccion());
            cliente.setKeycloakUserId(keycloakUserId);
            cliente.setFechaRegistro(LocalDateTime.now());

            log.info("Guardando cliente en la b.d. con keycloakUserId: {}", keycloakUserId);

            Cliente guardado = clienteRepository.save(cliente);
            log.info("Cliente creado con ID: {} y Keycloak User ID: {}", guardado.getId(), keycloakUserId);

            return toResponseDTO(guardado);
        } catch (DuplicateResourceException e) {
            throw e; // Relanzar las excepciones de duplicados sin envolver
        } catch (KeycloakOperationException e) {
            throw e; // Re-lanzar excepciones de Keycloak            
        } catch (Exception e) {
            log.error("Error inesperado al crear cliente: {}", e.getMessage());
            throw new RuntimeException("Error al crear cliente: " + e.getMessage(), e);
        }
    }

    public List<ClienteResponseDTO> obtenerTodos() {
        return clienteRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public ClienteResponseDTO obtenerPorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        return toResponseDTO(cliente);
    }

    public Cliente obtenerPorKeycloakUserId(String keycloakUserId) {
        return clienteRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
    }

    public ClienteResponseDTO actualizarCliente(Long id, ClienteRequestDTO request) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        // actualizar datos locales
        cliente.setNombre(request.getNombre());
        cliente.setTelefono(request.getTelefono());
        cliente.setDireccion(request.getDireccion());
        // Si el email cambió, actualizar en Keycloak también
        if (!cliente.getEmail().equals(request.getEmail())) {
            keycloakAdminService.actualizarEmail(cliente.getKeycloakUserId(), request.getEmail());
            cliente.setEmail(request.getEmail());
        }
        Cliente actualizado = clienteRepository.save(cliente);
          log.info("Cliente actualizado con ID: {}", actualizado.getId());

        return toResponseDTO(actualizado);
    }

    public void eliminarCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        try {
            // 1. Eliminar de Keycloak
            keycloakAdminService.eliminarUsuario(cliente.getKeycloakUserId());

            // 2. Eliminar de la base de datos
            clienteRepository.deleteById(id);
            log.info("Cliente eliminado con ID: {}", id);

        } catch (Exception e) {
            log.error("Error al eliminar cliente: {}", e.getMessage());
            throw new RuntimeException("Error al eliminar cliente: " + e.getMessage(), e);
        }
    }

    /**
     * Resetear contraseña de un cliente
     */
    public void resetearPassword(Long id, String nuevaPassword) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        keycloakAdminService.resetearPassword(cliente.getKeycloakUserId(), nuevaPassword);
        log.info("Contraseña reseteada para cliente ID: {}", id);
    }

    public boolean esPropietario(Long clienteId, String keycloakUserId) {
        try {
            Cliente cliente = clienteRepository.findById(clienteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
            return cliente.getKeycloakUserId().equals(keycloakUserId);
        } catch (Exception e) {
            log.error("esPropietario. Error: " + e.getLocalizedMessage());
            return false;
        }
    }

    // Mapeo manual
    private ClienteResponseDTO toResponseDTO(Cliente cliente) {
        ClienteResponseDTO dto = new ClienteResponseDTO();
        dto.setId(cliente.getId());
        dto.setNombre(cliente.getNombre());
        dto.setEmail(cliente.getEmail());
        dto.setTelefono(cliente.getTelefono());
        dto.setDireccion(cliente.getDireccion());
        dto.setFechaRegistro(cliente.getFechaRegistro());

        // Mapear pedidos a resumen (sin items completos)
        if (cliente.getPedidos() != null && !cliente.getPedidos().isEmpty()) {
            dto.setPedidos(cliente.getPedidos().stream()
                    .map(pedidoService::toPedidoResumenDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

}
