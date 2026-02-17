package com.example.keycloakdemo.service;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class KeycloakAdminService {

   @Value("${keycloak.auth-server-url}")
   private String serverUrl;

   @Value("${keycloak.realm}")
   private String realm;

   @Value("${keycloak.admin.username}")
   private String adminUsername;

   @Value("${keycloak.admin.password}")
   private String adminPassword;

   @Value("${keycloak.admin.realm}")
   private String adminRealm;

   @Value("${keycloak.admin.client-id}")
   private String adminClientId;

   @Value("${keycloak.resource}")
   private String clientId; // demo-client

   private Keycloak keycloak;

   @PostConstruct
   public void init() {
      this.keycloak = KeycloakBuilder.builder()
            .serverUrl(serverUrl)
            .realm(adminRealm)
            .username(adminUsername)
            .password(adminPassword)
            .clientId(adminClientId)
            .build();
   }

   /**
    * Crea un usuario en Keycloak con rol CLIENTE
    * 
    * @param username nombre de usuario
    * @param email    email del usuario
    * @param password contraseña temporal
    * @param nombre   nombre completo
    * @return ID del usuario creado en Keycloak
    */
   public String crearUsuarioCliente(String username, String email, String password, String nombre) {
      RealmResource realmResource = keycloak.realm(realm);
      UsersResource usersResource = realmResource.users();

      // Crear representación del usuario
      UserRepresentation user = new UserRepresentation();
      user.setUsername(username);
      user.setEmail(email);
      user.setFirstName(nombre.split(" ")[0]);
      if (nombre.contains(" ")) {
         user.setLastName(nombre.substring(nombre.indexOf(" ") + 1));
      }
      user.setEnabled(true);
      user.setEmailVerified(false); // Puedes cambiarlo a true si no requieres verificación

      // Crear usuario
      Response response = usersResource.create(user);

      try {
         if (response.getStatus() != 201) {
            String errorBody = response.readEntity(String.class);

            log.error("Error al crear usuario en Keycloak. Status: {}, Body: {}",
                  response.getStatus(),
                  errorBody);
            throw new RuntimeException("Error al crear usuario en Keycloak: " + response.getStatusInfo());
         }

         // Obtener ID del usuario creado desde el header Location
         String locationHeader = response.getHeaderString("Location");
         if (locationHeader == null || locationHeader.isEmpty()) {
            log.error("No se recibió el header Location en la respuesta de Keycloak");
            throw new RuntimeException("No se pudo obtener el ID del usuario creado en Keycloak");
         }

         // Obtener ID del usuario creado
         String userId = locationHeader.substring(locationHeader.lastIndexOf('/') + 1);
         log.info("Usuario creado en Keycloak con ID: {}", userId);
         // Establecer contraseña
         CredentialRepresentation credential = new CredentialRepresentation();
         credential.setType(CredentialRepresentation.PASSWORD);
         credential.setValue(password);
         credential.setTemporary(true); // El usuario deberá cambiarla en el primer login
         UserResource userResource = usersResource.get(userId);
         userResource.resetPassword(credential);
         log.info("Contraseña establecida para usuario: {}", userId);

         // Asignar rol CLIENTE del client demo-client
         asignarRolCliente(userId);

         // respuesta correcta
         return userId;

      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         response.close();
      }

      return null;

   }

   /**
    * Asigna el rol CLIENTE del client demo-client al usuario
    */
   private void asignarRolCliente(String userId) {
      RealmResource realmResource = keycloak.realm(realm);
      UserResource userResource = realmResource.users().get(userId);

      // Obtener el client demo-client
      String clientUuid = realmResource.clients()
            .findByClientId(clientId)
            .get(0)
            .getId();

      // Obtener el rol CLIENTE del client
      RoleRepresentation clientRole = realmResource.clients()
            .get(clientUuid)
            .roles()
            .get("CLIENTE")
            .toRepresentation();

      // Asignar el rol al usuario
      userResource.roles()
            .clientLevel(clientUuid)
            .add(Collections.singletonList(clientRole));

      log.info("Rol CLIENTE asignado al usuario {}", userId);
   }

   /**
    * Actualiza el email de un usuario en Keycloak
    */
   public void actualizarEmail(String keycloakUserId, String nuevoEmail) {
      RealmResource realmResource = keycloak.realm(realm);
      UserResource userResource = realmResource.users().get(keycloakUserId);

      UserRepresentation user = userResource.toRepresentation();
      user.setEmail(nuevoEmail);

      userResource.update(user);
      log.info("Email actualizado para usuario {}", keycloakUserId);
   }

   /**
    * Elimina un usuario de Keycloak
    */
   public void eliminarUsuario(String keycloakUserId) {
      RealmResource realmResource = keycloak.realm(realm);
      UsersResource usersResource = realmResource.users();

      Response response = usersResource.delete(keycloakUserId);

      if (response.getStatus() == 204) {
         log.info("Usuario {} eliminado de Keycloak", keycloakUserId);
      } else {
         log.error("Error al eliminar usuario de Keycloak. Status: {}", response.getStatus());
      }

      response.close();
   }

   /**
    * Resetea la contraseña de un usuario
    */
   public void resetearPassword(String keycloakUserId, String nuevaPassword) {
      RealmResource realmResource = keycloak.realm(realm);
      UserResource userResource = realmResource.users().get(keycloakUserId);

      CredentialRepresentation credential = new CredentialRepresentation();
      credential.setType(CredentialRepresentation.PASSWORD);
      credential.setValue(nuevaPassword);
      credential.setTemporary(true);

      userResource.resetPassword(credential);
      log.info("Contraseña reseteada para usuario {}", keycloakUserId);
   }

   /**
    * Verifica si existe un usuario con el username dado
    * 
    * @param username nombre de usuario a verificar
    * @return true si existe, false si no
    */
   public boolean existeUsername(String username) {
      RealmResource realmResource = keycloak.realm(realm);
      UsersResource usersResource = realmResource.users();

      // Buscar usuarios con ese username exacto
      List<UserRepresentation> users = usersResource.search(username, true);

      return !users.isEmpty();
   }

   /**
    * Verifica si existe un usuario con el email dado
    * 
    * @param email email a verificar
    * @return true si existe, false si no
    */
   public boolean existeEmail(String email) {
      RealmResource realmResource = keycloak.realm(realm);
      UsersResource usersResource = realmResource.users();

      // Buscar usuarios por email
      List<UserRepresentation> users = usersResource.searchByEmail(email, true);

      return !users.isEmpty();
   }
}