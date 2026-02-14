# Keycloak Demo - Spring Boot REST API

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://www.oracle.com/java/technologies/javase-jdk21-downloads.html)
[![Keycloak](https://img.shields.io/badge/Keycloak-22.0.1-purple)](https://www.keycloak.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)](https://www.postgresql.org/)

Proyecto de demostración de una API REST segura con **Spring Boot**, **JPA**, **PostgreSQL** y autenticación/autorización mediante **Keycloak**. Implementa roles `ADMIN` y `CLIENTE` para gestionar entidades como `Cliente`, `Producto`, `Categoría` y `Pedido`.

## Tabla de Contenidos
- [Tecnologías](#tecnologías)
- [Requisitos Previos](#requisitos-previos)
- [Configuración de Keycloak](#configuración-de-keycloak)
- [Configuración de Base de Datos](#configuración-de-base-de-datos)
- [Configuración de la Aplicación](#configuración-de-la-aplicación)
- [Ejecutar la Aplicación](#ejecutar-la-aplicación)
- [Uso de la API](#uso-de-la-api)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Manejo de Relaciones Bidireccionales (DTOs)](#manejo-de-relaciones-bidireccionales-dtos)
- [Próximos Pasos](#próximos-pasos)
- [Licencia](#licencia)

## Tecnologías
- **Spring Boot 4.0.2**
- **Java 21**
- **Spring Data JPA**
- **Spring Security** + OAuth2 Resource Server
- **Keycloak 22.0.1** (contenedor Docker)
- **PostgreSQL 15** (contenedor Docker)
- **Lombok**
- **Maven**

## Requisitos Previos
- [Docker](https://www.docker.com/) y [Podman](https://podman.io/) (o Docker) instalados.
- [Java 21](https://adoptium.net/)
- [Maven](https://maven.apache.org/)
- Cliente REST (curl, Postman, etc.)

## Configuración de Keycloak

### 1. Levantar Keycloak
```bash
docker run -d --name keycloak \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:22.0.1 start-dev
```

### 2. Crear Realm y Cliente
Accede a la consola de administración: [http://localhost:8180](http://localhost:8180) (usuario: `admin`, contraseña: `admin`).

- **Realm**: `demo-realm`
- **Cliente**: `demo-client`
  - *Capability config*: marcar **Client authentication** (ON), **Standard flow** (ON), **Direct access grants** (ON) para desarrollo.
  - *Login settings*: `Valid redirect URIs`: `http://localhost:8080/*`, `Web origins`: `*`

### 3. Crear Roles de Cliente
Dentro del cliente `demo-client`, ve a la pestaña **Roles** y crea:
- `ADMIN`
- `CLIENTE`

### 4. Crear Usuarios y Asignar Roles
- **Usuario admin**: username `admin`, password `admin`. En la pestaña **Role mapping**, asigna el rol `ADMIN` del cliente `demo-client`.
- **Usuario cliente**: username `cliente1`, password `cliente1`. Asigna el rol `CLIENTE`.

### 5. Configurar Mapper para Incluir Roles en el Token
Para que los roles de cliente aparezcan en el JWT bajo `resource_access.demo-client.roles`:

- En el cliente `demo-client`, ve a la pestaña **Client scopes**.
- Haz clic en el enlace **`demo-client-dedicated`** (o créalo si no existe).
- Pestaña **Mappers** → **Add mapper** → **By configuration** → **User Client Role**.
- Configurar:
  - **Name**: `client roles`
  - **Client ID**: `demo-client`
  - **Token Claim Name**: `resource_access.${client_id}.roles`
  - **Add to access token**: `ON`
  - **Multivalued**: `ON`
- Guardar.

Luego, en la pestaña **Scope** del dedicated scope, activa **Full Scope Allowed**.

> **Nota**: Si el dedicated scope aparece como "None" en la columna **Assigned Type** de la lista de client scopes, reasígnelo como **Default** desde la misma pantalla.

### 6. Obtener Client Secret
En el cliente `demo-client`, pestaña **Credentials**, copia el **Client secret**. Lo usarás en la aplicación.

## Configuración de Base de Datos

### 1. Levantar PostgreSQL
```bash
podman run -d --name postgres \
  -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -v ./data/postgres:/var/lib/postgresql/data:Z \
  docker.io/postgres:15-alpine
```

### 2. Crear Base de Datos y Usuario para la Aplicación
Conéctate a PostgreSQL:
```bash
podman exec -it postgres psql -U postgres
```
Ejecuta:
```sql
CREATE USER appuser WITH PASSWORD 'apppass';
CREATE DATABASE appdb OWNER appuser;
\c appdb;
GRANT ALL ON SCHEMA public TO appuser;
ALTER SCHEMA public OWNER TO appuser;
```
Sal con `\q`.

## Configuración de la Aplicación

Edita `src/main/resources/application.properties`:

```properties
spring.application.name=keycloak-demo

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/appdb
spring.datasource.username=appuser
spring.datasource.password=apppass
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Keycloak / OAuth2
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/demo-realm
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/demo-realm/protocol/openid-connect/certs

keycloak.realm=demo-realm
keycloak.auth-server-url=http://localhost:8180
keycloak.resource=demo-client
keycloak.credentials.secret=<tu-client-secret>
keycloak.use-resource-role-mappings=true
keycloak.bearer-only=true

# Logging
logging.level.org.springframework.security=DEBUG
logging.level.com.example.keycloakdemo=DEBUG
```

Reemplaza `<tu-client-secret>` con el secreto obtenido de Keycloak.

## Ejecutar la Aplicación

```bash
mvn spring-boot:run
```

La aplicación arrancará en `http://localhost:8080`.

## Uso de la API

### Obtener Token (usuario admin)
```bash
curl -X POST http://localhost:8180/realms/demo-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=demo-client" \
  -d "client_secret=<tu-client-secret>" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin"
```
Guarda el `access_token` de la respuesta.

### Crear una Categoría (solo ADMIN)
```bash
curl -X POST http://localhost:8080/api/categorias \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"nombre": "Electrónica", "descripcion": "Productos electrónicos"}'
```

### Crear un Producto (solo ADMIN)
```bash
curl -X POST http://localhost:8080/api/productos \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Smartphone",
    "descripcion": "Teléfono de última generación",
    "precio": 599.99,
    "stock": 10,
    "categoriaId": 1
  }'
```

### Listar Categorías (autenticado)
```bash
curl -X GET http://localhost:8080/api/categorias \
  -H "Authorization: Bearer <token>"
```

> **Nota**: Actualmente este endpoint puede producir **recursión infinita** debido a relaciones bidireccionales. La solución se explica en la sección [Manejo de Relaciones Bidireccionales](#manejo-de-relaciones-bidireccionales-dtos).

## Estructura del Proyecto

```
src/main/java/com/example/keycloakdemo/
├── config/
│   ├── KeycloakSecurityConfig.java
│   ├── KeycloakJwtAuthenticationConverter.java
│   └── CorsConfig.java
├── controller/
│   ├── ClienteController.java
│   ├── ProductoController.java
│   ├── CategoriaController.java
│   └── PedidoController.java
├── dto/                      ← DTOs para peticiones/respuestas
│   ├── ProductoRequestDTO.java
│   ├── ProductoResponseDTO.java
│   └── CategoriaResumenDTO.java
├── model/
│   ├── Cliente.java
│   ├── Producto.java
│   ├── Categoria.java
│   ├── Pedido.java
│   └── PedidoItem.java
├── repository/
│   ├── ClienteRepository.java
│   ├── ProductoRepository.java
│   ├── CategoriaRepository.java
│   └── PedidoRepository.java
├── service/
│   ├── ClienteService.java
│   ├── ProductoService.java
│   ├── CategoriaService.java
│   └── PedidoService.java
└── KeycloakDemoApplication.java
```

## Manejo de Relaciones Bidireccionales (DTOs)

El proyecto utiliza DTOs (Data Transfer Objects) para evitar la **recursión infinita** al serializar entidades con relaciones JPA bidireccionales (ej. `Categoria` ↔ `Producto`).

**Ejemplo de DTOs**:

- `ProductoRequestDTO`: Contiene `categoriaId` en lugar de la entidad completa.
- `ProductoResponseDTO`: Incluye una versión resumida de la categoría (`CategoriaResumenDTO`) sin la lista de productos.

Para listas, se recomienda usar un **framework de mapeo** como **MapStruct** para automatizar las conversiones entre entidades y DTOs, evitando código manual repetitivo y errores. La implementación de MapStruct está planificada como próximo paso.

## Próximos Pasos

1. **Implementar MapStruct** para automatizar el mapeo entidad ↔ DTO.
2. **Crear DTOs específicos para cada endpoint de lista** (ej. `CategoriaListDTO` con productos resumidos).
3. **Optimizar consultas** con `@EntityGraph` o JPQL para evitar problemas N+1.
4. **Agregar validaciones** con Bean Validation en los DTOs.
5. **Escribir pruebas** unitarias e de integración.

## Licencia

Este proyecto es de código abierto y está disponible bajo la licencia MIT.
