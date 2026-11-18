# MeetingRoomReservation

Servicio REST para gestionar usuarios, salas y reservas de salas de reuniones. Evita reservas
solapadas, permite cancelarlas y protege las operaciones de escritura mediante JWT.

## Tecnologías

- Java 21 y Spring Boot 4
- PostgreSQL 17 y Flyway
- Spring Security con JWT
- Gradle
- Docker Compose
- OpenAPI y Swagger UI

## Ejecución con Docker

Requisitos: Docker y Docker Compose.

1. Crea un archivo `.env` en la raíz:

```dotenv
DB_NAME=meetroom
DB_USER=meetroom
DB_PASSWORD=cambia-esta-contrasena
DB_HOST=postgres
DB_PORT=5432
JWT_SECRET=cambia-esta-clave-de-al-menos-32-bytes
APP_PORT=8080
DB_EXPOSED_PORT=5432
```

2. Construye e inicia los servicios:

```bash
docker compose up --build
```

Flyway aplica automáticamente las migraciones al iniciar la aplicación.

Servicios disponibles:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Estado: `http://localhost:8080/actuator/health`

Para detener los contenedores:

```bash
docker compose down
```

## Usuario inicial

Las migraciones crean un administrador para el primer acceso:

```text
Email: admin@meetroom.local
Contraseña: admin
```

Estas credenciales deben cambiarse antes de utilizar el proyecto fuera de un entorno local.

## Endpoints principales

| Método | Ruta | Acceso | Función |
| --- | --- | --- | --- |
| `POST` | `/v1/users/login` | Público | Iniciar sesión |
| `POST` | `/v1/users` | `ADMIN` | Crear usuario |
| `GET` | `/v1/rooms` | Público | Listar salas |
| `POST` | `/v1/rooms` | `ADMIN` | Crear sala |
| `GET` | `/v1/reservations` | Público | Listar reservas |
| `POST` | `/v1/reservations` | Autenticado | Crear reserva |
| `POST` | `/v1/reservations/{id}/cancel` | Responsable o `ADMIN` | Cancelar reserva |

Los endpoints protegidos reciben el token mediante:

```http
Authorization: Bearer <token>
```

El contrato completo y los filtros disponibles pueden consultarse en Swagger UI.

## Ejecución local

Requisitos: Java 21 y PostgreSQL. Define `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`,
`DB_PASSWORD` y `JWT_SECRET`, y después ejecuta:

```powershell
.\gradlew.bat bootRun
```

En Linux o macOS utiliza `./gradlew bootRun`.

## Pruebas

Las pruebas de integración requieren Docker en ejecución porque utilizan Testcontainers.

```powershell
.\gradlew.bat test
```

En Linux o macOS utiliza `./gradlew test`.

## Arquitectura

El código sigue arquitectura hexagonal y está organizado por los módulos `user`, `room` y
`reservation`. El dominio no depende de Spring, JPA ni de los adaptadores externos.
