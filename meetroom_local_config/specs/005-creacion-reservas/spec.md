# 005 — Creación de reservas

**Estado**: aprobada.

## 1. Objetivo

Permitir que un usuario autenticado reserve una sala para un intervalo determinado, evitando
reservas activas solapadas.

Depende de:

| Funcionalidad | Aporta |
| --- | --- |
| `001 — Creación de usuarios` | Usuarios, seguridad y manejo de errores |
| `002 — Autenticación` | Identificación del usuario mediante JWT |
| `003 — Creación de salas` | Salas, capacidad y estado |

No requiere dependencias nuevas.

## 2. Alcance

Incluye:

- Modelo de reserva e intervalo temporal.
- Creación transaccional de reservas.
- Validación de sala, capacidad, horario y asistentes.
- Prevención de solapamientos.
- Persistencia y migración.
- Endpoint protegido.
- Pruebas de dominio, web, integración y concurrencia.

No incluye:

- Cancelación, modificación o reprogramación.
- Listado o consulta individual.
- Reservas recurrentes.
- Invitados o notificaciones.
- Consulta de disponibilidad.

## 3. Reserva

Cada reserva contiene:

| Campo | Restricciones |
| --- | --- |
| `id` | UUID generado por la aplicación |
| `roomId` | Sala existente y activa |
| `ownerId` | Usuario obtenido del token |
| `startsAt` | Instante de inicio |
| `endsAt` | Posterior al inicio y al momento actual |
| `attendees` | Entero mayor o igual a 1 y dentro de la capacidad |
| `status` | `ACTIVE` o `CANCELLED` |
| `createdAt` | Instante de creación |

La reserva se crea con estado `ACTIVE`.

El intervalo es semiabierto: incluye el inicio y excluye el final. Por tanto, `10:00–11:00` y
`11:00–12:00` no se solapan.

## 4. Persistencia

Tabla `reservations`:

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| `id` | `uuid` | Clave primaria |
| `room_id` | `uuid` | Clave foránea a `rooms` |
| `owner_id` | `uuid` | Clave foránea a `users` |
| `starts_at` | `timestamptz` | No nulo |
| `ends_at` | `timestamptz` | No nulo y posterior al inicio |
| `attendees` | `integer` | No nulo y mayor o igual a 1 |
| `status` | `varchar(20)` | `ACTIVE` o `CANCELLED` |
| `created_at` | `timestamptz` | No nulo |

La capacidad no se replica en la reserva.

La migración:

