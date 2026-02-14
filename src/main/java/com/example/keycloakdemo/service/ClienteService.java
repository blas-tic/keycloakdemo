package com.example.keycloakdemo.service;

import com.example.keycloakdemo.model.*;
import com.example.keycloakdemo.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional
    public Cliente crearCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    public List<Cliente> obtenerTodos() {
        return clienteRepository.findAll();
    }

    public Cliente obtenerPorId(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));
    }

    public Cliente obtenerPorKeycloakUserId(String keycloakUserId) {
        return clienteRepository.findByKeycloakUserId(keycloakUserId)
            .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado"));
    }

    @Transactional
    public Cliente actualizarCliente(Long id, Cliente clienteActualizado) {
        Cliente cliente = obtenerPorId(id);
        cliente.setNombre(clienteActualizado.getNombre());
        cliente.setTelefono(clienteActualizado.getTelefono());
        cliente.setDireccion(clienteActualizado.getDireccion());
        return clienteRepository.save(cliente);
    }

    @Transactional
    public void eliminarCliente(Long id) {
        clienteRepository.deleteById(id);
    }

    public boolean esPropietario(Long clienteId, String keycloakUserId) {
        try {
            Cliente cliente = obtenerPorId(clienteId);
            return cliente.getKeycloakUserId().equals(keycloakUserId);
        } catch (EntityNotFoundException e) {
            return false;
        }
    }
}


