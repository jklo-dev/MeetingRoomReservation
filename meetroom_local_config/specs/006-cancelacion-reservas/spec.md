# 006 — Cancelación de reservas

**Estado**: aprobada.

## 1. Objetivo

Permitir que el responsable de una reserva o un administrador la cancele, conservando el registro
y liberando el horario restante.

Depende de:

| Funcionalidad | Aporta |
| --- | --- |
| `001 — Creación de usuarios` | Usuarios, roles y seguridad |
| `002 — Autenticación` | Identidad y rol mediante JWT |
| `005 — Creación de reservas` | Reservas, estados y restricción de solapamiento |

No requiere dependencias nuevas.

## 2. Alcance

Incluye:

- Cancelación por el responsable o un administrador.
- Auditoría de quién y cuándo canceló.
- Conservación de la reserva.
- Liberación del horario.
- Endpoint protegido.
- Pruebas de dominio, web e integración.

No incluye reactivación, borrado físico, motivo, notificaciones, cancelación masiva ni listado.

## 3. Persistencia

La tabla `reservations`, definida en `005 — Creación de reservas`, incorpora:

| Columna | Tipo | Restricciones |
| --- | --- | --- |
| `cancelled_at` | `timestamptz` | Nulo mientras la reserva esté activa |
| `cancelled_by` | `uuid` | Clave foránea al usuario que canceló |

```sql
ALTER TABLE reservations
ADD CONSTRAINT reservations_cancellation_consistency
CHECK (
    (status = 'CANCELLED' AND cancelled_at IS NOT NULL AND cancelled_by IS NOT NULL)
 OR (status = 'ACTIVE' AND cancelled_at IS NULL AND cancelled_by IS NULL)
);
```

La restricción de exclusión existente no cambia. Al pasar a `CANCELLED`, la reserva deja de
bloquear el horario.

## 4. Reglas y orden de evaluación

| Orden | Validación | Error |
| --- | --- | --- |
| 1 | Identificador UUID válido | `400 validation-error` |
| 2 | Reserva existente | `404 reservation-not-found` |
| 3 | Solicitante propietario o `ADMIN` | `403 insufficient-permissions` |
| 4 | Reserva en estado `ACTIVE` | `409 reservation-already-cancelled` |
| 5 | Fin posterior al momento actual | `409 reservation-already-finished` |
| 6 | Cambio a `CANCELLED` y auditoría | `200` |

- Una reserva en curso puede cancelarse.
- Una reserva cuyo fin coincide con el momento actual se considera finalizada.
- `cancelledBy` procede del claim `sub` y `cancelledAt` del reloj inyectado.
- La fila se conserva y la cancelación es definitiva.
- Un usuario sin permiso recibe `403` sin conocer el estado de una reserva ajena.

## 5. Concurrencia

La reserva se lee con bloqueo de fila dentro de la transacción. Dos cancelaciones simultáneas
producen una respuesta `200` y una `409 reservation-already-cancelled`.

Si el bloqueo no se obtiene en 5 segundos, se devuelve `503 reservation-busy`. El bloqueo se
libera al confirmar o revertir la transacción.

## 6. Endpoint

```http
POST /v1/reservations/{id}/cancel
Authorization: Bearer <token>
```

Requiere autenticación. La autorización se comprueba en el caso de uso. No lleva cuerpo; si se
envía uno, se ignora.

### Respuesta satisfactoria

```http
200 OK
```

Mantiene el contrato definido en `005 — Creación de reservas`, cambia el estado a `CANCELLED` y
añade `cancelledAt`:

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
  "status": "CANCELLED",
  "createdAt": "2026-11-17T09:14:22.418Z",
  "cancelledAt": "2026-11-17T09:31:05.772Z"
}
```

`cancelledBy` no se expone. En reservas activas, `cancelledAt` se omite.

## 7. Errores

Todas las respuestas utilizan `application/problem+json`.

| Estado | Tipo | Causa |
| --- | --- | --- |
| `400` | `/problems/validation-error` | Identificador inválido |
| `401` | `/problems/authentication-required` | Token ausente o inválido |
| `403` | `/problems/insufficient-permissions` | No es propietario ni administrador |
| `404` | `/problems/reservation-not-found` | Reserva inexistente |
| `409` | `/problems/reservation-already-cancelled` | Reserva ya cancelada |
| `409` | `/problems/reservation-already-finished` | Reserva ya finalizada |
| `503` | `/problems/reservation-busy` | Bloqueo no obtenido en 5 segundos |

```json
{
  "type": "/problems/reservation-already-cancelled",
  "title": "Reserva ya cancelada",
  "status": 409,
  "detail": "La reserva ya estaba cancelada",
  "instance": "/v1/reservations/d41f8a62-9c57-4b03-ae18-2f6b9d0c3754/cancel"
}
```

Cancelar nuevamente no es idempotente: devuelve `409`.

## 8. Casos relevantes

| Caso | Resultado |
| --- | --- |
| El responsable cancela su reserva | `200` |
| Un administrador cancela una reserva ajena | `200` |
| Otro usuario intenta cancelar | `403` |
| Reserva activa en curso | `200` |
| Reserva cuyo fin coincide con el momento actual | `409` |
| Reserva ya finalizada | `409` |
| Reserva ya cancelada | `409` |
| Identificador inválido | `400` |
| Identificador inexistente | `404` |
| Petición sin token | `401` |
| Cuerpo enviado | Se ignora |
| Dos cancelaciones simultáneas | Un `200` y un `409` |
| Nueva reserva sobre el horario liberado | `201` |

## 9. Diseño

- La transición de estado pertenece al dominio.
- La autorización utiliza identidad y rol, sin depender de Spring Security.
- El reloj se inyecta.
- La operación es transaccional.
- La infraestructura implementa el bloqueo de fila.
- El dominio no depende de Spring ni de JPA.

## 10. Criterios de aceptación

- El responsable y un administrador pueden cancelar; otro usuario recibe `403`.
- La reserva permanece almacenada con estado `CANCELLED`.
- `cancelledAt` y `cancelledBy` quedan persistidos.
- `cancelledBy` identifica a quien ejecutó la acción y no aparece en la respuesta.
- Una reserva en curso puede cancelarse.
- Una reserva finalizada o cancelada devuelve el `409` correspondiente.
- El horario cancelado puede reservarse nuevamente.
- Dos cancelaciones simultáneas producen un `200` y un `409`.
- La restricción de coherencia impide estados inválidos.
- Las migraciones funcionan desde una base de datos vacía.

## 11. Pruebas

Se comprobarán:

- Transición válida a `CANCELLED`.
- Reserva ya cancelada o finalizada.
- Límite exacto del momento de finalización.
- Autorización del propietario y administrador.
- Rechazo de otro usuario.
- Persistencia de auditoría.
- Liberación efectiva del horario.
- Restricción de coherencia mediante escritura directa.
- Dos cancelaciones simultáneas y espera máxima de 5 segundos.

## 12. Limitaciones conocidas

- No se registra el motivo.
- No se envían notificaciones.
- No existe reactivación.
- La operación no es idempotente.
- `cancelledBy` solo está disponible en base de datos.
- No existe cancelación masiva.
