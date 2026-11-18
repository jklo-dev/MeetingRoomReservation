# MeetingRoomReservation — Contexto del proyecto

Este documento contiene el contexto técnico y operativo específico del proyecto.

Los principios permanentes de trabajo se definen en
[`constitution.md`](constitution.md) y no se repiten aquí.

Las funcionalidades se especifican en [`specs/`](specs/).

## Estado de las decisiones

- **[Verificado]**: comprobado en el código o configuración.
- **[Decidido]**: acordado, aunque puede no estar implementado.
- **[Pendiente]**: debe resolverse en la especificación correspondiente.

## Proyecto

API REST para gestionar salas de reuniones y sus reservas.

Capacidades previstas:

- Crear y consultar salas.
- Crear, consultar y cancelar reservas.
- Evitar reservas solapadas para una misma sala.
- Filtrar y paginar consultas de reservas.
- Proteger mediante autenticación las operaciones de escritura.

No existe interfaz de usuario en este repositorio.

Quedan fuera del alcance mientras no se aprueben expresamente:

- Reprogramación de reservas.
- Registro, perfiles y gestión avanzada de usuarios.
- Notificaciones, calendarios externos e invitados.
- Reservas recurrentes, multi-tenencia y zonas horarias por usuario.
- Interfaz gráfica.

## Estado actual

**[Verificado]** Las funcionalidades 001 de creación de usuarios, 002 de autenticación, 003 de
creación de salas y 004 de listado de salas están implementadas.

Actualmente existen el dominio de usuarios, los casos de uso de creación y autenticación,
adaptadores web, JPA y JWT, migraciones Flyway, validación y emisión de tokens, manejo centralizado
de errores y pruebas de dominio, web e integración con PostgreSQL mediante Testcontainers.
También existe el dominio de salas, su caso de uso de creación, adaptadores web y JPA y la
migración `V3__create_rooms_table.sql`.

## Tecnologías verificadas

| Elemento | Valor |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.1.2 |
| Gradle | 9.9.1, Kotlin DSL |
| Base de datos prevista | PostgreSQL |
| Paquete base | `org.example.meetingroomreservation` |
| Nombre del proyecto | `MeetingRoomReservation` |

Las dependencias actuales están declaradas en `build.gradle.kts`. Antes de proponer cambios deben
revisarse directamente allí.

## Arquitectura

**[Decidido]** Se utiliza arquitectura hexagonal organizada por módulo de dominio.

Módulos principales:

- `user`
- `room`
- `reservation`
- Seguridad, configuración y gestión de errores como elementos transversales.

Organización general:

```text
módulo/
├── domain/
│   ├── model/
│   ├── port/in/
│   ├── port/out/
│   └── service/
└── infrastructure/
    └── adapter/
        ├── in/web/
        └── out/jpa/
```

Reglas específicas:

- El dominio no depende de Spring, JPA, Jackson ni de adaptadores.
- Los puertos pertenecen al dominio.
- Las entidades JPA no se usan como modelos de dominio ni se exponen en la API.
- Los DTO pertenecen al adaptador web.
- La conversión entre capas es explícita.
- La interacción entre módulos se realiza mediante puertos.

## Persistencia

**[Decidido]**

- PostgreSQL como base de datos.
- Flyway con migraciones SQL.
- Las migraciones son la fuente de verdad del esquema.
- Las migraciones aplicadas no se modifican.

Los modelos, restricciones, índices, datos temporales y estrategia de cancelación se definirán
en sus especificaciones.

La creación de reservas combina comprobación explícita de solapamiento, cerrojo pesimista por
sala y una restricción de exclusión en PostgreSQL. El cerrojo espera como máximo 5 segundos.

## API

**[Verificado]** `POST /v1/users` crea usuarios y requiere un JWT con rol `ADMIN`.
`POST /v1/users/login` es público y emite tokens JWT.
`POST /v1/rooms` crea salas y requiere un JWT con rol `ADMIN`.
`GET /v1/rooms` lista salas y es público; un bearer inválido o caducado devuelve `401`.

**[Decidido]** La API cubrirá:

- Creación de usuarios por administradores.
- Autenticación mediante correo y contraseña.
- Creación y consulta de salas.
- Creación, consulta y cancelación de reservas.

