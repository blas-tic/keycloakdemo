package com.example.keycloakdemo.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class KeycloakJwtAuthenticationConverter 
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        // Extraer roles del resource access
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        
        if (resourceAccess == null) {
            return List.of();
        }

        Map<String, Object> client = (Map<String, Object>) resourceAccess.get("demo-client");
        if (client == null) {
            return List.of();
        }

        List<String> roles = (List<String>) client.get("roles");
        if (roles == null) {
            return List.of();
        }

        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());
    }
}

