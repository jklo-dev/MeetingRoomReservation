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

Con los valores del archivo `.env` del ejemplo, los servicios quedan disponibles en:

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

## Decisiones técnicas

- **Arquitectura hexagonal por módulos.** La lógica de negocio queda separada de HTTP, JPA y
  Spring. Esto permite probar el dominio sin levantar infraestructura y cambiar adaptadores sin
  modificar los casos de uso.
- **PostgreSQL y Flyway como fuente de verdad.** El esquema se crea mediante migraciones y
  Hibernate solo lo valida. Las reglas importantes también se protegen en la base de datos.
- **Control de reservas concurrentes.** La creación de una reserva bloquea la sala con un
  cerrojo pesimista y espera como máximo 30 segundos. Además, una restricción de exclusión en
  PostgreSQL impide intervalos solapados incluso si dos peticiones superan a la vez la validación
  previa de la aplicación.
- **Intervalos semiabiertos.** Las reservas usan la forma `[inicio, fin)`: una reserva puede
  comenzar exactamente cuando termina la anterior. Si se incorpora tiempo de acondicionamiento,
  esta regla deberá ampliarse con ese margen.
- **Cancelación con auditoría.** Cancelar no elimina la reserva. Se conserva su estado y se
  registra quién la canceló y cuándo.
- **Seguridad mediante JWT y roles.** Las operaciones de escritura requieren autenticación y las
  operaciones administrativas exigen el rol `ADMIN`. Los secretos se reciben desde el entorno.
- **Errores uniformes.** Los errores HTTP siguen `Problem Details`, con tipos y detalles
  descriptivos. Las violaciones de base de datos solo se traducen a un error de negocio cuando se
  identifica la restricción concreta.
- **Consultas paginadas y orden estable.** Los listados admiten filtros acumulativos y usan el
  identificador como desempate para evitar resultados repetidos u omitidos entre páginas.
- **Validación en varias capas.** La entrada HTTP, el dominio y la base de datos protegen sus
  respectivas invariantes. La base de datos sigue siendo la última barrera ante escrituras
  concurrentes o externas.
- **Uso de capacidad de salas.** En los requisitos iniciales no se pedía incluir la capacidad de
  las salas, pero creí necesario añadirla.


## Qué haría con más tiempo

- Mejorar el manejo de concurrencia en las reservas, especialmente cuando varios usuarios intentan reservar la misma sala al mismo tiempo.
- Ampliar la información de las salas con descripción, coste, acceso privado, conexión a internet, podio, sistema de audio, proyector, equipamiento y posibles defectos.
- Añadir horarios de funcionamiento para cada sala, teniendo en cuenta que algunas podrían estar disponibles durante todo el día y la noche y otras tener horarios más limitados.
- Mejorar las búsquedas de disponibilidad para poder encontrar salas con determinadas características, consultar las disponibles en un intervalo o encontrar el primer horario libre a partir de una fecha.
- Permitir configurar un tiempo de acondicionamiento entre reservas para dejar la sala preparada antes de recibir al siguiente usuario.
- Automatizar el cierre de las reservas que ya hayan pasado y contemplar casos de inasistencia.
- Mejorar el sistema de permisos de los usuarios, permitiendo un control más detallado de quién puede realizar determinadas operaciones.
- Permitir realizar reservas a nombre de otras personas, incluso si no tienen una cuenta en el sistema.
- Integrar una pasarela de pago para gestionar las reservas de salas que tengan un coste.
- Eliminar el usuario administrador creado por defecto y buscar una forma más segura de inicializar el sistema.
- Implementar la renovación y revocación de tokens, además de la rotación de claves.
- Añadir soporte para varios idiomas, tanto en los mensajes de error de los endpoints como en los nombres y descripciones de las salas.
- Revisar la gestión de contraseñas y los parámetros de BCrypt para mantener una configuración segura.
- Añadir registros y mecanismos de seguimiento que permitan detectar errores y entender mejor lo que ocurre en la aplicación.

## Uso de inteligencia artificial

Utilicé IA como apoyo para acelerar tareas mecánicas y de revisión, no como sustituto de la
decisión final. Le pedí principalmente que:

- organizara y simplificara las especificaciones sin cambiar su significado;
- generara clases, pruebas y archivos de configuración que después revisé;
- comparara el código con las especificaciones ya aprobadas;
- detectara ambigüedades, casos límite e inconsistencias entre documentación e implementación;
- ayudara con la documentación en sentido general, incluidos Swagger y el README.

Acepté varias propuestas de contratos de endpoints y estructuras de clases. También acepté la
estructura de los campos `type` en las respuestas de error y la implementación del health check
mediante Actuator en lugar de crear un endpoint propio.

También corregí o acoté varias respuestas antes de incorporarlas. Pedí eliminar redacción repetida, 
mantener los cambios de documentación estrictamente
semánticos y conservar la numeración de las funcionalidades. Exigí tipos y detalles de error
descriptivos en lugar de respuestas genéricas. Corregí que las rutas inexistentes devolvieran `404`, que
las capacidades decimales devolvieran `400`, que el nombre de una sala se recortara antes de
validar su longitud y que solo la restricción de nombre único se tradujera como nombre duplicado.
Cuando una generación modificó especificaciones ya cerradas, revisé las diferencias, descarté los
cambios accidentales y mantuve únicamente los que reflejaban el comportamiento real del código.
