# 007 — Listado de reservas

**Estado**: aprobada.

## 1. Objetivo

Permitir consultar reservas mediante filtros, ordenación y paginación.

Depende de:

| Funcionalidad | Aporta |
| --- | --- |
| `004 — Listado de salas` | Contrato y tipos de paginación |
| `005 — Creación de reservas` | Modelo y persistencia de reservas |
| `006 — Cancelación de reservas` | Estado y fecha de cancelación |

No requiere dependencias ni migraciones nuevas.

## 2. Alcance

Incluye:

- Listado público.
- Filtros por sala, responsable, estado y rango temporal.
- Ordenación por fecha de inicio.
- Paginación.
- Validación de parámetros.
- Pruebas de caso de uso, web e integración.

No incluye consulta individual, mutaciones, cálculo de disponibilidad, estadísticas ni exportación.

## 3. Endpoint

```http
GET /v1/reservations
```

Es público. Si se envía una cabecera `Authorization`, no se evalúa.

### Parámetros

Todos son opcionales y acumulativos.

| Parámetro | Reglas | Valor predeterminado |
| --- | --- | --- |
| `roomId` | UUID válido | Sin filtro |
| `ownerId` | UUID válido | Sin filtro |
| `status` | `ACTIVE`, `CANCELLED` o `ALL` | `ACTIVE` |
| `from` | ISO-8601 con offset | Sin límite inferior |
| `to` | ISO-8601 con offset; posterior a `from` | Sin límite superior |
| `direction` | `asc` o `desc` | `asc` |
| `page` | Entero mayor o igual a 0 | `0` |
| `size` | Entero mayor o igual a 1, sin máximo | `20` |

No existe `sortBy`: el orden siempre utiliza `startsAt`.

`status` y `direction` no distinguen mayúsculas. Los parámetros desconocidos se ignoran.

Ejemplo:

```http
GET /v1/reservations?roomId=8c1f4e2a-7b93-4d16-a5e8-0f3c9d2b6514&from=2026-11-20T00:00:00%2B01:00&status=ALL&direction=desc&size=50
```

## 4. Rango temporal

`from` y `to` seleccionan reservas que se solapan con el rango consultado.

| Parámetros | Condición |
| --- | --- |
| Solo `from` | `endsAt > from` |
| Solo `to` | `startsAt < to` |
| Ambos | `endsAt > from AND startsAt < to` |
| Ninguno | Sin filtro temporal |

La semántica es semiabierta:

- Una reserva que termina exactamente en `from` no aparece.
- Una reserva que empieza exactamente en `to` no aparece.
- Una reserva que toca parcialmente, contiene o está contenida en el rango sí aparece.

Ejemplos para el rango `11:00–15:00`:

| Reserva | Resultado |
| --- | --- |
| `10:00–12:00` | Incluida |
| `12:00–13:00` | Incluida |
| `09:00–16:00` | Incluida |
| `11:00–15:00` | Incluida |
| `09:00–11:00` | Excluida |
| `15:00–16:00` | Excluida |
| `08:00–10:00` | Excluida |
| `16:00–17:00` | Excluida |

## 5. Respuesta

Una petición válida devuelve `200`, aunque no existan resultados.

```json
{
  "content": [
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
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

La paginación mantiene el contrato definido en `004 — Listado de salas`.

Cada reserva mantiene el contrato de `005 — Creación de reservas` y añade `cancelledAt` cuando
está cancelada, según `006 — Cancelación de reservas`.

No se exponen el correo del responsable ni `cancelledBy`.

## 6. Errores

Los parámetros inválidos devuelven `400` con `application/problem+json`.

| Tipo | Motivo |
| --- | --- |
| `/problems/invalid-query-parameter` | Uno o más parámetros no cumplen sus reglas |

```json
{
  "type": "/problems/invalid-query-parameter",
  "title": "Parámetros de consulta inválidos",
  "status": 400,
  "detail": "El parámetro to debe ser posterior a from",
  "instance": "/v1/reservations",
  "errors": [
    {
      "field": "to",
      "message": "Debe ser posterior a from"
    }
  ]
}
```

El endpoint no devuelve `401`, `403` ni `404`.

Un `roomId` u `ownerId` válido pero inexistente devuelve `200` con contenido vacío.

## 7. Reglas

- Sin parámetros se devuelven reservas `ACTIVE` de todo el historial.
- El orden predeterminado es `startsAt` ascendente.
- `status=ALL` desactiva el filtro de estado.
- `roomId` y `ownerId` filtran por igualdad.
- `to` debe ser estrictamente posterior a `from`.
- Offsets diferentes se comparan como instantes.
- Toda ordenación usa `id` como desempate.
- Una página fuera de rango devuelve `200` con contenido vacío.
- El filtro de responsable utiliza `ownerId`, no nombre.
- El endpoint no procesa tokens.

## 8. Casos relevantes

| Caso | Resultado |
| --- | --- |
| Sin parámetros | Reservas activas, orden ascendente, página 0 y tamaño 20 |
| `status=all` | Se acepta |
| Estado desconocido | `400` |
| Identificador con formato inválido | `400` |
| Identificador válido pero inexistente | `200` vacío |
| `from` o `to` sin offset | `400` |
| `to` igual o anterior a `from` | `400` |
| Solo `from` | Reservas cuyo fin es posterior |
| Solo `to` | Reservas cuyo inicio es anterior |
| `direction=desc` | Orden descendente |
| Dirección desconocida | `400` |
| Página negativa o tamaño `0` | `400` |
| Página fuera de rango | `200` vacío |
| Tamaño muy grande | Se acepta |
| Token válido o caducado | Se ignora |
| Parámetro desconocido | Se ignora |

## 9. Diseño

- Reutiliza los tipos de paginación de `004 — Listado de salas`.
- El dominio recibe filtros y paginación mediante tipos propios.
- La infraestructura traduce esos criterios a persistencia.
- Los filtros opcionales utilizan `Specification`.
- Sala y responsable se cargan en la misma consulta para evitar N+1.
- No se añaden índices nuevos.

## 10. Criterios de aceptación

- El listado funciona sin autenticación.
- Los filtros individuales y combinados producen los resultados esperados.
- El rango temporal utiliza semántica semiabierta.
- Las reservas que sobresalen del rango se incluyen.
- Los contactos exactos con `from` o `to` se excluyen.
- El estado predeterminado es `ACTIVE`.
- La ordenación funciona en ambos sentidos y es estable.
- Los identificadores inexistentes producen contenido vacío.
- Los parámetros inválidos producen errores descriptivos.
- Una página fuera de rango devuelve contenido vacío.
- No existe problema N+1 al cargar sala y responsable.
- El dominio no depende de Spring ni de JPA.

## 11. Pruebas

Se comprobarán:

- Valores predeterminados.
- Filtros por sala, responsable y estado.
- Filtros combinados.
- Solapamiento temporal y límites exactos.
- Uso independiente de `from` y `to`.
- Orden ascendente y descendente.
- Estabilidad de paginación.
- Página vacía y fuera de rango.
- Parámetros inválidos.
- Identificadores inexistentes.
- Número de consultas para detectar N+1.

## 12. Limitaciones conocidas

- El tamaño de página no tiene máximo.
- No existe índice específico sobre `owner_id`.
- El total requiere una consulta de recuento.
- El endpoint público expone nombres, horarios y salas.
- No calcula disponibilidad.
- No filtra por asistentes ni capacidad.