- Crea la extensión `btree_gist`.
- Crea la tabla y sus claves foráneas.
- Añade las restricciones de datos.
- Añade un índice sobre `(room_id, starts_at)`.
- Añade una restricción de exclusión contra solapamientos.

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservations
ADD CONSTRAINT reservations_no_overlap
EXCLUDE USING gist (
    room_id WITH =,
    tstzrange(starts_at, ends_at, '[)') WITH &&
) WHERE (status = 'ACTIVE');
```

La extensión puede requerir permisos adicionales fuera del entorno local.

## 5. Endpoint

```http
POST /v1/reservations
Authorization: Bearer <token>
Content-Type: application/json
```

Requiere autenticación, pero no un rol específico.

### Petición

```json
{
  "roomId": "8c1f4e2a-7b93-4d16-a5e8-0f3c9d2b6514",
  "startsAt": "2026-11-20T10:00:00+01:00",
  "endsAt": "2026-11-20T11:00:00+01:00",
  "attendees": 6
}
```

| Campo | Reglas |
| --- | --- |
| `roomId` | UUID válido |
| `startsAt` | ISO-8601 con offset obligatorio |
| `endsAt` | ISO-8601 con offset obligatorio |
| `attendees` | Entero mayor o igual a 1 |

El responsable se obtiene del claim `sub`. `ownerId` y otros campos desconocidos se ignoran.

### Respuesta satisfactoria

```http
201 Created
Location: /v1/reservations/{id}
```

```json
{
  "id": "d41f8a62-9c57-4b03-ae18-2f6b9d0c3754",
  "room": {
    "id": "8c1f4e2a-7b93-4d16-a5e8-0f3c9d2b6514",
    "name": "Sala Azul",
    "capacity": 12
  },
  "owner": {
    "id": "3f2b9c7a-5d41-4e8b-9a0c-7e1d2f6b8a34",
    "firstName": "Ana",
    "lastName": "López"
  },
  "startsAt": "2026-11-20T10:00:00+01:00",
  "endsAt": "2026-11-20T11:00:00+01:00",
  "attendees": 6,
  "status": "ACTIVE",
  "createdAt": "2026-11-17T09:14:22.418Z"
}
```

El responsable no incluye su correo.

## 6. Reglas y orden de evaluación

Las reglas se evalúan en este orden:

| Orden | Validación | Error |
| --- | --- | --- |
| 1 | Formato y campos obligatorios | `400 validation-error` |
| 2 | Fin posterior al inicio | `400 invalid-time-range` |
| 3 | Fin posterior al momento actual | `400 reservation-must-end-in-future` |
| 4 | Duración entre 15 minutos y 24 horas | `400 reservation-duration-out-of-range` |
| 5 | Sala existente | `404 room-not-found` |
| 6 | Sala activa | `409 room-not-available` |
| 7 | Asistentes dentro de la capacidad | `400 attendees-exceed-capacity` |
| 8 | Ausencia de solapamiento | `409 reservation-overlap` |
| 9 | Persistencia | `201` o `409 reservation-overlap` |

Reglas adicionales:

- El inicio puede estar en el pasado si el fin todavía es futuro.
- Las duraciones límite de 15 minutos y 24 horas son válidas.
- Offsets diferentes se comparan como instantes.
- La sala se bloquea únicamente durante la sección transaccional.
- El reloj se inyecta para que las reglas temporales sean deterministas.
- El dominio no llama directamente a `OffsetDateTime.now()`.

## 7. Concurrencia

La creación utiliza tres mecanismos:

1. Cerrojo pesimista sobre la sala.
2. Comprobación explícita de solapamiento.
3. Restricción de exclusión en PostgreSQL.

El cerrojo se toma antes de consultar solapamientos y se libera al confirmar o revertir la
transacción.

Las operaciones sobre la misma sala se serializan; las operaciones sobre salas diferentes no se
bloquean entre sí.

La restricción de exclusión garantiza que tampoco puedan introducirse solapamientos mediante otra
instancia, scripts o accesos directos a la base de datos.

Si el cerrojo no se obtiene en 5 segundos, se devuelve `503 room-busy`.

Toda la lectura de la sala, comprobación de solapamiento e inserción ocurre en una sola transacción
con aislamiento `READ COMMITTED`.

## 8. Errores

Todas las respuestas utilizan `application/problem+json`.

| Estado | Tipo | Causa |
| --- | --- | --- |
| `400` | `/problems/validation-error` | Formato o campos inválidos |
| `400` | `/problems/malformed-json` | JSON inválido |
| `400` | `/problems/invalid-time-range` | Fin no posterior al inicio |
| `400` | `/problems/reservation-must-end-in-future` | Fin no posterior al momento actual |
| `400` | `/problems/reservation-duration-out-of-range` | Duración fuera del rango permitido |
| `400` | `/problems/attendees-exceed-capacity` | Demasiados asistentes |
| `401` | `/problems/authentication-required` | Token ausente o inválido |
| `404` | `/problems/room-not-found` | Sala inexistente |
| `409` | `/problems/room-not-available` | Sala inactiva |
| `409` | `/problems/reservation-overlap` | Horario ocupado |
| `503` | `/problems/room-busy` | Cerrojo no obtenido en 5 segundos |

Solo `validation-error` incluye el array `errors`.

Cada error utiliza un `title` y un `detail` específicos. El conflicto por solapamiento no revela
la reserva ni el responsable involucrados.

El error de capacidad indica la capacidad real de la sala.

Ejemplo:

```json
{
  "type": "/problems/reservation-overlap",
  "title": "Horario no disponible",
  "status": 409,
  "detail": "La sala ya tiene una reserva activa en el horario indicado",
  "instance": "/v1/reservations"
}
```

## 9. Casos relevantes

| Caso | Resultado |
| --- | --- |
| Reuniones consecutivas | `201` |
| Solapamiento parcial, total o idéntico | `409` |
| Mismo horario en salas diferentes | `201` |
| Solapamiento con una reserva cancelada | `201` |
| Inicio pasado y fin futuro | `201` |
| Inicio y fin pasados | `400` |
| Fin igual o anterior al inicio | `400` |
| Duración inferior a 15 minutos o superior a 24 horas | `400` |
| Asistentes iguales a la capacidad | `201` |
| Asistentes superiores a la capacidad | `400` |
| Sala inexistente | `404` |
| Sala inactiva | `409` |
| Dos peticiones concurrentes y solapadas | Un `201` y un `409` |
| Dos peticiones concurrentes no solapadas | Dos `201` |
| Peticiones concurrentes en salas distintas | Dos `201` sin bloqueo mutuo |
| Fecha sin offset | `400` |
| Sin autenticación | `401` |

## 10. Diseño

- El intervalo temporal pertenece al dominio.
- La regla de solapamiento puede probarse sin base de datos.
- El módulo de reservas accede a la sala mediante un puerto propio.
- El dominio expresa la necesidad de acceso exclusivo sin conocer `FOR UPDATE` ni JPA.
- La infraestructura implementa el bloqueo y la persistencia.
- Las reglas dependientes del tiempo utilizan un reloj inyectado.

## 11. Criterios de aceptación

- Un usuario autenticado puede crear una reserva válida.
- El responsable siempre procede del token.
- La reserva se crea con estado `ACTIVE`.
- La respuesta contiene sala y responsable sin exponer el correo.
- Las reglas temporales, de capacidad y estado producen los errores definidos.
- Los intervalos consecutivos se aceptan.
- Los solapamientos activos se rechazan.
- Las reservas canceladas no bloquean horarios.
- Dos creaciones concurrentes y solapadas producen exactamente un `201` y un `409`.
- La base de datos impide solapamientos introducidos fuera de la aplicación.
- La migración funciona desde una base de datos vacía.
- El dominio no depende de Spring ni de JPA.

## 12. Pruebas

Se comprobarán:

- Semántica semiabierta del intervalo.
- Validaciones temporales con reloj controlado.
- Capacidad y estado de la sala.
- Orden de evaluación de errores.
- Creación satisfactoria.
- Errores `400`, `401`, `404`, `409` y `503`.
- Restricción de exclusión mediante inserción directa.
- Concurrencia real con reservas solapadas y no solapadas.
- Independencia entre salas.

## 13. Limitaciones conocidas

- `btree_gist` puede requerir permisos adicionales.
- El cerrojo serializa todas las creaciones de una misma sala.
- No existe modificación ni reprogramación.
- No hay límite de antelación.
- No hay límite de reservas por usuario.
- Los cambios posteriores de capacidad no afectan a reservas existentes.
- El reloj del servidor es la referencia.
