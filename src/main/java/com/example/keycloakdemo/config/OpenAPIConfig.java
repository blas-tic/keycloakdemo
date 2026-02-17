package com.example.keycloakdemo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class OpenAPIConfig {
      
   @Value("${keycloak.auth-server-url}")
   private String keycloakServerUrl;

   @Value("${keycloak.realm}")
   private String realm;


   @Bean
   @Lazy(false)  // Fuerza la inicialización temprana
   public OpenAPI customOpenAPI() {

      final String securitySchemeName = "bearerAuth";
      final String oauth2SchemeName = "oauth2";

      return new OpenAPI()
            .addSecurityItem(new SecurityRequirement().addList(oauth2SchemeName))
            .components(new Components()
                  .addSecuritySchemes(securitySchemeName,
                        new SecurityScheme()
                              .name(securitySchemeName)
                              .type(SecurityScheme.Type.HTTP)
                              .scheme("bearer")
                              .bearerFormat("JWT")
                              .description("Pega aquí tu token JWT de Keycloak"))
            )
            .info(new Info()
                  .title("Keycloak Demo API")
                  .description("API REST con autenticación Keycloak para gestión de clientes, productos y pedidos")
                  .version("1.0.0")
                  .contact(new Contact()
                        .name("BlasTIC")
                        .email("blasandtic@gmail.com")
                        .url("https://github.com/blas-tic"))
                  .license(new License()
                        .name("MIT")
                        .url("https://opensource.org/licenses/MIT")));
   }
}