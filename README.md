# Keycloak Demo - Spring Boot REST API

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://www.oracle.com/java/technologies/javase-jdk21-downloads.html)
[![Keycloak](https://img.shields.io/badge/Keycloak-22.0.1-purple)](https://www.keycloak.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)](https://www.postgresql.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

API REST segura con **Spring Boot 4**, **JPA**, **PostgreSQL** y autenticación/autorización mediante **Keycloak**. Implementa roles `ADMIN` y `CLIENTE` para gestionar entidades como `Cliente`, `Producto`, `Categoría` y `Pedido`, con documentación interactiva via Swagger UI.

## Tabla de Contenidos

- [Características](#características)
- [Tecnologías](#tecnologías)
- [Requisitos Previos](#requisitos-previos)
- [Configuración de Keycloak](#configuración-de-keycloak)
- [Configuración de Base de Datos](#configuración-de-base-de-datos)
- [Configuración de la Aplicación](#configuración-de-la-aplicación)
- [Ejecutar la Aplicación](#ejecutar-la-aplicación)
- [Documentación de la API (Swagger)](#documentación-de-la-api-swagger)
- [Uso de la API](#uso-de-la-api)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Decisiones de Diseño](#decisiones-de-diseño)
- [Próximos Pasos](#próximos-pasos)
- [Licencia](#licencia)

---

## Características

- ✅ **API REST completa**: CRUD para Cliente, Producto, Categoría, Pedido y PedidoItem
- ✅ **Autenticación JWT** con Keycloak como Identity Provider
- ✅ **Autorización por roles**: `ADMIN` (acceso total) y `CLIENTE` (solo lectura y pedidos propios)
- ✅ **Gestión de usuarios sincronizada**: crear un Cliente crea automáticamente su usuario en Keycloak
- ✅ **DTOs completos**: patrón Request/Response/Resumen para todos los recursos
- ✅ **Manejo de excepciones**: respuestas HTTP semánticamente correctas (400, 403, 404, 409, 500)
- ✅ **Validaciones**: Bean Validation en todos los DTOs de entrada
- ✅ **Documentación interactiva**: Swagger UI con autenticación Bearer integrada
- ✅ **Gestión de stock**: verificación y descuento automático al crear pedidos

---

## Tecnologías

| Tecnología | Versión | Uso |
|---|---|---|
| Spring Boot | 4.0.2 | Framework base |
| Java | 21 | Lenguaje |
| Spring Data JPA | (incluido en SB) | Persistencia |
| Spring Security + OAuth2 | (incluido en SB) | Seguridad |
| Keycloak | 22.0.1 | Identity Provider |
| PostgreSQL | 15 | Base de datos |
| SpringDoc OpenAPI | 2.6.0 | Documentación API |
| Keycloak Admin Client | 22.0.1 | Gestión programática de usuarios |
| Lombok | (incluido en SB) | Reducción de boilerplate |
| Maven | 3.x | Gestión de dependencias |

> **Nota sobre SpringDoc**: Se usa la versión 2.6.0 por compatibilidad con Spring Boot 4.0.2 + Spring 7.x. Versiones superiores de SpringDoc (2.7.x+) tienen incompatibilidades con `ControllerAdviceBean` en Spring 7.

---

## Requisitos Previos

- [Docker](https://www.docker.com/) o [Podman](https://podman.io/)
- [Java 21](https://adoptium.net/)
- [Maven 3.x](https://maven.apache.org/)
- Cliente REST para pruebas: [Postman](https://www.postman.com/), curl, o usa el Swagger UI integrado

---

## Configuración de Keycloak

### 1. Levantar Keycloak

```bash
docker run -d --name keycloak \
  -p 8180:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:22.0.1 start-dev
```

Accede a la consola de administración: [http://localhost:8180](http://localhost:8180)
- Usuario: `admin` | Contraseña: `admin`

### 2. Crear Realm y Cliente

- **Realm**: `demo-realm`
- **Cliente**: `demo-client`
  - *Capability config*: **Client authentication** ✅, **Standard flow** ✅, **Direct access grants** ✅
  - *Login settings*: `Valid redirect URIs`: `http://localhost:8080/*` | `Web origins`: `*`

### 3. Crear Roles de Cliente

Dentro del cliente `demo-client` → pestaña **Roles**, crea:
- `ADMIN`
- `CLIENTE`

### 4. Crear Usuarios Iniciales

| Username | Password | Rol |
|---|---|---|
| `admin` | `admin` | `ADMIN` |
| `cliente1` | `cliente1` | `CLIENTE` |

Para asignar roles: usuario → **Role mapping** → **Assign role** → filtra por cliente `demo-client`.

### 5. Configurar Mapper de Roles en el Token JWT

Para que los roles aparezcan en el JWT bajo `resource_access.demo-client.roles`:

1. En `demo-client` → **Client scopes** → clic en **`demo-client-dedicated`**
2. **Mappers** → **Add mapper** → **By configuration** → **User Client Role**
3. Configura:
   - **Name**: `client roles`
   - **Client ID**: `demo-client`
   - **Token Claim Name**: `resource_access.${client_id}.roles`
   - **Add to access token**: `ON`
   - **Multivalued**: `ON`
4. En la pestaña **Scope** del dedicated scope → activa **Full Scope Allowed**

> **Nota**: Si el dedicated scope aparece como "None" en **Assigned Type**, reasígnalo como **Default** desde la lista de client scopes.

### 6. Obtener Client Secret

`demo-client` → **Credentials** → copia el **Client secret**.

### 7. Configurar Keycloak Admin Client

La aplicación necesita conectarse al realm `master` para crear/eliminar usuarios programáticamente:

- Se usa el cliente `admin-cli` del realm `master` (viene preconfigurado en Keycloak)
- Usuario: el **Admin Permanente** creado al instalar Keycloak (`admin`/`admin`)

---

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

### 2. Crear Base de Datos y Usuario

```bash
podman exec -it postgres psql -U postgres
```

```sql
CREATE USER appuser WITH PASSWORD 'apppass';
CREATE DATABASE appdb OWNER appuser;
\c appdb;
GRANT ALL ON SCHEMA public TO appuser;
ALTER SCHEMA public OWNER TO appuser;
\q
```

---

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

# Keycloak / OAuth2 Resource Server
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/demo-realm
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/demo-realm/protocol/openid-connect/certs

keycloak.realm=demo-realm
keycloak.auth-server-url=http://localhost:8180
keycloak.resource=demo-client
keycloak.credentials.secret=<tu-client-secret>
keycloak.use-resource-role-mappings=true
keycloak.bearer-only=true

# Keycloak Admin Client (para gestión programática de usuarios)
keycloak.admin.realm=master
keycloak.admin.client-id=admin-cli
keycloak.admin.username=admin
keycloak.admin.password=admin

# Swagger / OpenAPI
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.tryItOutEnabled=true

# Logging
logging.level.org.springframework.security=DEBUG
logging.level.com.example.keycloakdemo=DEBUG
```

> ⚠️ **Seguridad**: No commitees `keycloak.credentials.secret` ni contraseñas reales. Usa variables de entorno en producción:
> ```properties
> keycloak.credentials.secret=${KEYCLOAK_CLIENT_SECRET}
> ```

---

## Ejecutar la Aplicación

```bash
mvn spring-boot:run
```

La aplicación arrancará en `http://localhost:8080`.

---

## Documentación de la API (Swagger)

La API incluye documentación interactiva con Swagger UI:

- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Cómo autenticarse en Swagger

**Paso 1**: Obtén un token usando el endpoint público de la propia API:

```
POST http://localhost:8080/auth/token

{
  "username": "admin",
  "password": "admin"
}
```

**Paso 2**: Copia el valor de `access_token` de la respuesta.

**Paso 3**: En Swagger UI, haz clic en **Authorize** → campo `bearerAuth` → pega el token → **Authorize**.

Ahora puedes usar todos los endpoints protegidos directamente desde el navegador.

---

## Uso de la API

### Endpoints disponibles

| Recurso | Método | Endpoint | Rol requerido |
|---|---|---|---|
| **Auth** | POST | `/auth/token` | Público |
| **Clientes** | GET | `/api/clientes` | ADMIN, CLIENTE |
| | GET | `/api/clientes/{id}` | ADMIN, CLIENTE |
| | POST | `/api/clientes` | ADMIN |
| | PUT | `/api/clientes/{id}` | ADMIN |
| | DELETE | `/api/clientes/{id}` | ADMIN |
| | POST | `/api/clientes/{id}/reset-password` | ADMIN |
| **Productos** | GET | `/api/productos` | ADMIN, CLIENTE |
| | GET | `/api/productos/{id}` | ADMIN, CLIENTE |
| | POST | `/api/productos` | ADMIN |
| | PUT | `/api/productos/{id}` | ADMIN |
| | DELETE | `/api/productos/{id}` | ADMIN |
| **Categorías** | GET | `/api/categorias` | ADMIN, CLIENTE |
| | GET | `/api/categorias/{id}` | ADMIN, CLIENTE |
| | POST | `/api/categorias` | ADMIN |
| | PUT | `/api/categorias/{id}` | ADMIN |
| | DELETE | `/api/categorias/{id}` | ADMIN |
| **Pedidos** | GET | `/api/pedidos` | ADMIN |
| | GET | `/api/pedidos/{id}` | ADMIN, CLIENTE |
| | GET | `/api/pedidos/cliente/{clienteId}` | ADMIN, CLIENTE |
| | POST | `/api/pedidos` | ADMIN, CLIENTE |
| | PATCH | `/api/pedidos/{id}/estado` | ADMIN |
| | DELETE | `/api/pedidos/{id}` | ADMIN, CLIENTE |

### Ejemplos con curl

#### Obtener token

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin"}' \
  | jq -r '.access_token')
```

#### Crear una categoría (solo ADMIN)

```bash
curl -X POST http://localhost:8080/api/categorias \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nombre": "Electrónica", "descripcion": "Productos electrónicos"}'
```

#### Crear un producto (solo ADMIN)

```bash
curl -X POST http://localhost:8080/api/productos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Smartphone",
    "descripcion": "Teléfono de última generación",
    "precio": 599.99,
    "stock": 10,
    "categoriaId": 1
  }'
```

#### Crear un cliente (solo ADMIN — crea usuario en Keycloak automáticamente)

```bash
curl -X POST http://localhost:8080/api/clientes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Juan Pérez",
    "email": "juan@example.com",
    "telefono": "123456789",
    "direccion": "Calle Falsa 123",
    "username": "juanperez",
    "password": "Password123!"
  }'
```

> La contraseña es temporal: el usuario deberá cambiarla en su primer login.

#### Crear un pedido

```bash
curl -X POST http://localhost:8080/api/pedidos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 1,
    "items": [
      {"productoId": 1, "cantidad": 2},
      {"productoId": 2, "cantidad": 1}
    ]
  }'
```

> El total se calcula automáticamente y el stock se descuenta al confirmar el pedido.

#### Actualizar estado de un pedido (solo ADMIN)

```bash
curl -X PATCH http://localhost:8080/api/pedidos/1/estado \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"estado": "CONFIRMADO"}'
```

Estados disponibles: `PENDIENTE` → `CONFIRMADO` → `EN_PREPARACION` → `ENVIADO` → `ENTREGADO` | `CANCELADO`

### Respuestas de error

La API devuelve errores en formato JSON estandarizado:

```json
{
  "status": 404,
  "message": "Producto no encontrado con ID: 99",
  "timestamp": "2026-02-17T10:30:00"
}
```

| Código | Situación |
|---|---|
| 400 | Datos inválidos, stock insuficiente, estado de pedido no permitido |
| 401 | Token ausente o inválido |
| 403 | Sin permisos para la operación |
| 404 | Recurso no encontrado |
| 409 | Recurso duplicado (email, username) |
| 500 | Error del servidor |

---

## Estructura del Proyecto

```
src/main/java/com/example/keycloakdemo/
├── config/
│   ├── KeycloakSecurityConfig.java           # Spring Security + OAuth2 Resource Server
│   ├── KeycloakJwtAuthenticationConverter.java  # Mapeo roles Keycloak → Spring Security
│   ├── OpenAPIConfig.java                    # Configuración Swagger/OpenAPI + Bearer Auth
│   ├── CorsConfig.java                       # Configuración CORS
│   └── RestTemplateConfig.java               # Bean RestTemplate para AuthController
├── controller/
│   ├── AuthController.java                   # POST /auth/token (público)
│   ├── ClienteController.java                # CRUD /api/clientes
│   ├── ProductoController.java               # CRUD /api/productos
│   ├── CategoriaController.java              # CRUD /api/categorias
│   └── PedidoController.java                 # CRUD /api/pedidos
├── dto/
│   ├── ClienteRequestDTO.java                # Crear cliente (incluye username/password para Keycloak)
│   ├── ClienteResponseDTO.java               # Cliente con pedidos resumidos
│   ├── ClienteResumenDTO.java                # id, nombre, email (para PedidoResponseDTO)
│   ├── ProductoRequestDTO.java               # Crear producto (con categoriaId)
│   ├── ProductoResponseDTO.java              # Producto con CategoriaResumenDTO
│   ├── ProductoResumenDTO.java               # id, nombre, precio, stock
│   ├── CategoriaRequestDTO.java              # Crear categoría
│   ├── CategoriaResponseDTO.java             # Categoría con lista de ProductoResumenDTO
│   ├── CategoriaResumenDTO.java              # id, nombre
│   ├── PedidoRequestDTO.java                 # Crear pedido (clienteId + items)
│   ├── PedidoResponseDTO.java                # Pedido completo con items
│   ├── PedidoResumenDTO.java                 # id, fecha, total, estado, cantidadItems
│   ├── PedidoItemRequestDTO.java             # productoId + cantidad
│   └── PedidoItemResponseDTO.java            # Item con producto, precio y subtotal
├── exception/
│   ├── DuplicateResourceException.java       # HTTP 409
│   ├── ResourceNotFoundException.java        # HTTP 404
│   ├── KeycloakOperationException.java       # HTTP 500
│   └── GlobalExceptionHandler.java           # @RestControllerAdvice
├── model/
│   ├── Cliente.java                          # Entidad con keycloakUserId
│   ├── Producto.java
│   ├── Categoria.java
│   ├── Pedido.java
│   ├── PedidoItem.java
│   └── EstadoPedido.java                     # Enum de estados del pedido
├── repository/
│   ├── ClienteRepository.java
│   ├── ProductoRepository.java
│   ├── CategoriaRepository.java
│   └── PedidoRepository.java
├── service/
│   ├── KeycloakAdminService.java             # Gestión programática de usuarios en Keycloak
│   ├── ClienteService.java                   # Lógica de negocio + sincronización con Keycloak
│   ├── ProductoService.java
│   ├── CategoriaService.java
│   └── PedidoService.java                    # Incluye verificación de stock y cálculo de totales
└── KeycloakDemoApplication.java
```

---

## Decisiones de Diseño

### Patrón DTO (Request / Response / Resumen)

Se usan tres tipos de DTO por entidad para evitar recursión infinita en relaciones JPA bidireccionales y controlar exactamente qué datos se exponen:

- **RequestDTO**: Para crear/actualizar. Usa IDs en lugar de entidades completas (ej. `categoriaId` en lugar de `Categoria`).
- **ResponseDTO**: Para devolver datos. Incluye versiones resumidas de las relaciones.
- **ResumenDTO**: Versión mínima para usar dentro de otros ResponseDTOs (evita recursión).

### Gestión dual de usuarios (BD + Keycloak)

Al crear un `Cliente` se crea automáticamente un usuario en Keycloak con rol `CLIENTE`. El campo `keycloakUserId` en la entidad `Cliente` vincula ambos sistemas:

- **Keycloak**: gestiona autenticación y autorización
- **PostgreSQL**: almacena datos de negocio (pedidos, dirección, teléfono, etc.)

Las operaciones de actualización y eliminación se propagan a ambos sistemas.

### Autenticación en Swagger

Se usa un esquema **Bearer Token** en lugar de OAuth2 Authorization Code Flow, más sencillo para desarrollo. El endpoint público `/auth/token` actúa como proxy hacia Keycloak, permitiendo obtener tokens directamente desde Swagger UI sin herramientas externas.

### Mapeo manual en servicios (sin MapStruct)

Se intentó MapStruct pero presentó incompatibilidades con la combinación Spring Boot 4.x + Lombok. Se optó por mapeo manual en los servicios, que ofrece control total y no añade complejidad de configuración.

---

## Próximos Pasos

- [ ] **Testing**: Tests unitarios con Mockito y tests de integración con `@SpringBootTest`
- [ ] **Paginación**: Implementar `Pageable` en endpoints de lista (`/api/productos?page=0&size=20`)
- [ ] **Optimización N+1**: Usar `@EntityGraph` en repositorios para evitar queries múltiples
- [ ] **Compensación de transacciones**: Si falla la creación en BD, eliminar el usuario de Keycloak automáticamente
- [ ] **Auditoría**: Campos `createdAt`, `updatedAt`, `createdBy` con Spring Data Auditing
- [ ] **Filtros y búsqueda**: Implementar `Specification` para filtros dinámicos
- [ ] **Frontend**: Interfaz web consumiendo esta API (HTML + JS vanilla como primer paso)
- [ ] **Dockerización**: `docker-compose.yml` que levante PostgreSQL + Keycloak + aplicación
- [ ] **Variables de entorno**: Externalizar secretos a variables de entorno para producción

---

## Licencia

Este proyecto es de código abierto y está disponible bajo la licencia [MIT](https://opensource.org/licenses/MIT).