Las rutas, contratos, filtros, paginación, validaciones y respuestas de error se definirán en
cada especificación.

El listado público `GET /v1/rooms` devuelve por defecto salas `ACTIVE`, ordenadas por nombre y
paginadas con tamaño 20. Admite filtros acumulativos por estado, capacidad mínima y nombre,
además de ordenación y paginación.

`POST /v1/reservations` permite crear reservas a cualquier usuario autenticado. El responsable
procede del claim `sub` y los campos desconocidos del cuerpo se ignoran.

`POST /v1/reservations/{id}/cancel` permite cancelar al responsable o a un administrador. Conserva
la reserva, registra quién y cuándo canceló y usa un bloqueo de fila con espera máxima de 5
segundos.

`GET /v1/reservations` es público y admite filtros acumulativos por sala, responsable, estado y
rango temporal. El rango usa semántica semiabierta; el resultado se ordena por inicio y está
paginado.

Cuando resulte útil, el contrato de un endpoint se documentará junto a su especificación.

## Seguridad

**[Decidido]**

- Autenticación mediante JWT firmado con HS256.
- Usuarios persistidos en PostgreSQL.
- Contraseñas almacenadas con BCrypt.
- Roles `USER` y `ADMIN`.
- Login público mediante `POST /v1/users/login`.
- Creación de usuarios mediante `POST /v1/users`, restringida a `ADMIN`.
- Creación de salas mediante `POST /v1/rooms`, restringida a `ADMIN`.
- Creación de reservas mediante `POST /v1/reservations`, disponible para cualquier usuario
  autenticado.
- Cancelación mediante `POST /v1/reservations/{id}/cancel`, permitida al responsable o a un
  administrador.
- Tokens con vigencia de 24 horas y claims `sub`, `role`, `iat`, `exp` e `iss`.
- El emisor del token es `meetroom`.
- No se incluyen renovación ni revocación de tokens.
- Consultas públicas.
- Creación y cancelación protegidas.
- Secretos obtenidos mediante configuración externa.
- Gestión centralizada y segura de errores con tipos y detalles descriptivos.

Las reglas específicas de cada endpoint se definen en su especificación.

## Configuración

**[Verificado]** `application.yaml` configura el datasource PostgreSQL, JPA en modo `validate`,
Flyway y las propiedades de JWT.

**[Decidido]**

- Credenciales y secretos se proporcionan mediante variables de entorno o propiedades externas.
- No se almacenan secretos reales en el repositorio.

Variables externas utilizadas: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` y
`JWT_SECRET`.

## Comandos

En PowerShell:

| Acción | Comando |
| --- | --- |
| Compilar | `.\gradlew.bat build` |
| Compilar clases | `.\gradlew.bat classes` |
| Ejecutar pruebas | `.\gradlew.bat test` |
| Ejecutar aplicación | `.\gradlew.bat bootRun` |
| Listar tareas | `.\gradlew.bat tasks` |

Los tests que utilizan Testcontainers requieren Docker en ejecución.

Ningún comando de construcción, prueba, migración o ejecución debe lanzarse sin la autorización
previa exigida por la constitución.

## Especificaciones

Ubicación:

```text
meetroom_local_config/specs/NNN-nombre-funcionalidad/
├── spec.md
├── task.md
└── documentación adicional
```

- `spec.md` es obligatorio.
- `task.md` se utiliza cuando la implementación requiere varias tareas.
- La numeración usa el siguiente valor disponible.
- No se renumeran especificaciones existentes.

Funcionalidades definidas:

- `001-creacion-usuarios`: creación de usuarios por administradores y validación JWT;
  implementada.
- `002-autenticacion`: login y emisión de tokens JWT; implementada.
- `003-creacion-salas`: creación de salas por administradores; implementada.
- `004-listado-salas`: listado público de salas con filtros, ordenación y paginación;
  implementada.
- `005-creacion-reservas`: creación transaccional de reservas sin solapamientos; aprobada.
- `006-cancelacion-reservas`: cancelación con auditoría por el responsable o un administrador;
  aprobada.
- `007-listado-reservas`: listado público con filtros, ordenación y paginación; aprobada.
