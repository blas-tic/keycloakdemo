package com.example.keycloakdemo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.keycloakdemo.exception.AuthorizationDeniedException;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticación", description = "Endpoints públicos para obtener tokens de Keycloak")
public class AuthController {

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;

    @PostMapping("/token")
    @Operation(
        summary = "Obtener token JWT",
        description = "Obtén un token de acceso con username y password.\n\n" +
                      "**Usuarios de prueba:**\n" +
                      "- admin / admin → rol ADMIN\n" +
                      "- cliente1 / cliente1 → rol CLIENTE\n\n" +
                      "Copia el `access_token` de la respuesta y úsalo en el botón **Authorize**."
    )
    public ResponseEntity<?> getToken(@Valid @RequestBody LoginRequest loginRequest) {

        String tokenUrl = keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        log.info("Solicitando token para usuario: {}", loginRequest.getUsername());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "password");
        body.add("username", loginRequest.getUsername());
        body.add("password", loginRequest.getPassword());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            log.info("Token obtenido para usuario: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response.getBody());

        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("Credenciales inválidas para usuario: {}", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));

        } catch (AuthorizationDeniedException e) {
            log.warn("Credenciales inválidas para usuario: {}", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales inválidas"));

        } catch (Exception e) {
            log.error("Error al obtener token: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al comunicarse con Keycloak"));
        }
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "El username es obligatorio")
        private String username;

        @NotBlank(message = "El password es obligatorio")
        private String password;
    }
}